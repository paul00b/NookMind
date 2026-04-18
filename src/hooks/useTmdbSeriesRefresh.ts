import { useEffect, useRef } from 'react';
import { useSeries } from '../context/SeriesContext';
import { fetchSeriesDetails, extractSeriesData } from '../lib/tmdb';
import { getEffectiveSeriesStatus } from '../lib/seriesUtils';
import type { Series } from '../types';

const REFRESH_KEY = 'nookmind_series_tmdb_refresh';
const REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24h
const REQUEST_DELAY_MS = 300; // éviter le rate limiting TMDB

export function useTmdbSeriesRefresh() {
  const { series, updateSeries } = useSeries();
  const hasRun = useRef(false);

  useEffect(() => {
    if (hasRun.current || series.length === 0) return;

    const lastRefresh = localStorage.getItem(REFRESH_KEY);
    const now = Date.now();
    if (lastRefresh && now - parseInt(lastRefresh, 10) < REFRESH_INTERVAL_MS) return;

    hasRun.current = true;
    localStorage.setItem(REFRESH_KEY, String(now));

    const toRefresh = series.filter(
      (s): s is Series & { tmdb_id: number } =>
        s.tmdb_id !== null && ['watching', 'watched'].includes(getEffectiveSeriesStatus(s))
    );

    toRefresh.forEach((s, i) => {
      setTimeout(async () => {
        const tmdbData = await fetchSeriesDetails(s.tmdb_id);
        if (!tmdbData) return;
        const extracted = extractSeriesData(tmdbData);
        const nextAirDate = extracted.next_air_date ?? null;
        const nextSeasonNumber = extracted.next_season_number ?? null;
        const seasons = extracted.seasons;
        if (
          nextAirDate === s.next_air_date &&
          nextSeasonNumber === s.next_season_number &&
          seasons === s.seasons
        ) return;
        updateSeries(s.id, { seasons, next_air_date: nextAirDate, next_season_number: nextSeasonNumber });
      }, i * REQUEST_DELAY_MS);
    });
  }, [series, updateSeries]);
}
