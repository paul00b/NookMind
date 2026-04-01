import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Check, Tv } from 'lucide-react';
import { useSeries } from '../context/SeriesContext';
import { fetchSeriesDetails } from '../lib/tmdb';
import type { Series, TmdbSeries, TmdbEpisode } from '../types';
import { deriveSeriesStatus } from '../components/SeasonGrid';

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
  const [dismissingKeys, setDismissingKeys] = useState<string[]>([]);
  const [enteringKeys, setEnteringKeys] = useState<string[]>([]);
  const previousActiveKeysRef = useRef<string[]>([]);

  useEffect(() => {
    if (watching.length === 0) { setLoading(false); return; }
    let active = true;
    (async () => {
      const results: Record<string, TmdbSeries> = {};
      await Promise.all(watching.map(async s => {
        if (!s.tmdb_id) return;
        const cacheKey = `nookmind_tmdb_series_${s.tmdb_id}`;
        const cached = sessionStorage.getItem(cacheKey);
        if (cached) { results[s.id] = JSON.parse(cached); return; }
        const info = await fetchSeriesDetails(s.tmdb_id);
        if (info) {
          results[s.id] = info;
          sessionStorage.setItem(cacheKey, JSON.stringify(info));
        }
      }));
      if (active) { setTmdbData(results); setLoading(false); }
    })();
    return () => { active = false; };
  }, [watching.map(s => s.id).join(',')]);

  const seriesCards = watching.map(s => {
    const tmdb = tmdbData[s.id];
    const state = loading && !tmdb ? { type: 'unknown' as const } : getEpisodeState(s, tmdb);
    return { series: s, state };
  });

  const activeCards = seriesCards.filter(({ state }) => state.type !== 'up_to_date');
  const upToDateCards = seriesCards.filter(({ state }) => state.type === 'up_to_date');
  const activeCardKeys = activeCards.map(({ series: currentSeries, state }) => getCardKey(currentSeries, state));

  useEffect(() => {
    const previousActiveKeys = previousActiveKeysRef.current;
    const newKeys = activeCardKeys.filter(key => !previousActiveKeys.includes(key));
    if (previousActiveKeys.length > 0 && newKeys.length > 0) {
      setEnteringKeys(prev => [...new Set([...prev, ...newKeys])]);
      const timeout = window.setTimeout(() => {
        setEnteringKeys(prev => prev.filter(key => !newKeys.includes(key)));
      }, 520);
      previousActiveKeysRef.current = activeCardKeys;
      return () => window.clearTimeout(timeout);
    }
    previousActiveKeysRef.current = activeCardKeys;
  }, [activeCardKeys.join('|')]);

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

    setDismissingKeys(prev => [...new Set([...prev, dismissKey])]);
    window.setTimeout(() => {
      void updateSeries(seriesItem.id, {
        watched_episodes: nextWatchedEpisodes,
        watched_seasons: [...nextWatchedSeasons].sort((a, b) => a - b),
        status: deriveSeriesStatus([...nextWatchedSeasons].sort((a, b) => a - b), seriesItem.seasons),
      });
    }, 420);
    window.setTimeout(() => {
      setDismissingKeys(prev => prev.filter(key => key !== dismissKey));
    }, 900);
  };

  return (
    <div className="p-4 md:p-8 max-w-2xl mx-auto">
      <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {t('discover.title')}
      </h1>
      {activeCards.length > 0 && (
        <div className="space-y-3">
          {activeCards.map(({ series: currentSeries, state }) => (
            <SeriesNextCard
              key={getCardKey(currentSeries, state)}
              series={currentSeries}
              state={state}
              t={t}
              lang={i18n.language}
              dismissing={dismissingKeys.includes(getCardKey(currentSeries, state))}
              entering={enteringKeys.includes(getCardKey(currentSeries, state))}
              onMarkEpisodeWatched={handleMarkEpisodeWatched}
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
  );
}

// ─── card sub-component ─────────────────────────────────────────────────────

function SeriesNextCard({
  series, state, t, lang, dismissing, entering, onMarkEpisodeWatched,
}: {
  series: Series;
  state: EpisodeState;
  t: (key: string, opts?: Record<string, unknown>) => string;
  lang: string;
  dismissing: boolean;
  entering: boolean;
  onMarkEpisodeWatched?: (series: Series, season: number, episode: number) => void;
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

  return (
    <div className={`card relative overflow-hidden p-4 flex gap-4 items-center ${dismissing ? 'next-up-card-dismiss' : ''} ${entering ? 'next-up-card-enter' : ''}`}>
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
          onClick={() => onMarkEpisodeWatched(series, state.season, state.episode)}
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
