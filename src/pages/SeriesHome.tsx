import { useState, useEffect, useRef, useCallback } from 'react';
import { Search, Plus, X, Tv, CheckCircle2, Eye } from 'lucide-react';
import { searchSeries as searchTmdbSeries, extractSeriesData, fetchSeriesDetails, getPosterUrl } from '../lib/tmdb';
import type { TmdbSeries, Series } from '../types';
import AddSeriesModal from '../components/AddSeriesModal';
import SeriesRatingsModal from '../components/SeriesRatingsModal';
import SeriesDetailModal from '../components/SeriesDetailModal';
import StarRating from '../components/StarRating';
import { useSeries } from '../context/SeriesContext';
import { useTranslation } from 'react-i18next';

function useDebounce<T>(value: T, delay: number): T {
  const [dv, setDv] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDv(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return dv;
}

function WantToWatchSlider({ onSelect }: { onSelect: (s: Series) => void }) {
  const { series } = useSeries();
  const { t } = useTranslation();

  const wantToWatch = series
    .filter(s => s.status === 'want_to_watch')
    .slice(0, 10);

  if (wantToWatch.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('seriesHome.wantToWatch')}
      </h2>
      <div className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
        {wantToWatch.map(s => (
          <div key={s.id} onClick={() => onSelect(s)} className="flex-shrink-0 snap-start group cursor-pointer">
            <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
              {s.poster_url ? (
                <img src={s.poster_url} alt={s.title} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={22} className="text-gray-300 dark:text-gray-600" />
                </div>
              )}
            </div>
            <div className="w-20 md:w-28 text-left">
              <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                {s.title}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function WatchingSlider({ onSelect }: { onSelect: (s: Series) => void }) {
  const { series } = useSeries();
  const { t } = useTranslation();

  const watching = series
    .filter(s => s.status === 'watching')
    .slice(0, 10);

  if (watching.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('seriesHome.watching')}
      </h2>
      <div className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
        {watching.map(s => (
          <div key={s.id} onClick={() => onSelect(s)} className="flex-shrink-0 snap-start group cursor-pointer">
            <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200 relative">
              {s.poster_url ? (
                <img src={s.poster_url} alt={s.title} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={22} className="text-gray-300 dark:text-gray-600" />
                </div>
              )}
              <span className="absolute bottom-1.5 right-1.5 text-[10px] font-bold bg-blue-500 text-white px-1.5 py-0.5 rounded-md">
                S{s.watched_seasons.length}/{s.seasons ?? '?'}
              </span>
            </div>
            <div className="w-20 md:w-28 text-left">
              <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                {s.title}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function LastWatchedSlider({ onSelect }: { onSelect: (s: Series) => void }) {
  const { series } = useSeries();
  const { t } = useTranslation();

  const lastWatched = series
    .filter(s => s.status === 'watched')
    .slice(0, 10);

  if (lastWatched.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('seriesHome.lastWatched')}
      </h2>
      <div className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
        {lastWatched.map(s => (
          <div key={s.id} onClick={() => onSelect(s)} className="flex-shrink-0 snap-start group cursor-pointer">
            <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
              {s.poster_url ? (
                <img src={s.poster_url} alt={s.title} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={22} className="text-gray-300 dark:text-gray-600" />
                </div>
              )}
            </div>
            <div className="w-20 md:w-28 text-left">
              <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                {s.title}
              </p>
              {s.rating && (
                <div className="mt-0.5">
                  <StarRating value={s.rating} readonly size={10} />
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

export default function SeriesHome() {
  const { t } = useTranslation();
  const { series: allSeries } = useSeries();
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 600);
  const [results, setResults] = useState<TmdbSeries[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReturnType<typeof extractSeriesData> | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [ratingsTarget, setRatingsTarget] = useState<TmdbSeries | null>(null);
  const [selectedSeries, setSelectedSeries] = useState<Series | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const doSearch = useCallback(async (q: string) => {
    if (q.trim().length < 3) { setResults([]); setDropdownOpen(false); return; }
    setSearching(true);
    setSearchError('');
    try {
      const res = await searchTmdbSeries(q);
      setResults(res);
      setDropdownOpen(true);
    } catch {
      setSearchError(t('seriesHome.searchTimeout'));
      setDropdownOpen(false);
    } finally {
      setSearching(false);
    }
  }, [t]);

  useEffect(() => { doSearch(debouncedQuery); }, [debouncedQuery, doSearch]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSelectSeries = async (tmdbSeries: TmdbSeries) => {
    const details = await fetchSeriesDetails(tmdbSeries.id);
    const data = extractSeriesData(details ?? tmdbSeries);
    setPrefill(data);
    setModalOpen(true);
    setDropdownOpen(false);
    setQuery('');
  };

  const handleAddManually = () => {
    setPrefill(null);
    setModalOpen(true);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4 py-12">
      <div className="text-center mb-10">
        <h1 className="font-serif text-4xl md:text-5xl font-bold text-gray-900 dark:text-gray-100 mb-3">
          {t('seriesHome.title')}
        </h1>
        <p className="text-gray-500 dark:text-gray-400 text-lg max-w-md mx-auto">
          {t('seriesHome.subtitle')}
        </p>
      </div>

      <div className="w-full max-w-xl relative" ref={dropdownRef}>
        <div className="relative">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onFocus={() => results.length > 0 && setDropdownOpen(true)}
            placeholder={t('seriesHome.searchPlaceholder')}
            className="input rounded-full pl-11 pr-10 py-3.5 text-base shadow-sm"
            autoComplete="off"
          />
          {query && (
            <button
              onClick={() => { setQuery(''); setResults([]); setDropdownOpen(false); inputRef.current?.focus(); }}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            >
              <X size={16} />
            </button>
          )}
        </div>

        {(dropdownOpen || searching) && query && (
          <div className="absolute top-full mt-2 left-0 right-0 card shadow-xl z-20 overflow-hidden animate-slide-up">
            {searching ? (
              <div className="p-3 space-y-2">
                {[1, 2, 3].map(i => (
                  <div key={i} className="flex gap-3 animate-pulse p-2">
                    <div className="w-10 h-14 bg-gray-200 dark:bg-gray-700 rounded-lg flex-shrink-0" />
                    <div className="flex-1 space-y-2 py-1">
                      <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-3/4" />
                      <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-1/2" />
                    </div>
                  </div>
                ))}
              </div>
            ) : results.length === 0 ? (
              <div className="p-6 text-center text-gray-500 dark:text-gray-400 text-sm">
                {t('seriesHome.noSeriesFound', { query })}
              </div>
            ) : (
              <ul>
                {results.map((s, idx) => {
                  const posterUrl = getPosterUrl(s.poster_path);
                  const alreadyAdded = allSeries.some(
                    existing => normalize(existing.title) === normalize(s.name)
                  );
                  return (
                    <li key={s.id}>
                      {idx > 0 && <div className="border-t border-black/[0.06] dark:border-white/[0.06] mx-3" />}
                      <div className="flex items-center hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors">
                        <button
                          onClick={() => handleSelectSeries(s)}
                          className="flex-1 flex items-center gap-3 px-4 py-3 text-left min-w-0"
                        >
                          <div className="w-10 h-14 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100 dark:bg-gray-800">
                            {posterUrl ? (
                              <img src={posterUrl} alt="" className="w-full h-full object-cover" />
                            ) : (
                              <div className="w-full h-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                                <Tv size={16} className="text-gray-400" />
                              </div>
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">{s.name}</p>
                            <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                              {s.first_air_date?.slice(0, 4) || '—'}
                            </p>
                          </div>
                          {alreadyAdded && (
                            <span className="flex-shrink-0 flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded-full">
                              <CheckCircle2 size={12} />
                              {t('seriesHome.inList')}
                            </span>
                          )}
                        </button>
                        <button
                          onClick={(e) => { e.stopPropagation(); setRatingsTarget(s); }}
                          className="flex-shrink-0 mr-3 p-1.5 rounded-lg text-gray-400 hover:text-amber-500 hover:bg-amber-500/10 transition-colors"
                          title={t('seriesDetail.viewImdbRatings')}
                        >
                          <Eye size={15} />
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </div>

      {searchError && <p className="mt-3 text-sm text-red-500">{searchError}</p>}

      <div className="mt-6 flex items-center gap-3">
        <div className="w-16 h-px bg-gray-200 dark:bg-gray-700" />
        <span className="text-sm text-gray-400 dark:text-gray-500">{t('seriesHome.or')}</span>
        <div className="w-16 h-px bg-gray-200 dark:bg-gray-700" />
      </div>
      <button onClick={handleAddManually} className="mt-4 btn-ghost flex items-center gap-2 text-sm">
        <Plus size={16} /> {t('seriesHome.addManually')}
      </button>

      <WatchingSlider onSelect={setSelectedSeries} />
      <WantToWatchSlider onSelect={setSelectedSeries} />
      <LastWatchedSlider onSelect={setSelectedSeries} />

      {modalOpen && (
        <AddSeriesModal
          prefill={prefill ?? undefined}
          onClose={() => { setModalOpen(false); setPrefill(null); }}
        />
      )}

      {selectedSeries && (
        <SeriesDetailModal
          series={selectedSeries}
          onClose={() => setSelectedSeries(null)}
        />
      )}

      {ratingsTarget && (
        <SeriesRatingsModal
          isOpen={true}
          onClose={() => setRatingsTarget(null)}
          title={ratingsTarget.name}
          description={ratingsTarget.overview || null}
          posterUrl={getPosterUrl(ratingsTarget.poster_path)}
          firstAirDate={ratingsTarget.first_air_date || null}
          totalSeasons={(ratingsTarget as any).number_of_seasons ?? null}
          showAddButton={true}
          onAdd={() => handleSelectSeries(ratingsTarget)}
        />
      )}
    </div>
  );
}
