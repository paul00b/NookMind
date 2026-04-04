import { useState, useEffect, useRef, useCallback } from 'react';
import { Tv } from 'lucide-react';
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
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const sentinelRef = useRef<HTMLDivElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null);

  const loadPage = useCallback(async (p: number) => {
    setLoading(true);
    const { results, hasMore: more } = await fetchTrendingSeries(p);
    setSeries(prev => {
      const existingIds = new Set(prev.map(s => s.id));
      const fresh = results.filter(s => !existingIds.has(s.id));
      return [...prev, ...fresh];
    });
    setHasMore(more);
    setLoading(false);
  }, []);

  useEffect(() => {
    void loadPage(1);
  }, [loadPage]);

  useEffect(() => {
    if (!sentinelRef.current) return;

    observerRef.current?.disconnect();

    if (!hasMore || loading) return;

    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          setPage(p => p + 1);
        }
      },
      { threshold: 0.1 }
    );
    observerRef.current.observe(sentinelRef.current);

    return () => observerRef.current?.disconnect();
  }, [hasMore, loading, series.length]);

  useEffect(() => {
    if (page === 1) return;
    void loadPage(page);
  }, [page, loadPage]);

  if (!loading && series.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('seriesHome.trendingTitle')}
      </h2>
      <div className="-mx-4 md:mx-0">
        <div
          className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth px-4 md:px-0 scroll-px-4 md:scroll-px-0"
          style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
        >
          {loading && series.length === 0
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : series.map(s => (
                <div
                  key={s.id}
                  onClick={() => onSelect(s)}
                  className="flex-shrink-0 snap-start group cursor-pointer"
                >
                  <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
                    {s.poster_path ? (
                      <img
                        src={getPosterUrl(s.poster_path)}
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
              ))}

          {/* Skeletons de chargement supplémentaire */}
          {loading && series.length > 0 &&
            Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={`sk-${i}`} />)
          }

          {/* Sentinel pour lazy loading */}
          <div ref={sentinelRef} className="flex-shrink-0 w-1" />
        </div>
      </div>
    </div>
  );
}
