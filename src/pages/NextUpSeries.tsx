import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CalendarDays, Check, Clock3, Star, Tv, X } from 'lucide-react';
import { useSeries } from '../context/SeriesContext';
import SheetModal, { SheetCloseButton } from '../components/SheetModal';
import { fetchSeasonDetails, fetchSeriesDetails, getPosterUrl } from '../lib/tmdb';
import type { Series, TmdbSeries, TmdbEpisode } from '../types';
import { deriveSeriesStatus } from '../components/SeasonGrid';

const NEXT_UP_DISMISS_DURATION_MS = 380;
const NEXT_UP_ENTER_DURATION_MS = 420;

// ─── helpers ───────────────────────────────────────────────────────────────

function getUserLastWatched(
  watchedEpisodes: Record<string, number[]>
): { season: number; episode: number } | null {
  const seasons = Object.keys(watchedEpisodes).map(Number).sort((a, b) => b - a);
  for (const season of seasons) {
    const eps = watchedEpisodes[String(season)];
    if (eps && eps.length > 0) return { season, episode: Math.max(...eps) };
  }
  return null;
}

function episodeScore(s: number, e: number) { return s * 10000 + e; }

function resolveWatchedEpisodes(series: Series, tmdb: TmdbSeries | null | undefined): Record<string, number[]> {
  const resolved: Record<string, number[]> = Object.fromEntries(
    Object.entries(series.watched_episodes ?? {}).map(([season, episodes]) => [season, [...episodes].sort((a, b) => a - b)])
  );
  if (!tmdb?.seasons) return resolved;

  for (const watchedSeason of series.watched_seasons) {
    const seasonInfo = tmdb.seasons.find(season => season.season_number === watchedSeason);
    const episodeCount = seasonInfo?.episode_count ?? 0;
    if (episodeCount > 0 && (resolved[String(watchedSeason)]?.length ?? 0) === 0) {
      resolved[String(watchedSeason)] = Array.from({ length: episodeCount }, (_, i) => i + 1);
    }
  }

  return resolved;
}

type EpisodeState =
  | { type: 'available'; season: number; episode: number }
  | { type: 'coming_soon'; ep: TmdbEpisode }
  | { type: 'up_to_date' }
  | { type: 'unknown' };

type DismissingCard = {
  key: string;
  series: Series;
  state: EpisodeState;
};

type PendingDismiss = {
  dismissKey: string;
};

type EpisodeSheetState = {
  series: Series;
  state: Extract<EpisodeState, { type: 'available' | 'coming_soon' }>;
};

function getEpisodeState(series: Series, tmdb: TmdbSeries | null | undefined): EpisodeState {
  const resolvedWatchedEpisodes = resolveWatchedEpisodes(series, tmdb);
  const lastWatched = getUserLastWatched(resolvedWatchedEpisodes);
  const lastWatchedScore = lastWatched ? episodeScore(lastWatched.season, lastWatched.episode) : -1;

  const lastAired = tmdb?.last_episode_to_air;
  const nextAiring = tmdb?.next_episode_to_air;

  if (!tmdb) return { type: 'unknown' };

  const lastAiredScore = lastAired
    ? episodeScore(lastAired.season_number, lastAired.episode_number)
    : -1;

  if (lastWatchedScore < lastAiredScore) {
    const nextSeason = lastWatched?.season ?? 1;
    const nextEp = lastWatched
      ? lastWatched.episode + 1
      : 1;
    const seasonInfo = tmdb.seasons?.find(s => s.season_number === nextSeason);
    if (seasonInfo && nextEp > seasonInfo.episode_count) {
      return { type: 'available', season: nextSeason + 1, episode: 1 };
    }
    return { type: 'available', season: nextSeason, episode: nextEp };
  }

  if (nextAiring) return { type: 'coming_soon', ep: nextAiring };

  return { type: 'up_to_date' };
}

function daysUntil(dateStr: string): number {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr); target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

