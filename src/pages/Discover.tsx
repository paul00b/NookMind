import { useState, useEffect, useCallback } from 'react';
import { Compass, Tv, BookOpen, Film, Sparkles, TrendingUp } from 'lucide-react';
import { useMediaMode } from '../context/MediaModeContext';
import { useBooks } from '../context/BooksContext';
import { useMovies } from '../context/MoviesContext';
import { useSeries } from '../context/SeriesContext';
import { useTranslation } from 'react-i18next';
import { fetchTrendingMovies, fetchTrendingSeries, fetchMoviesByGenre, fetchSeriesByGenre, getPosterUrl } from '../lib/tmdb';
import { fetchByGenre as fetchBooksByGenre, fetchByAuthor } from '../lib/googleBooks';
import type { TmdbMovie, TmdbSeries, GoogleBookVolume } from '../types';

const CACHE_KEY = 'nookmind_discover_cache';
const CACHE_TTL = 24 * 60 * 60 * 1000; // 24h

interface CacheEntry<T> {
  data: T;
  ts: number;
}

function getCache<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(`${CACHE_KEY}_${key}`);
    if (!raw) return null;
    const entry: CacheEntry<T> = JSON.parse(raw);
    if (Date.now() - entry.ts > CACHE_TTL) {
      localStorage.removeItem(`${CACHE_KEY}_${key}`);
      return null;
    }
    return entry.data;
  } catch {
    return null;
  }
}

function setCache<T>(key: string, data: T) {
  try {
    localStorage.setItem(`${CACHE_KEY}_${key}`, JSON.stringify({ data, ts: Date.now() }));
  } catch { /* quota exceeded — ignore */ }
}

