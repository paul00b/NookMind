import { useState, useEffect } from 'react';
import type { Series } from '../types';
import { fetchSeasonDetails } from '../lib/tmdb';

export interface SeriesStats {
  totalSeries: number;
  watchedEpisodesCount: number | null;
  watchedSeasonsCount: number;
  averageRating: number | null;
  favoriteGenre: string | null;
  favoriteCreator: string | null;
  totalMinutes: number | null;
  loadingMinutes: boolean;
}

function computeLocalStats(series: Series[]): Omit<SeriesStats, 'watchedEpisodesCount' | 'totalMinutes' | 'loadingMinutes'> {
  const totalSeries = series.length;

  let watchedSeasonsCount = 0;
  for (const s of series) {
    watchedSeasonsCount += s.watched_seasons.length;
  }

  const rated = series.filter(s => s.rating != null && s.rating > 0);
  const averageRating =
    rated.length > 0
      ? Math.round((rated.reduce((sum, s) => sum + (s.rating ?? 0), 0) / rated.length) * 10) / 10
      : null;

  const genreCount: Record<string, number> = {};
  for (const s of series) {
    if (s.genre) genreCount[s.genre] = (genreCount[s.genre] ?? 0) + 1;
  }
  const favoriteGenre =
    Object.keys(genreCount).length > 0
      ? Object.entries(genreCount).sort((a, b) => b[1] - a[1])[0][0]
      : null;

  const creatorCount: Record<string, number> = {};
  for (const s of series) {
    if (s.creator) creatorCount[s.creator] = (creatorCount[s.creator] ?? 0) + 1;
  }
  const favoriteCreator =
    Object.keys(creatorCount).length > 0
      ? Object.entries(creatorCount).sort((a, b) => b[1] - a[1])[0][0]
      : null;

  return { totalSeries, watchedSeasonsCount, averageRating, favoriteGenre, favoriteCreator };
}

async function computeTmdbStats(series: Series[]): Promise<{ minutes: number; episodes: number }> {
  const perSeries = await Promise.all(
    series.map(async s => {
      if (!s.tmdb_id) return { minutes: 0, episodes: 0 };

      let minutes = 0;
      let episodes = 0;
      const watchedSeasonsSet = new Set(s.watched_seasons);
      const partialSeasons = Object.keys(s.watched_episodes)
        .map(Number)
        .filter(n => !watchedSeasonsSet.has(n));

      // Saisons complètes : tous les épisodes
      const completeResults = await Promise.all(
        s.watched_seasons.map(async seasonNum => {
          const details = await fetchSeasonDetails(s.tmdb_id!, seasonNum);
          if (!details) return { minutes: 0, episodes: 0 };
          return {
            minutes: details.episodes.reduce((acc, ep) => acc + (ep.runtime ?? 0), 0),
            episodes: details.episodes.length,
          };
        })
      );
      for (const r of completeResults) { minutes += r.minutes; episodes += r.episodes; }

      // Saisons partielles : uniquement les épisodes regardés
      const partialResults = await Promise.all(
        partialSeasons.map(async seasonNum => {
          const details = await fetchSeasonDetails(s.tmdb_id!, seasonNum);
          if (!details) return { minutes: 0, episodes: 0 };
          const watchedEps = new Set(s.watched_episodes[String(seasonNum)] ?? []);
          const watched = details.episodes.filter(ep => watchedEps.has(ep.episode_number));
          return {
            minutes: watched.reduce((acc, ep) => acc + (ep.runtime ?? 0), 0),
            episodes: watched.length,
          };
        })
      );
      for (const r of partialResults) { minutes += r.minutes; episodes += r.episodes; }

      return { minutes, episodes };
    })
  );

  return {
    minutes: perSeries.reduce((a, b) => a + b.minutes, 0),
    episodes: perSeries.reduce((a, b) => a + b.episodes, 0),
  };
}

export function useSeriesStats(series: Series[]): SeriesStats {
  const relevantSeries = series.filter(
    s => s.status === 'watched' || s.status === 'watching'
  );

  const localStats = computeLocalStats(relevantSeries);
  const [totalMinutes, setTotalMinutes] = useState<number | null>(null);
  const [watchedEpisodesCount, setWatchedEpisodesCount] = useState<number | null>(null);
  const [loadingMinutes, setLoadingMinutes] = useState(true);

  useEffect(() => {
    if (relevantSeries.length === 0) {
      setTotalMinutes(0);
      setWatchedEpisodesCount(0);
      setLoadingMinutes(false);
      return;
    }

    setLoadingMinutes(true);
    setTotalMinutes(null);
    setWatchedEpisodesCount(null);

    let cancelled = false;
    computeTmdbStats(relevantSeries)
      .then(({ minutes, episodes }) => {
        if (!cancelled) {
          setTotalMinutes(minutes);
          setWatchedEpisodesCount(episodes);
          setLoadingMinutes(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTotalMinutes(null);
          setWatchedEpisodesCount(null);
          setLoadingMinutes(false);
        }
      });

    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [relevantSeries.length]);

  return { ...localStats, watchedEpisodesCount, totalMinutes, loadingMinutes };
}