function formatDate(dateStr: string, lang: string): string {
  return new Date(dateStr).toLocaleDateString(lang === 'fr' ? 'fr-FR' : 'en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

function getCardKey(series: Series, state: EpisodeState): string {
  if (state.type === 'available') return `${series.id}-available-${state.season}-${state.episode}`;
  if (state.type === 'coming_soon') return `${series.id}-coming-soon-${state.ep.season_number}-${state.ep.episode_number}`;
  return `${series.id}-${state.type}`;
}

// ─── component ─────────────────────────────────────────────────────────────

export default function NextUpSeries() {
  const { series, updateSeries } = useSeries();
  const { t, i18n } = useTranslation();
  const watching = series.filter(s => s.status === 'watching');

  const [tmdbData, setTmdbData] = useState<Record<string, TmdbSeries>>({});
  const [loading, setLoading] = useState(watching.length > 0);
  const [dismissingCards, setDismissingCards] = useState<Record<string, DismissingCard>>({});
  const [pendingDismisses, setPendingDismisses] = useState<Record<string, PendingDismiss>>({});
  const [enteringKeys, setEnteringKeys] = useState<string[]>([]);
  const [selectedEpisode, setSelectedEpisode] = useState<EpisodeSheetState | null>(null);
  const [episodeDetails, setEpisodeDetails] = useState<TmdbEpisode | null>(null);
  const [episodeDetailsLoading, setEpisodeDetailsLoading] = useState(false);

  useEffect(() => {
    if (watching.length === 0) { setLoading(false); return; }
    let active = true;
    (async () => {
      const results: Record<string, TmdbSeries> = {};
      await Promise.all(watching.map(async s => {
        if (!s.tmdb_id) return;
        const info = await fetchSeriesDetails(s.tmdb_id);
        if (info) results[s.id] = info;
      }));
      if (active) { setTmdbData(results); setLoading(false); }
    })();
    return () => { active = false; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [watching.map(s => s.id).join(',')]);

  useEffect(() => {
    if (!selectedEpisode?.series.tmdb_id) {
      setEpisodeDetails(null);
      setEpisodeDetailsLoading(false);
      return;
    }

    const seasonNumber = selectedEpisode.state.type === 'available'
      ? selectedEpisode.state.season
      : selectedEpisode.state.ep.season_number;
    const episodeNumber = selectedEpisode.state.type === 'available'
      ? selectedEpisode.state.episode
      : selectedEpisode.state.ep.episode_number;

    let active = true;
    setEpisodeDetailsLoading(true);
    fetchSeasonDetails(selectedEpisode.series.tmdb_id, seasonNumber)
      .then(details => {
        if (!active) return;
        const match = details?.episodes.find(ep => ep.episode_number === episodeNumber) ?? null;
        setEpisodeDetails(match);
      })
      .finally(() => {
        if (active) setEpisodeDetailsLoading(false);
      });

    return () => { active = false; };
  }, [selectedEpisode]);

  const seriesCards = watching.map(s => {
    const tmdb = tmdbData[s.id];
    const state = loading && !tmdb ? { type: 'unknown' as const } : getEpisodeState(s, tmdb);
    return { series: s, state };
  });

  const seriesCardsById = Object.fromEntries(seriesCards.map(card => [card.series.id, card]));
  const activeCards = watching.flatMap(currentSeries => {
    const dismissingCard = dismissingCards[currentSeries.id];
    if (dismissingCard) {
      return [{
        series: dismissingCard.series,
        state: dismissingCard.state,
        key: dismissingCard.key,
        dismissing: true,
        entering: false,
      }];
    }

    const liveCard = seriesCardsById[currentSeries.id];
    if (!liveCard || liveCard.state.type === 'up_to_date') return [];

    const liveKey = getCardKey(liveCard.series, liveCard.state);
    return [{
      series: liveCard.series,
      state: liveCard.state,
      key: liveKey,
      dismissing: false,
      entering: enteringKeys.includes(liveKey),
    }];
  });

  const upToDateCards = seriesCards.filter(({ state }) => state.type === 'up_to_date');

  useEffect(() => {
    const readySeriesIds = Object.entries(pendingDismisses).flatMap(([seriesId, pending]) => {
      const nextCard = seriesCardsById[seriesId];
      if (!nextCard) return [seriesId];

      const nextKey = getCardKey(nextCard.series, nextCard.state);
      if (nextCard.state.type === 'up_to_date' || nextKey !== pending.dismissKey) {
        return [seriesId];
      }

      return [];
    });

    if (readySeriesIds.length === 0) return;

    setDismissingCards(prev => {
      const next = { ...prev };
      for (const seriesId of readySeriesIds) delete next[seriesId];
      return next;
    });

    setPendingDismisses(prev => {
      const next = { ...prev };
      for (const seriesId of readySeriesIds) delete next[seriesId];
      return next;
    });

    const nextKeys = readySeriesIds.flatMap(seriesId => {
      const nextCard = seriesCardsById[seriesId];
      if (!nextCard || nextCard.state.type === 'up_to_date') return [];

      const nextKey = getCardKey(nextCard.series, nextCard.state);
      const pending = pendingDismisses[seriesId];
      return nextKey !== pending?.dismissKey ? [nextKey] : [];
    });

    if (nextKeys.length > 0) {
      setEnteringKeys(prev => [...new Set([...prev, ...nextKeys])]);
      const timeout = window.setTimeout(() => {
        setEnteringKeys(prev => prev.filter(key => !nextKeys.includes(key)));
      }, NEXT_UP_ENTER_DURATION_MS);
      return () => window.clearTimeout(timeout);
    }
  }, [pendingDismisses, seriesCardsById]);

  if (watching.length === 0) {
    return (
      <div className="p-4 md:p-8 max-w-2xl mx-auto flex flex-col items-center justify-center min-h-64 text-center">
        <Tv size={40} className="text-gray-300 dark:text-gray-600 mb-4" />
        <p className="text-gray-500 dark:text-gray-400 text-sm">{t('nextUp.noWatching')}</p>
      </div>
    );
  }

  const handleMarkEpisodeWatched = async (seriesItem: Series, season: number, episode: number) => {
    const tmdb = tmdbData[seriesItem.id];
    const dismissKey = `${seriesItem.id}-available-${season}-${episode}`;
    const dismissState: EpisodeState = { type: 'available', season, episode };
    const resolvedWatchedEpisodes = resolveWatchedEpisodes(seriesItem, tmdb);
    const seasonKey = String(season);
    const nextEpisodes = [
      ...new Set([...(resolvedWatchedEpisodes[seasonKey] ?? []), episode]),
    ].sort((a, b) => a - b);
    const nextWatchedEpisodes = { ...resolvedWatchedEpisodes, [seasonKey]: nextEpisodes };
    const nextWatchedSeasons = new Set(seriesItem.watched_seasons);

    for (const seasonInfo of tmdb?.seasons ?? []) {
      const watchedForSeason = nextWatchedEpisodes[String(seasonInfo.season_number)]?.length ?? 0;
      if (seasonInfo.episode_count > 0 && watchedForSeason >= seasonInfo.episode_count) {
        nextWatchedSeasons.add(seasonInfo.season_number);
      }
    }

    setDismissingCards(prev => ({
      ...prev,
      [seriesItem.id]: {
        key: dismissKey,
        series: seriesItem,
        state: dismissState,
      },
    }));
    setPendingDismisses(prev => ({
      ...prev,
      [seriesItem.id]: { dismissKey },
    }));
    window.setTimeout(() => {
      void updateSeries(seriesItem.id, {
        watched_episodes: nextWatchedEpisodes,
        watched_seasons: [...nextWatchedSeasons].sort((a, b) => a - b),
        status: deriveSeriesStatus([...nextWatchedSeasons].sort((a, b) => a - b), seriesItem.seasons),
      });
    }, NEXT_UP_DISMISS_DURATION_MS);
  };

  const handleOpenEpisode = (seriesItem: Series, state: Extract<EpisodeState, { type: 'available' | 'coming_soon' }>) => {
    setSelectedEpisode({ series: seriesItem, state });
  };

  return (
    <>
      <div className="p-4 md:p-8 max-w-2xl mx-auto">
        <h1 className="font-serif text-2xl font-bold text-teal-600 dark:text-teal-400 mb-6">
          {t('discover.title')}
        </h1>
        {activeCards.length > 0 && (
          <div className="space-y-3">
            {activeCards.map(({ series: currentSeries, state, key, dismissing, entering }) => (
              <SeriesNextCard
                key={key}
                series={currentSeries}
                state={state}
                t={t}
                lang={i18n.language}
                dismissing={dismissing}
                entering={entering}
                onMarkEpisodeWatched={handleMarkEpisodeWatched}
                onOpenEpisode={handleOpenEpisode}
              />
            ))}
          </div>
        )}

        {upToDateCards.length > 0 && (
          <section className={activeCards.length > 0 ? 'mt-8' : ''}>
            <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-gray-500 dark:text-gray-400 mb-3">
              {t('nextUp.upToDateSection')}
            </h2>
            <div className="space-y-3">
              {upToDateCards.map(({ series: currentSeries, state }) => (
                <SeriesNextCard
                  key={getCardKey(currentSeries, state)}
                  series={currentSeries}
                  state={state}
                  t={t}
                  lang={i18n.language}
                  dismissing={false}
                  entering={enteringKeys.includes(getCardKey(currentSeries, state))}
                />
              ))}
            </div>
          </section>
        )}
      </div>

      {selectedEpisode && (
        <EpisodeDetailSheet
          series={selectedEpisode.series}
          state={selectedEpisode.state}
          episode={episodeDetails}
          loading={episodeDetailsLoading}
          lang={i18n.language}
          t={t}
          onClose={() => setSelectedEpisode(null)}
          onMarkEpisodeWatched={selectedEpisode.state.type === 'available' ? handleMarkEpisodeWatched : undefined}
        />
      )}
    </>
  );
}

// ─── card sub-component ─────────────────────────────────────────────────────

function SeriesNextCard({
  series, state, t, lang, dismissing, entering, onMarkEpisodeWatched, onOpenEpisode,
}: {
  series: Series;
  state: EpisodeState;
  t: (key: string, opts?: Record<string, unknown>) => string;
  lang: string;
  dismissing: boolean;
  entering: boolean;
  onMarkEpisodeWatched?: (series: Series, season: number, episode: number) => void;
  onOpenEpisode?: (series: Series, state: Extract<EpisodeState, { type: 'available' | 'coming_soon' }>) => void;
}) {
  const badge = (() => {
    if (state.type === 'unknown') return <span className="text-xs text-gray-400 animate-pulse">…</span>;
    if (state.type === 'up_to_date') return (
      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-500">
        {t('nextUp.upToDate')}
      </span>
    );
    if (state.type === 'available') return (
      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-400">
        {t('nextUp.available')}
      </span>
    );
    if (state.type === 'coming_soon' && state.ep.air_date) {
      const days = daysUntil(state.ep.air_date);
      const dateStr = formatDate(state.ep.air_date, lang);
      const label = days === 0
        ? t('nextUp.today', { date: dateStr })
        : days === 1
        ? t('nextUp.comingSoon', { days, date: dateStr })
        : t('nextUp.comingSoonPlural', { days, date: dateStr });
      return (
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-400">
          {label}
        </span>
      );
    }
    return null;
  })();

  const detail = (() => {
    if (state.type === 'available') return (
      <span className="text-sm text-gray-600 dark:text-gray-300">
        {t('nextUp.seasonEpisode', { season: state.season, episode: state.episode })}
      </span>
    );
    if (state.type === 'coming_soon') return (
      <span className="text-sm text-gray-600 dark:text-gray-300">
        {t('nextUp.seasonEpisode', { season: state.ep.season_number, episode: state.ep.episode_number })}
        {state.ep.name && (
          <span className="text-gray-400"> · {state.ep.name}</span>
        )}
      </span>
    );
    return null;
  })();

  const clickable = (state.type === 'available' || state.type === 'coming_soon') && onOpenEpisode;

  return (
    <div
      className={`card relative overflow-hidden p-4 flex gap-4 items-center ${dismissing ? 'next-up-card-dismiss' : ''} ${entering ? 'next-up-card-enter' : ''} ${clickable ? 'cursor-pointer transition-colors hover:bg-black/[0.02] dark:hover:bg-white/[0.02]' : ''}`}
      onClick={clickable ? () => onOpenEpisode(series, state) : undefined}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
      onKeyDown={clickable ? (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onOpenEpisode(series, state);
        }
      } : undefined}
    >
      <div className="w-12 aspect-[2/3] rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
        {series.poster_url
          ? <img src={series.poster_url} alt={series.title} className="w-full h-full object-cover" />
          : <div className="w-full h-full flex items-center justify-center"><Tv size={20} className="text-gray-400" /></div>
        }
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">{series.title}</p>
        {detail && <div className="mt-0.5">{detail}</div>}
        <div className="mt-1.5">{badge}</div>
      </div>
      {state.type === 'available' && onMarkEpisodeWatched && (
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onMarkEpisodeWatched(series, state.season, state.episode);
          }}
          disabled={dismissing}
          className="h-10 w-10 rounded-full bg-emerald-500 text-white flex items-center justify-center shadow-sm transition-all duration-150 hover:bg-emerald-600 active:scale-95 shrink-0 self-center disabled:opacity-60"
          title={t('nextUp.markEpisodeWatched')}
          aria-label={t('nextUp.markEpisodeWatched')}
        >
          <Check size={18} strokeWidth={2.4} />
        </button>
      )}
    </div>
  );
}

function EpisodeDetailSheet({
  series,
  state,
  episode,
  loading,
  lang,
  t,
  onClose,
  onMarkEpisodeWatched,
}: {
  series: Series;
  state: Extract<EpisodeState, { type: 'available' | 'coming_soon' }>;
  episode: TmdbEpisode | null;
  loading: boolean;
  lang: string;
  t: (key: string, opts?: Record<string, unknown>) => string;
  onClose: () => void;
  onMarkEpisodeWatched?: (series: Series, season: number, episode: number) => void;
}) {
  const seasonNumber = state.type === 'available' ? state.season : state.ep.season_number;
  const episodeNumber = state.type === 'available' ? state.episode : state.ep.episode_number;
  const airDate = episode?.air_date ?? (state.type === 'coming_soon' ? state.ep.air_date : null);
  const episodeName = episode?.name ?? (state.type === 'coming_soon' ? state.ep.name : '');
  const overview = episode?.overview?.trim();
  const stillUrl = episode?.still_path ? getPosterUrl(episode.still_path) : null;
  const statusLabel = state.type === 'available' ? t('nextUp.available') : t('nextUp.detailComingSoon');

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-xl card rounded-t-3xl rounded-b-none md:rounded-3xl max-h-[88vh] animate-slide-up"
      scrollable
    >
        <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </SheetCloseButton>

        <div className="p-6 pb-5">
          <div className="flex gap-4 items-start">
            <div className="w-20 aspect-[2/3] rounded-2xl overflow-hidden bg-gray-100 dark:bg-gray-800 shrink-0">
              {series.poster_url ? (
                <img src={series.poster_url} alt={series.title} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={28} className="text-gray-400" />
                </div>
              )}
            </div>
            <div className="min-w-0 flex-1 pr-10">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-600 dark:text-teal-400 mb-2">
                {statusLabel}
              </p>
              <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight">
                {episodeName || t('nextUp.episodeFallback', { season: seasonNumber, episode: episodeNumber })}
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                {series.title} · {t('nextUp.seasonEpisode', { season: seasonNumber, episode: episodeNumber })}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 mt-4">
            {airDate && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gray-100 dark:bg-gray-800 text-sm text-gray-700 dark:text-gray-300">
                <CalendarDays size={14} />
                {formatDate(airDate, lang)}
              </span>
            )}
            {typeof episode?.runtime === 'number' && episode.runtime > 0 && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gray-100 dark:bg-gray-800 text-sm text-gray-700 dark:text-gray-300">
                <Clock3 size={14} />
                {t('nextUp.runtimeMinutes', { count: episode.runtime })}
              </span>
            )}
            {typeof episode?.vote_average === 'number' && episode.vote_average > 0 && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-400 text-sm">
                <Star size={14} className="fill-current" />
                {episode.vote_average.toFixed(1)}
              </span>
            )}
          </div>

          {stillUrl && (
            <div className="mt-5 rounded-2xl overflow-hidden border border-black/[0.06] dark:border-white/[0.06] bg-gray-100 dark:bg-gray-900/40">
              <img src={stillUrl} alt={episodeName || `${series.title} ${t('nextUp.seasonEpisode', { season: seasonNumber, episode: episodeNumber })}`} className="w-full h-auto object-cover" />
            </div>
          )}

          <div className={`mt-5 space-y-3 ${!overview ? 'pb-4 md:pb-2' : ''}`}>
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">{t('nextUp.episodeSynopsis')}</p>
              {loading ? (
                <div className="space-y-2">
                  <div className="h-3.5 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
                  <div className="h-3.5 rounded bg-gray-200 dark:bg-gray-700 animate-pulse w-11/12" />
                  <div className="h-3.5 rounded bg-gray-200 dark:bg-gray-700 animate-pulse w-8/12" />
                </div>
              ) : overview ? (
                <p className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">{overview}</p>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400">{t('nextUp.noEpisodeOverview')}</p>
              )}
            </div>
          </div>

          {state.type === 'available' && onMarkEpisodeWatched && (
            <div className="mt-6 pb-4 md:pb-2 flex justify-center">
              <button
                type="button"
                onClick={() => {
                  onMarkEpisodeWatched(series, seasonNumber, episodeNumber);
                  onClose();
                }}
                className="btn-primary inline-flex items-center justify-center gap-2 min-w-36"
              >
                <Check size={18} />
                {t('nextUp.markEpisodeWatched')}
              </button>
            </div>
          )}
        </div>
    </SheetModal>
  );
}
