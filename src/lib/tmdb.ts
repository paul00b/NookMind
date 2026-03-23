import type { TmdbMovie, TmdbSeries } from '../types';

const BASE_URL = 'https://api.themoviedb.org/3';
const IMAGE_BASE = 'https://image.tmdb.org/t/p/w500';
const API_KEY = import.meta.env.VITE_TMDB_API_KEY as string | undefined;

function buildUrl(path: string, params: Record<string, string> = {}): string {
  const url = new URL(`${BASE_URL}${path}`);
  if (API_KEY) url.searchParams.set('api_key', API_KEY);
  Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
  return url.toString();
}

export async function searchMovies(query: string, maxResults = 8): Promise<TmdbMovie[]> {
  if (!query.trim()) return [];
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);
  try {
    const res = await fetch(
      buildUrl('/search/movie', { query, include_adult: 'false', language: 'en-US' }),
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
      buildUrl(`/movie/${tmdbId}`, { append_to_response: 'credits', language: 'en-US' })
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
    const res = await fetch(buildUrl('/trending/movie/week', { language: 'en-US' }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbMovie[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

export async function fetchTrendingSeries(maxResults = 12): Promise<TmdbSeries[]> {
  try {
    const res = await fetch(buildUrl('/trending/tv/week', { language: 'en-US' }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbSeries[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

export async function fetchMoviesByGenre(genreQuery: string, maxResults = 12): Promise<TmdbMovie[]> {
  try {
    const res = await fetch(buildUrl('/search/movie', { query: genreQuery, include_adult: 'false', language: 'en-US' }));
    if (!res.ok) return [];
    const data = await res.json();
    return ((data.results as TmdbMovie[]) ?? []).slice(0, maxResults);
  } catch {
    return [];
  }
}

export async function fetchSeriesByGenre(genreQuery: string, maxResults = 12): Promise<TmdbSeries[]> {
  try {
    const res = await fetch(buildUrl('/search/tv', { query: genreQuery, include_adult: 'false', language: 'en-US' }));
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
      buildUrl('/search/tv', { query, include_adult: 'false', language: 'en-US' }),
      { signal: controller.signal }
    );
    if (!res.ok) throw new Error('Failed to fetch series');
    const data = await res.json();
    return ((data.results as TmdbSeries[]) ?? []).slice(0, maxResults);
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function fetchSeriesDetails(tmdbId: number): Promise<TmdbSeries | null> {
  try {
    const res = await fetch(
      buildUrl(`/tv/${tmdbId}`, { language: 'en-US' })
    );
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export function extractSeriesData(series: TmdbSeries) {
  return {
    tmdb_id: series.id,
    title: series.name || 'Unknown Title',
    creator: series.created_by?.[0]?.name || '',
    description: series.overview || null,
    poster_url: getPosterUrl(series.poster_path),
    first_air_date: series.first_air_date || null,
    seasons: series.number_of_seasons || null,
    genre: series.genres?.[0]?.name || null,
  };
}
