import type { TmdbMovie, TmdbSeries, TmdbSeasonDetails } from '../types';

const BASE_URL = 'https://api.themoviedb.org/3';
const IMAGE_BASE = 'https://image.tmdb.org/t/p/w500';
const API_KEY = import.meta.env.VITE_TMDB_API_KEY as string | undefined;

function getTmdbLocale(): string {
  const lang = typeof navigator !== 'undefined' ? navigator.language : 'en-US';
  if (lang.startsWith('fr')) return 'fr-FR';
  return 'en-US';
}
const TMDB_CACHE_TTL_MS = 6 * 60 * 60 * 1000;
const seriesDetailsCache = new Map<number, Promise<TmdbSeries | null>>();
const seasonDetailsCache = new Map<string, Promise<TmdbSeasonDetails | null>>();

function buildUrl(path: string, params: Record<string, string> = {}): string {
  const url = new URL(`${BASE_URL}${path}`);
  if (API_KEY) url.searchParams.set('api_key', API_KEY);
  Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
  return url.toString();
}

function getSessionCache<T>(key: string): T | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { ts: number; data: T };
    if (Date.now() - parsed.ts > TMDB_CACHE_TTL_MS) {
      sessionStorage.removeItem(key);
      return null;
    }
    return parsed.data;
  } catch {
    return null;
  }
}

function setSessionCache<T>(key: string, data: T) {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.setItem(key, JSON.stringify({ ts: Date.now(), data }));
  } catch {
    // Ignore cache write failures.
  }
}

function normalizeDate(dateStr: string | null | undefined): Date | null {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return null;
  date.setHours(0, 0, 0, 0);
  return date;
}

async function fetchMovieListPages(
  path: string,
  maxResults: number,
  filter: (movie: TmdbMovie) => boolean,
  maxPages = 3
): Promise<TmdbMovie[]> {
  const results: TmdbMovie[] = [];
  const seenIds = new Set<number>();

  for (let page = 1; page <= maxPages && results.length < maxResults; page += 1) {
    const res = await fetch(buildUrl(path, { language: getTmdbLocale(), page: String(page) }));
    if (!res.ok) break;

    const data = await res.json();
    const pageResults = ((data.results as TmdbMovie[]) ?? []).filter(movie => {
      if (seenIds.has(movie.id)) return false;
      if (!filter(movie)) return false;
      seenIds.add(movie.id);
      return true;
    });

    results.push(...pageResults);

    const totalPages = typeof data.total_pages === 'number' ? data.total_pages : page;
    if (page >= totalPages) break;
  }

  return results.slice(0, maxResults);
}

