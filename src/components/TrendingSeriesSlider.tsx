import { useState, useEffect, useRef, useCallback } from 'react';
import { Tv, Flame } from 'lucide-react';
import { fetchTrendingSeries, getPosterUrl } from '../lib/tmdb';
import type { TmdbSeries } from '../types';
import { useTranslation } from 'react-i18next';

interface TrendingSeriesSliderProps {
  onSelect: (series: TmdbSeries) => void;
}

function SkeletonCard() {
  return (
    <div className="flex-shrink-0">
      <div className="w-20 md:w-28 aspect-[2/3] rounded-xl bg-gray-100 dark:bg-gray-800 animate-pulse mb-2" />
      <div className="w-20 md:w-28 h-3 rounded bg-gray-100 dark:bg-gray-800 animate-pulse" />
    </div>
  );
}

export default function TrendingSeriesSlider({ onSelect }: TrendingSeriesSliderProps) {
  const { t } = useTranslation();
  const [series, setSeries] = useState<TmdbSeries[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const nextPageRef = useRef(1);
  const fetchingRef = useRef(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  const fetchNext = useCallback(() => {
    if (fetchingRef.current) return;
    fetchingRef.current = true;
    const p = nextPageRef.current;
    fetchTrendingSeries(p).then(({ results, hasMore: more }) => {
      nextPageRef.current = p + 1;
      fetchingRef.current = false;
      setSeries(prev => {
        const ids = new Set(prev.map(s => s.id));
        return [...prev, ...results.filter(s => !ids.has(s.id))];
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

  // Chargement initial
  useEffect(() => {
    fetchNext();
  }, [fetchNext]);

  // Observer pour le lazy loading
  useEffect(() => {
    if (!sentinelRef.current || !hasMore || loadingMore || initialLoading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          setLoadingMore(true);
          fetchNext();
        }
      },
      { threshold: 0.1 }
    );
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, loadingMore, initialLoading, fetchNext]);

  if (!initialLoading && series.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-amber-500 dark:text-amber-400 uppercase tracking-wider mb-3 px-1 flex items-center gap-1.5">
        <Flame size={14} />
        {t('seriesHome.trendingTitle')}
      </h2>
      <div className="-mx-4 md:mx-0">
        <div
          className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth px-4 md:px-0 scroll-px-4 md:scroll-px-0"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          {initialLoading
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : series.map(s => {
                const poster = getPosterUrl(s.poster_path) ?? undefined;
                return (
                  <div
                    key={s.id}
                    onClick={() => onSelect(s)}
                    className="flex-shrink-0 snap-start group cursor-pointer"
                  >
                    <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
                      {poster ? (
                        <img
                          src={poster}
                          alt={s.name}
                          className="w-full h-full object-cover"
                          loading="lazy"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <Tv size={22} className="text-gray-300 dark:text-gray-600" />
                        </div>
                      )}
                    </div>
                    <div className="w-20 md:w-28 text-left">
                      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 line-clamp-2 leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                        {s.name}
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
