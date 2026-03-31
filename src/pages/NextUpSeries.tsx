import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Tv } from 'lucide-react';
import { useSeries } from '../context/SeriesContext';
import { fetchSeriesDetails } from '../lib/tmdb';
import type { Series, TmdbSeries, TmdbEpisode } from '../types';

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

type EpisodeState =
  | { type: 'available'; season: number; episode: number }
  | { type: 'coming_soon'; ep: TmdbEpisode }
  | { type: 'up_to_date' }
  | { type: 'unknown' };

function getEpisodeState(series: Series, tmdb: TmdbSeries | null | undefined): EpisodeState {
  const lastWatched = getUserLastWatched(series.watched_episodes ?? {});
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

// ─── component ─────────────────────────────────────────────────────────────

export default function NextUpSeries() {
  const { series } = useSeries();
  const { t, i18n } = useTranslation();
  const watching = series.filter(s => s.status === 'watching');

  const [tmdbData, setTmdbData] = useState<Record<string, TmdbSeries>>({});
  const [loading, setLoading] = useState(watching.length > 0);

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

  if (watching.length === 0) {
    return (
      <div className="p-4 md:p-8 max-w-2xl mx-auto flex flex-col items-center justify-center min-h-64 text-center">
        <Tv size={40} className="text-gray-300 dark:text-gray-600 mb-4" />
        <p className="text-gray-500 dark:text-gray-400 text-sm">{t('nextUp.noWatching')}</p>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 max-w-2xl mx-auto">
      <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        {t('discover.title')}
      </h1>
      <div className="space-y-3">
        {watching.map(s => {
          const tmdb = tmdbData[s.id];
          const state = loading && !tmdb ? { type: 'unknown' as const } : getEpisodeState(s, tmdb);
          return <SeriesNextCard key={s.id} series={s} state={state} t={t} lang={i18n.language} />;
        })}
      </div>
    </div>
  );
}

// ─── card sub-component ─────────────────────────────────────────────────────

function SeriesNextCard({
  series, state, t, lang,
}: {
  series: Series;
  state: EpisodeState;
  t: (key: string, opts?: Record<string, unknown>) => string;
  lang: string;
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
    <div className="card p-4 flex gap-4 items-start">
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
    </div>
  );
}
