import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Film, Flame } from 'lucide-react';
import { fetchTopRatedMoviesPaged, fetchTrendingMoviesPaged, fetchNowPlayingMoviesPaged, getPosterUrl } from '../lib/tmdb';
import type { TmdbMovie } from '../types';
import { useTranslation } from 'react-i18next';
import { useMovies } from '../context/MoviesContext';

type Category = 'top_rated' | 'trending' | 'now_playing';

interface TrendingMoviesSliderProps {
  onSelect: (movie: TmdbMovie) => void;
}

function SkeletonCard() {
  return (
    <div className="flex-shrink-0">
      <div className="w-20 md:w-28 aspect-[2/3] rounded-xl bg-gray-100 dark:bg-gray-800 animate-pulse mb-2" />
      <div className="w-20 md:w-28 h-3 rounded bg-gray-100 dark:bg-gray-800 animate-pulse" />
    </div>
  );
}

const fetchers: Record<Category, (page: number) => Promise<{ results: TmdbMovie[]; hasMore: boolean }>> = {
  top_rated:   fetchTopRatedMoviesPaged,
  trending:    fetchTrendingMoviesPaged,
  now_playing: fetchNowPlayingMoviesPaged,
};

export default function TrendingMoviesSlider({ onSelect }: TrendingMoviesSliderProps) {
  const { t } = useTranslation();
  const { movies: libraryMovies } = useMovies();
  const trackedIds = useMemo(
    () => new Set(libraryMovies.filter(m => m.tmdb_id != null).map(m => m.tmdb_id!)),
    [libraryMovies]
  );
  const [category, setCategory] = useState<Category>('trending');
  const [movies, setMovies] = useState<TmdbMovie[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const nextPageRef = useRef(1);
  const fetchingRef = useRef(false);
  const categoryRef = useRef<Category>('top_rated');
  const sentinelRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const fetchNext = useCallback((cat: Category) => {
    if (fetchingRef.current) return;
    fetchingRef.current = true;
    const p = nextPageRef.current;
    fetchers[cat](p).then(({ results, hasMore: more }) => {
      if (categoryRef.current !== cat) { fetchingRef.current = false; return; }
      nextPageRef.current = p + 1;
      fetchingRef.current = false;
      setMovies(prev => {
        const ids = new Set(prev.map(m => m.id));
        return [...prev, ...results.filter(m => !ids.has(m.id))];
      });
      setHasMore(more);
      if (p === 1) setInitialLoading(false);
      else setLoadingMore(false);
    }).catch(() => {
      fetchingRef.current = false;
      if (p === 1) setInitialLoading(false);
      else setLoadingMore(false);
    });
  }, []);

  useEffect(() => {
    categoryRef.current = category;
    fetchingRef.current = false;
    nextPageRef.current = 1;
    if (scrollRef.current) scrollRef.current.scrollTo({ left: 0, behavior: 'instant' });
    Promise.resolve().then(() => {
      setMovies([]);
      setHasMore(true);
      setInitialLoading(true);
      setLoadingMore(false);
      fetchNext(category);
    });
  }, [category, fetchNext]);

  useEffect(() => {
    if (!sentinelRef.current || !hasMore || loadingMore || initialLoading) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          setLoadingMore(true);
          fetchNext(categoryRef.current);
        }
      },
      { threshold: 0.1 }
    );
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, loadingMore, initialLoading, fetchNext]);

  const tabs: { key: Category; label: string }[] = [
    { key: 'trending',    label: t('movieHome.categoryTrending') },
    { key: 'now_playing', label: t('movieHome.categoryNowPlaying') },
    { key: 'top_rated',   label: t('movieHome.categoryTopRated') },
  ];

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-amber-500 dark:text-amber-400 uppercase tracking-wider mb-3 px-1 flex items-center gap-1.5">
        <Flame size={14} />
        {t('movieHome.trendingTitle')}
      </h2>

      <div className="flex gap-2 mb-3 px-1">
        {tabs.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => setCategory(key)}
            className={`px-3 py-1 rounded-full text-xs font-semibold transition-colors ${
              category === key
                ? 'bg-amber-500 text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 hover:bg-amber-500/10 hover:text-amber-600 dark:hover:text-amber-400'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="-mx-4 md:mx-0">
        <div
          ref={scrollRef}
          className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth px-4 md:px-0 scroll-px-4 md:scroll-px-0"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          {initialLoading
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : movies.filter(movie => !trackedIds.has(movie.id)).map(movie => {
                const poster = getPosterUrl(movie.poster_path) ?? undefined;
                return (
                  <div
                    key={movie.id}
                    onClick={() => onSelect(movie)}
                    className="flex-shrink-0 snap-start group cursor-pointer"
                  >
                    <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
                      {poster ? (
                        <img src={poster} alt={movie.title} className="w-full h-full object-cover" loading="lazy" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <Film size={22} className="text-gray-300 dark:text-gray-600" />
                        </div>
                      )}
                    </div>
                    <div className="w-20 md:w-28 text-left">
                      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 line-clamp-2 leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                        {movie.title}
                      </p>
                    </div>
                  </div>
                );
              })}

          {loadingMore && Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={`sk-${i}`} />)}
          <div ref={sentinelRef} className="flex-shrink-0 w-1" />
        </div>
      </div>
    </div>
  );
}