export async function searchMovies(query: string, maxResults = 8): Promise<TmdbMovie[]> {
  if (!query.trim()) return [];
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);
  try {
    const res = await fetch(
      buildUrl('/search/movie', { query, include_adult: 'false', language: getTmdbLocale() }),
      { signal: controller.signal }
    );
    if (!res.ok) throw new Error('Failed to fetch movies');
    const data = await res.json();
    return ((data.results as TmdbMovie[]) ?? []).slice(0, maxResults);
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function fetchMovieDetails(tmdbId: number): Promise<TmdbMovie | null> {
  try {
    const res = await fetch(
      buildUrl(`/movie/${tmdbId}`, { append_to_response: 'credits', language: getTmdbLocale() })
    );
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export function getPosterUrl(posterPath: string | null): string | null {
  if (!posterPath) return null;
  return `${IMAGE_BASE}${posterPath}`;
}

export function extractDirector(movie: TmdbMovie): string {
  const director = movie.credits?.crew.find(c => c.job === 'Director');
  return director?.name ?? 'Unknown Director';
}

export function extractMovieData(movie: TmdbMovie) {
  return {
    tmdb_id: movie.id,
    title: movie.title || 'Unknown Title',
    director: extractDirector(movie),
    description: movie.overview || null,
    poster_url: getPosterUrl(movie.poster_path),
    release_date: movie.release_date || null,
    runtime: movie.runtime || null,
    genre: movie.genres?.[0]?.name || null,
  };
}

// --- Trending / Discover ---

export async function fetchTrendingMovies(maxResults = 12): Promise<TmdbMovie[]> {
  try {
    const res = await fetch(buildUrl('/trending/movie/week', { language: getTmdbLocale() }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbMovie[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

export async function fetchTrendingSeries(page = 1): Promise<{ results: TmdbSeries[]; hasMore: boolean }> {
  try {
    const res = await fetch(buildUrl('/trending/tv/week', { language: getTmdbLocale(), page: String(page) }));
    if (!res.ok) return { results: [], hasMore: false };
    const data = await res.json() as { results: TmdbSeries[]; total_pages: number };
    const totalPages = data.total_pages ?? 1;
    return {
      results: data.results ?? [],
      hasMore: page < totalPages && page < 5,
    };
  } catch {
    return { results: [], hasMore: false };
  }
}

export async function fetchUpcomingMovies(maxResults = 10): Promise<TmdbMovie[]> {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return await fetchMovieListPages('/movie/upcoming', maxResults, movie => {
      const releaseDate = normalizeDate(movie.release_date);
      return releaseDate != null && releaseDate > today;
    });
  } catch {
    return [];
  }
}

export async function fetchRecentMovies(maxResults = 10): Promise<TmdbMovie[]> {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const recentCutoff = new Date(today);
    recentCutoff.setDate(recentCutoff.getDate() - 45);
    return await fetchMovieListPages('/movie/now_playing', maxResults, movie => {
      const releaseDate = normalizeDate(movie.release_date);
      return releaseDate != null && releaseDate >= recentCutoff && releaseDate <= today;
    });
  } catch {
    return [];
  }
}

export async function fetchMoviesByGenre(genreQuery: string, maxResults = 12): Promise<TmdbMovie[]> {
  try {
    const res = await fetch(buildUrl('/search/movie', { query: genreQuery, include_adult: 'false', language: getTmdbLocale() }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbMovie[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

export async function fetchSeriesByGenre(genreQuery: string, maxResults = 12): Promise<TmdbSeries[]> {
  try {
    const res = await fetch(buildUrl('/search/tv', { query: genreQuery, include_adult: 'false', language: getTmdbLocale() }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbSeries[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

// --- TV Series ---

export async function searchSeries(query: string, maxResults = 8): Promise<TmdbSeries[]> {
  if (!query.trim()) return [];
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);
  try {
    const res = await fetch(
      buildUrl('/search/tv', { query, include_adult: 'false', language: getTmdbLocale() }),
      { signal: controller.signal }
    );
    if (!res.ok) throw new Error('Failed to fetch series');
    const data = await res.json();
    return ((data.results as TmdbSeries[]) ?? []).slice(0, maxResults);
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function fetchSeasonEpisodeCount(tmdbId: number, seasonNumber: number): Promise<number | null> {
  const details = await fetchSeasonDetails(tmdbId, seasonNumber);
  return details?.episodes.length ?? null;
}

export async function fetchSeasonDetails(tmdbId: number, seasonNumber: number): Promise<TmdbSeasonDetails | null> {
  const cacheKey = `nookmind_tmdb_season_${tmdbId}_${seasonNumber}`;
  const cached = getSessionCache<TmdbSeasonDetails>(cacheKey);
  if (cached) return cached;

  const requestKey = `${tmdbId}:${seasonNumber}`;
  if (!seasonDetailsCache.has(requestKey)) {
    seasonDetailsCache.set(requestKey, (async () => {
      try {
        const res = await fetch(buildUrl(`/tv/${tmdbId}/season/${seasonNumber}`, { language: getTmdbLocale() }));
        if (!res.ok) return null;
        const data = await res.json();
        setSessionCache(cacheKey, data);
        return data;
      } catch {
        return null;
      } finally {
        seasonDetailsCache.delete(requestKey);
      }
    })());
  }

  return seasonDetailsCache.get(requestKey)!;
}

export async function fetchSeriesDetails(tmdbId: number): Promise<TmdbSeries | null> {
  const cacheKey = `nookmind_tmdb_series_${tmdbId}`;
  const cached = getSessionCache<TmdbSeries>(cacheKey);
  if (cached) return cached;

  if (!seriesDetailsCache.has(tmdbId)) {
    seriesDetailsCache.set(tmdbId, (async () => {
      try {
        const res = await fetch(
          buildUrl(`/tv/${tmdbId}`, { language: getTmdbLocale() })
        );
        if (!res.ok) return null;
        const data = await res.json();
        setSessionCache(cacheKey, data);
        return data;
      } catch {
        return null;
      } finally {
        seriesDetailsCache.delete(tmdbId);
      }
    })());
  }

  return seriesDetailsCache.get(tmdbId)!;
}

export function extractSeriesData(series: TmdbSeries) {
  const next = series.next_episode_to_air;
  // Ne compter que les saisons avec au moins 1 épisode (exclut les saisons annoncées sans contenu)
  const airedSeasons = series.seasons
    ? series.seasons.filter(s => s.season_number > 0 && (s.episode_count ?? 0) > 0).length || null
    : series.number_of_seasons || null;
  return {
    tmdb_id: series.id,
    title: series.name || 'Unknown Title',
    creator: series.created_by?.[0]?.name || '',
    description: series.overview || null,
    poster_url: getPosterUrl(series.poster_path),
    first_air_date: series.first_air_date || null,
    seasons: airedSeasons,
    genre: series.genres?.[0]?.name || null,
    next_air_date: next?.air_date ?? null,
    next_season_number: next?.season_number ?? null,
  };
}