function topGenres(items: { genre: string | null }[], max = 3): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.genre) counts[i.genre] = (counts[i.genre] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

function topAuthors(items: { author: string }[], max = 2): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.author && i.author !== 'Unknown Author') counts[i.author] = (counts[i.author] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

// --- Horizontal scroll card row ---

function PosterRow({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
      {children}
    </div>
  );
}

function MovieCard({ movie }: { movie: TmdbMovie }) {
  const poster = getPosterUrl(movie.poster_path);
  return (
    <div className="flex-shrink-0 snap-start w-28 md:w-32">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {poster ? (
          <img src={poster} alt={movie.title} className="w-full h-full object-cover" loading="lazy" />
        ) : (
          <div className="w-full h-full flex items-center justify-center"><Film size={22} className="text-gray-300 dark:text-gray-600" /></div>
        )}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight">{movie.title}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{movie.release_date?.slice(0, 4) || '—'}</p>
    </div>
  );
}

function SeriesCard({ series }: { series: TmdbSeries }) {
  const poster = getPosterUrl(series.poster_path);
  return (
    <div className="flex-shrink-0 snap-start w-28 md:w-32">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {poster ? (
          <img src={poster} alt={series.name} className="w-full h-full object-cover" loading="lazy" />
        ) : (
          <div className="w-full h-full flex items-center justify-center"><Tv size={22} className="text-gray-300 dark:text-gray-600" /></div>
        )}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight">{series.name}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{series.first_air_date?.slice(0, 4) || '—'}</p>
    </div>
  );
}

function BookCard({ volume }: { volume: GoogleBookVolume }) {
  const info = volume.volumeInfo;
  const cover = info.imageLinks?.thumbnail?.replace('http://', 'https://').replace('zoom=1', 'zoom=2') || null;
  return (
    <div className="flex-shrink-0 snap-start w-28 md:w-32">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {cover ? (
          <img src={cover} alt={info.title} className="w-full h-full object-cover" loading="lazy" />
        ) : (
          <div className="w-full h-full flex items-center justify-center"><BookOpen size={22} className="text-gray-300 dark:text-gray-600" /></div>
        )}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight">{info.title}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{info.authors?.join(', ') || '—'}</p>
    </div>
  );
}

function SectionTitle({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <h2 className="flex items-center gap-2 text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
      {icon} {label}
    </h2>
  );
}

function SkeletonRow() {
  return (
    <div className="flex gap-3 overflow-hidden">
      {[1, 2, 3, 4].map(i => (
        <div key={i} className="flex-shrink-0 w-28 md:w-32 animate-pulse">
          <div className="aspect-[2/3] rounded-xl bg-gray-200 dark:bg-gray-700 mb-2" />
          <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-3/4 mb-1" />
          <div className="h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full w-1/2" />
        </div>
      ))}
    </div>
  );
}

// --- Mode-specific discover components ---

function DiscoverBooks() {
  const { books } = useBooks();
  const { t } = useTranslation();
  const [trending, setTrending] = useState<GoogleBookVolume[]>([]);
  const [forYou, setForYou] = useState<{ label: string; items: GoogleBookVolume[] }[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    // Trending
    let trendingData = getCache<GoogleBookVolume[]>('books_trending');
    if (!trendingData) {
      trendingData = await fetchBooksByGenre('bestseller', 12);
      setCache('books_trending', trendingData);
    }
    setTrending(trendingData);

    // For you: based on genres + authors
    const genres = topGenres(books);
    const authors = topAuthors(books);
    const sections: { label: string; items: GoogleBookVolume[] }[] = [];

    for (const genre of genres) {
      const cacheKey = `books_genre_${genre}`;
      let items = getCache<GoogleBookVolume[]>(cacheKey);
      if (!items) {
        items = await fetchBooksByGenre(genre, 8);
        setCache(cacheKey, items);
      }
      if (items.length > 0) sections.push({ label: genre, items });
    }

    for (const author of authors) {
      const cacheKey = `books_author_${author}`;
      let items = getCache<GoogleBookVolume[]>(cacheKey);
      if (!items) {
        items = await fetchByAuthor(author, 8);
        setCache(cacheKey, items);
      }
      if (items.length > 0) sections.push({ label: t('discover.moreBy', { name: author }), items });
    }

    setForYou(sections);
    setLoading(false);
  }, [books, t]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-8">
      <div>
        <SectionTitle icon={<TrendingUp size={14} />} label={t('discover.trending')} />
        {loading ? <SkeletonRow /> : trending.length > 0 ? (
          <PosterRow>{trending.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow>
        ) : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>}
      </div>
      {forYou.map(section => (
        <div key={section.label}>
          <SectionTitle icon={<Sparkles size={14} />} label={section.label} />
          <PosterRow>{section.items.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow>
        </div>
      ))}
    </div>
  );
}

function DiscoverMovies() {
  const { movies } = useMovies();
  const { t } = useTranslation();
  const [trending, setTrending] = useState<TmdbMovie[]>([]);
  const [forYou, setForYou] = useState<{ label: string; items: TmdbMovie[] }[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    let trendingData = getCache<TmdbMovie[]>('movies_trending');
    if (!trendingData) {
      trendingData = await fetchTrendingMovies(12);
      setCache('movies_trending', trendingData);
    }
    setTrending(trendingData);

    const genres = topGenres(movies);
    const sections: { label: string; items: TmdbMovie[] }[] = [];
    for (const genre of genres) {
      const cacheKey = `movies_genre_${genre}`;
      let items = getCache<TmdbMovie[]>(cacheKey);
      if (!items) {
        items = await fetchMoviesByGenre(genre, 12);
        setCache(cacheKey, items);
      }
      if (items.length > 0) sections.push({ label: genre, items });
    }

    setForYou(sections);
    setLoading(false);
  }, [movies]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-8">
      <div>
        <SectionTitle icon={<TrendingUp size={14} />} label={t('discover.trending')} />
        {loading ? <SkeletonRow /> : trending.length > 0 ? (
          <PosterRow>{trending.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow>
        ) : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>}
      </div>
      {forYou.map(section => (
        <div key={section.label}>
          <SectionTitle icon={<Sparkles size={14} />} label={section.label} />
          <PosterRow>{section.items.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow>
        </div>
      ))}
    </div>
  );
}

function DiscoverSeries() {
  const { series } = useSeries();
  const { t } = useTranslation();
  const [trending, setTrending] = useState<TmdbSeries[]>([]);
  const [forYou, setForYou] = useState<{ label: string; items: TmdbSeries[] }[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    let trendingData = getCache<TmdbSeries[]>('series_trending');
    if (!trendingData) {
      trendingData = await fetchTrendingSeries(12);
      setCache('series_trending', trendingData);
    }
    setTrending(trendingData);

    const genres = topGenres(series);
    const sections: { label: string; items: TmdbSeries[] }[] = [];
    for (const genre of genres) {
      const cacheKey = `series_genre_${genre}`;
      let items = getCache<TmdbSeries[]>(cacheKey);
      if (!items) {
        items = await fetchSeriesByGenre(genre, 12);
        setCache(cacheKey, items);
      }
      if (items.length > 0) sections.push({ label: genre, items });
    }

    setForYou(sections);
    setLoading(false);
  }, [series]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-8">
      <div>
        <SectionTitle icon={<TrendingUp size={14} />} label={t('discover.trending')} />
        {loading ? <SkeletonRow /> : trending.length > 0 ? (
          <PosterRow>{trending.map(s => <SeriesCard key={s.id} series={s} />)}</PosterRow>
        ) : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>}
      </div>
      {forYou.map(section => (
        <div key={section.label}>
          <SectionTitle icon={<Sparkles size={14} />} label={section.label} />
          <PosterRow>{section.items.map(s => <SeriesCard key={s.id} series={s} />)}</PosterRow>
        </div>
      ))}
    </div>
  );
}

export default function Discover() {
  const { mode } = useMediaMode();
  const { t } = useTranslation();

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 bg-amber-500/10 rounded-xl flex items-center justify-center">
          <Compass size={20} className="text-amber-500" />
        </div>
        <div>
          <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100">{t('discover.title')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">{t('discover.subtitle')}</p>
        </div>
      </div>

      {mode === 'books' && <DiscoverBooks />}
      {mode === 'movies' && <DiscoverMovies />}
      {mode === 'series' && <DiscoverSeries />}
    </div>
  );
}
