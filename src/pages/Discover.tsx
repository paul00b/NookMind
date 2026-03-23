import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Compass, Tv, BookOpen, Film, Sparkles, TrendingUp, Settings2, Eye, EyeOff, X, Plus, GripVertical, CheckCircle2 } from 'lucide-react';
import { useMediaMode } from '../context/MediaModeContext';
import { useBooks } from '../context/BooksContext';
import { useMovies } from '../context/MoviesContext';
import { useSeries } from '../context/SeriesContext';
import { useTranslation } from 'react-i18next';
import { fetchTrendingMovies, fetchTrendingSeries, fetchMoviesByGenre, fetchSeriesByGenre, fetchMovieDetails, fetchSeriesDetails, extractMovieData, extractSeriesData, getPosterUrl } from '../lib/tmdb';
import { fetchByGenre as fetchBooksByGenre, fetchByAuthor, extractBookData } from '../lib/googleBooks';
import AddBookModal from '../components/AddBookModal';
import AddMovieModal from '../components/AddMovieModal';
import AddSeriesModal from '../components/AddSeriesModal';
import type { TmdbMovie, TmdbSeries, GoogleBookVolume } from '../types';

// --- Cache helpers ---

const CACHE_KEY = 'nookmind_discover_cache';
const CACHE_TTL = 24 * 60 * 60 * 1000;

function getCache<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(`${CACHE_KEY}_${key}`);
    if (!raw) return null;
    const entry = JSON.parse(raw);
    if (Date.now() - entry.ts > CACHE_TTL) { localStorage.removeItem(`${CACHE_KEY}_${key}`); return null; }
    return entry.data;
  } catch { return null; }
}

function setCache<T>(key: string, data: T) {
  try { localStorage.setItem(`${CACHE_KEY}_${key}`, JSON.stringify({ data, ts: Date.now() })); } catch {}
}

// --- Section config persistence ---

interface SectionConfig {
  id: string;
  visible: boolean;
}

const CONFIG_KEY = 'nookmind_discover_sections';

function loadSectionConfig(mode: string): SectionConfig[] {
  try {
    const raw = localStorage.getItem(`${CONFIG_KEY}_${mode}`);
    return raw ? JSON.parse(raw) : [];
  } catch { return []; }
}

function saveSectionConfig(mode: string, config: SectionConfig[]) {
  try { localStorage.setItem(`${CONFIG_KEY}_${mode}`, JSON.stringify(config)); } catch {}
}

function mergeConfig(saved: SectionConfig[], available: string[]): SectionConfig[] {
  const map = new Map(saved.map(s => [s.id, s]));
  const ordered: SectionConfig[] = [];
  for (const s of saved) {
    if (available.includes(s.id)) ordered.push(s);
  }
  for (const id of available) {
    if (!map.has(id)) ordered.push({ id, visible: true });
  }
  return ordered;
}

// --- Collection analysis helpers ---

const normalizeStr = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

function topGenres(items: { genre: string | null }[], max = 5): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.genre) counts[i.genre] = (counts[i.genre] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

function topAuthors(items: { author: string }[], max = 3): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.author && i.author !== 'Unknown Author') counts[i.author] = (counts[i.author] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

function topCreators(items: { creator: string }[], max = 3): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.creator) counts[i.creator] = (counts[i.creator] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

function topDirectors(items: { director: string }[], max = 3): string[] {
  const counts: Record<string, number> = {};
  items.forEach(i => { if (i.director && i.director !== 'Unknown Director') counts[i.director] = (counts[i.director] || 0) + 1; });
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, max).map(e => e[0]);
}

// --- UI components ---

function PosterRow({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth" style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}>
      {children}
    </div>
  );
}

function MovieCard({ movie, onClick }: { movie: TmdbMovie; onClick: () => void }) {
  const poster = getPosterUrl(movie.poster_path);
  return (
    <button onClick={onClick} className="flex-shrink-0 snap-start w-28 md:w-32 text-left group">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
        {poster ? <img src={poster} alt={movie.title} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><Film size={22} className="text-gray-300 dark:text-gray-600" /></div>}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">{movie.title}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{movie.release_date?.slice(0, 4) || '—'}</p>
    </button>
  );
}

function SeriesTmdbCard({ series, onClick }: { series: TmdbSeries; onClick: () => void }) {
  const poster = getPosterUrl(series.poster_path);
  return (
    <button onClick={onClick} className="flex-shrink-0 snap-start w-28 md:w-32 text-left group">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
        {poster ? <img src={poster} alt={series.name} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><Tv size={22} className="text-gray-300 dark:text-gray-600" /></div>}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">{series.name}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{series.first_air_date?.slice(0, 4) || '—'}</p>
    </button>
  );
}

function BookCard({ volume, onClick }: { volume: GoogleBookVolume; onClick: () => void }) {
  const info = volume.volumeInfo;
  const cover = info.imageLinks?.thumbnail?.replace('http://', 'https://').replace('zoom=1', 'zoom=2') || null;
  return (
    <button onClick={onClick} className="flex-shrink-0 snap-start w-28 md:w-32 text-left group">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
        {cover ? <img src={cover} alt={info.title} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><BookOpen size={22} className="text-gray-300 dark:text-gray-600" /></div>}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">{info.title}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{info.authors?.join(', ') || '—'}</p>
    </button>
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

// --- Settings panel with drag & drop ---

function SettingsPanel({ sections, onToggle, onReorder, onClose, onAddCustom }: {
  sections: SectionConfig[];
  onToggle: (id: string) => void;
  onReorder: (newOrder: SectionConfig[]) => void;
  onClose: () => void;
  onAddCustom: () => void;
}) {
  const { t } = useTranslation();
  const [dragIdx, setDragIdx] = useState<number | null>(null);
  const [overIdx, setOverIdx] = useState<number | null>(null);
  const itemRefs = useRef<(HTMLDivElement | null)[]>([]);
  const touchState = useRef<{ idx: number } | null>(null);

  const getItemIndexAtY = useCallback((clientY: number): number | null => {
    for (let i = 0; i < itemRefs.current.length; i++) {
      const el = itemRefs.current[i];
      if (!el) continue;
      const rect = el.getBoundingClientRect();
      if (clientY >= rect.top && clientY <= rect.bottom) return i;
    }
    return null;
  }, []);

  const finishDrag = useCallback((fromIdx: number, toIdx: number | null) => {
    if (toIdx !== null && toIdx !== fromIdx) {
      const next = [...sections];
      const [moved] = next.splice(fromIdx, 1);
      next.splice(toIdx, 0, moved);
      onReorder(next);
    }
    setDragIdx(null);
    setOverIdx(null);
    touchState.current = null;
  }, [sections, onReorder]);

  const handleMouseDown = useCallback((idx: number) => (e: React.MouseEvent) => {
    e.preventDefault();
    setDragIdx(idx);
    setOverIdx(idx);
    const onMouseMove = (ev: MouseEvent) => {
      const target = getItemIndexAtY(ev.clientY);
      if (target !== null) setOverIdx(target);
    };
    const onMouseUp = (ev: MouseEvent) => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
      const target = getItemIndexAtY(ev.clientY);
      finishDrag(idx, target);
    };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }, [getItemIndexAtY, finishDrag]);

  const handleTouchStart = useCallback((idx: number) => () => {
    touchState.current = { idx };
    setDragIdx(idx);
    setOverIdx(idx);
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!touchState.current) return;
    e.preventDefault();
    const target = getItemIndexAtY(e.touches[0].clientY);
    if (target !== null) setOverIdx(target);
  }, [getItemIndexAtY]);

  const handleTouchEnd = useCallback(() => {
    if (!touchState.current || dragIdx === null) return;
    finishDrag(dragIdx, overIdx);
  }, [dragIdx, overIdx, finishDrag]);

  const displaySections = useMemo(() => {
    if (dragIdx === null || overIdx === null || dragIdx === overIdx) return sections;
    const preview = [...sections];
    const [moved] = preview.splice(dragIdx, 1);
    preview.splice(overIdx, 0, moved);
    return preview;
  }, [sections, dragIdx, overIdx]);

  return (
    <div className="card p-4 mb-6 animate-slide-up">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('discover.settings')}</h3>
        <div className="flex items-center gap-2">
          <button onClick={onAddCustom} className="btn-ghost p-1.5 text-amber-500 hover:text-amber-600" title={t('discover.addSection')}>
            <Plus size={16} />
          </button>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={16} /></button>
        </div>
      </div>
      <div className="space-y-1" onTouchMove={handleTouchMove} onTouchEnd={handleTouchEnd}>
        {displaySections.map((s, idx) => {
          const isDragging = dragIdx !== null && s.id === sections[dragIdx]?.id;
          return (
            <div
              key={s.id}
              ref={el => { itemRefs.current[idx] = el; }}
              className={`flex items-center gap-2 py-2 px-2 rounded-lg transition-all select-none ${
                isDragging ? 'bg-amber-500/10 shadow-sm scale-[1.02]' : 'hover:bg-gray-50 dark:hover:bg-gray-800/50'
              }`}
            >
              <div
                className="p-1 cursor-grab active:cursor-grabbing text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 touch-none"
                onMouseDown={handleMouseDown(idx)}
                onTouchStart={handleTouchStart(idx)}
              >
                <GripVertical size={14} />
              </div>
              <button onClick={() => onToggle(s.id)} className={`p-1 rounded transition-colors ${s.visible ? 'text-amber-500' : 'text-gray-300 dark:text-gray-600'}`}>
                {s.visible ? <Eye size={14} /> : <EyeOff size={14} />}
              </button>
              <span className={`flex-1 text-sm ${s.visible ? 'text-gray-800 dark:text-gray-200' : 'text-gray-400 dark:text-gray-500'}`}>
                {s.id}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// --- Add custom section modal ---

function AddSectionModal({ existingIds, onAdd, onClose }: {
  existingIds: string[];
  onAdd: (label: string) => void;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [value, setValue] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || existingIds.includes(trimmed)) return;
    onAdd(trimmed);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />
      <div className="relative z-10 w-full max-w-sm card animate-slide-up p-6">
        <h3 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{t('discover.addSection')}</h3>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('discover.sectionLabel')}</label>
            <input className="input" value={value} onChange={e => setValue(e.target.value)} placeholder={t('discover.sectionPlaceholder')} autoFocus />
          </div>
          <div className="flex gap-3">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">{t('discover.cancel')}</button>
            <button type="submit" disabled={!value.trim()} className="btn-primary flex-1 disabled:opacity-50">{t('discover.add')}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// --- Section config hook (stable, no infinite loops) ---

function useSectionConfig(mode: string, availableIds: string[]) {
  // Stringify to get a stable dependency
  const idsKey = availableIds.join('|');

  const [config, setConfig] = useState<SectionConfig[]>(() =>
    mergeConfig(loadSectionConfig(mode), availableIds)
  );

  // Only re-merge when the set of available IDs actually changes
  useEffect(() => {
    setConfig(prev => {
      const merged = mergeConfig(prev, availableIds);
      // Avoid unnecessary state update
      if (JSON.stringify(merged) === JSON.stringify(prev)) return prev;
      return merged;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idsKey]);

  // Persist
  useEffect(() => {
    saveSectionConfig(mode, config);
  }, [mode, config]);

  const toggle = useCallback((id: string) => {
    setConfig(prev => prev.map(s => s.id === id ? { ...s, visible: !s.visible } : s));
  }, []);

  const reorder = useCallback((newOrder: SectionConfig[]) => {
    setConfig(newOrder);
  }, []);

  const addCustom = useCallback((label: string) => {
    setConfig(prev => [...prev, { id: label, visible: true }]);
  }, []);

  const visibleIds = useMemo(() =>
    config.filter(c => c.visible).map(c => c.id),
    [config]
  );

  return { config, visibleIds, toggle, reorder, addCustom };
}

// --- DiscoverBooks ---

function DiscoverBooks() {
  const { books } = useBooks();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, GoogleBookVolume[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReturnType<typeof extractBookData> | null>(null);

  const bookTitles = useMemo(() => new Set(books.map(b => normalizeStr(b.title))), [books]);
  const genres = useMemo(() => topGenres(books), [books]);
  const authors = useMemo(() => topAuthors(books), [books]);
  const trendingLabel = t('discover.trending');

  // Build stable list of section IDs (doesn't depend on fetched data)
  const availableIds = useMemo(() => {
    const ids = [trendingLabel, ...genres, ...authors.map(a => t('discover.moreBy', { name: a }))];
    const knownSet = new Set(ids);
    const saved = loadSectionConfig('books');
    for (const s of saved) {
      if (!knownSet.has(s.id)) ids.push(s.id);
    }
    return ids;
  }, [trendingLabel, genres, authors, t]);

  const { config, visibleIds, toggle, reorder, addCustom } = useSectionConfig('books', availableIds);

  // Fetch data — only depends on stable keys
  const configIds = useMemo(() => config.map(c => c.id).join('|'), [config]);
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      const result: Record<string, GoogleBookVolume[]> = {};

      let trending = getCache<GoogleBookVolume[]>('books_trending');
      if (!trending) { trending = await fetchBooksByGenre('bestseller', 12); setCache('books_trending', trending); }
      result[trendingLabel] = trending;

      for (const genre of genres) {
        let items = getCache<GoogleBookVolume[]>(`books_genre_${genre}`);
        if (!items) { items = await fetchBooksByGenre(genre, 8); setCache(`books_genre_${genre}`, items); }
        result[genre] = items;
      }
      for (const author of authors) {
        const label = t('discover.moreBy', { name: author });
        let items = getCache<GoogleBookVolume[]>(`books_author_${author}`);
        if (!items) { items = await fetchByAuthor(author, 8); setCache(`books_author_${author}`, items); }
        result[label] = items;
      }
      // Custom sections
      for (const id of configIds.split('|')) {
        if (id && !result[id]) {
          let items = getCache<GoogleBookVolume[]>(`books_custom_${id}`);
          if (!items) { items = await fetchBooksByGenre(id, 12); setCache(`books_custom_${id}`, items); }
          result[id] = items;
        }
      }
      if (!cancelled) { setData(result); setLoading(false); }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trendingLabel, genres.join(','), authors.join(','), configIds]);

  const handleAddCustom = useCallback((label: string) => {
    addCustom(label);
    (async () => {
      let items = getCache<GoogleBookVolume[]>(`books_custom_${label}`);
      if (!items) { items = await fetchBooksByGenre(label, 12); setCache(`books_custom_${label}`, items); }
      setData(prev => ({ ...prev, [label]: items! }));
    })();
  }, [addCustom]);

  return (
    <>
      {settingsOpen && <SettingsPanel sections={config} onToggle={toggle} onReorder={reorder} onClose={() => setSettingsOpen(false)} onAddCustom={() => setAddModalOpen(true)} />}
      {addModalOpen && <AddSectionModal existingIds={config.map(s => s.id)} onAdd={handleAddCustom} onClose={() => setAddModalOpen(false)} />}
      {prefill && <AddBookModal prefill={prefill} onClose={() => setPrefill(null)} />}
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {visibleIds.map(id => {
            const items = data[id];
            if (!items || items.length === 0) return null;
            const filtered = items.filter(v => !bookTitles.has(normalizeStr(v.volumeInfo.title)));
            if (filtered.length === 0) return null;
            const isTrending = id === trendingLabel;
            return (
              <div key={id}>
                <SectionTitle icon={isTrending ? <TrendingUp size={14} /> : <Sparkles size={14} />} label={id} />
                <PosterRow>{filtered.map(v => <BookCard key={v.id} volume={v} onClick={() => setPrefill(extractBookData(v))} />)}</PosterRow>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}

// --- DiscoverMovies ---

function DiscoverMovies() {
  const { movies } = useMovies();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, TmdbMovie[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReturnType<typeof extractMovieData> | null>(null);

  const movieTitles = useMemo(() => new Set(movies.map(m => normalizeStr(m.title))), [movies]);
  const genres = useMemo(() => topGenres(movies), [movies]);
  const directors = useMemo(() => topDirectors(movies), [movies]);
  const trendingLabel = t('discover.trending');

  const availableIds = useMemo(() => {
    const ids = [trendingLabel, ...genres, ...directors.map(d => t('discover.moreBy', { name: d }))];
    const knownSet = new Set(ids);
    const saved = loadSectionConfig('movies');
    for (const s of saved) { if (!knownSet.has(s.id)) ids.push(s.id); }
    return ids;
  }, [trendingLabel, genres, directors, t]);

  const { config, visibleIds, toggle, reorder, addCustom } = useSectionConfig('movies', availableIds);

  const configIds = useMemo(() => config.map(c => c.id).join('|'), [config]);
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      const result: Record<string, TmdbMovie[]> = {};

      let trending = getCache<TmdbMovie[]>('movies_trending');
      if (!trending) { trending = await fetchTrendingMovies(12); setCache('movies_trending', trending); }
      result[trendingLabel] = trending;

      for (const genre of genres) {
        let items = getCache<TmdbMovie[]>(`movies_genre_${genre}`);
        if (!items) { items = await fetchMoviesByGenre(genre, 12); setCache(`movies_genre_${genre}`, items); }
        result[genre] = items;
      }
      for (const director of directors) {
        const label = t('discover.moreBy', { name: director });
        let items = getCache<TmdbMovie[]>(`movies_director_${director}`);
        if (!items) { items = await fetchMoviesByGenre(director, 8); setCache(`movies_director_${director}`, items); }
        result[label] = items;
      }
      for (const id of configIds.split('|')) {
        if (id && !result[id]) {
          let items = getCache<TmdbMovie[]>(`movies_custom_${id}`);
          if (!items) { items = await fetchMoviesByGenre(id, 12); setCache(`movies_custom_${id}`, items); }
          result[id] = items;
        }
      }
      if (!cancelled) { setData(result); setLoading(false); }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trendingLabel, genres.join(','), directors.join(','), configIds]);

  const handleClickMovie = useCallback(async (tmdbMovie: TmdbMovie) => {
    const details = await fetchMovieDetails(tmdbMovie.id);
    setPrefill(extractMovieData(details ?? tmdbMovie));
  }, []);

  const handleAddCustom = useCallback((label: string) => {
    addCustom(label);
    (async () => {
      let items = getCache<TmdbMovie[]>(`movies_custom_${label}`);
      if (!items) { items = await fetchMoviesByGenre(label, 12); setCache(`movies_custom_${label}`, items); }
      setData(prev => ({ ...prev, [label]: items! }));
    })();
  }, [addCustom]);

  return (
    <>
      {settingsOpen && <SettingsPanel sections={config} onToggle={toggle} onReorder={reorder} onClose={() => setSettingsOpen(false)} onAddCustom={() => setAddModalOpen(true)} />}
      {addModalOpen && <AddSectionModal existingIds={config.map(s => s.id)} onAdd={handleAddCustom} onClose={() => setAddModalOpen(false)} />}
      {prefill && <AddMovieModal prefill={prefill} onClose={() => setPrefill(null)} />}
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {visibleIds.map(id => {
            const items = data[id];
            if (!items || items.length === 0) return null;
            const filtered = items.filter(m => !movieTitles.has(normalizeStr(m.title)));
            if (filtered.length === 0) return null;
            const isTrending = id === trendingLabel;
            return (
              <div key={id}>
                <SectionTitle icon={isTrending ? <TrendingUp size={14} /> : <Sparkles size={14} />} label={id} />
                <PosterRow>{filtered.map(m => <MovieCard key={m.id} movie={m} onClick={() => handleClickMovie(m)} />)}</PosterRow>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}

// --- DiscoverSeries ---

function DiscoverSeries() {
  const { series } = useSeries();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, TmdbSeries[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReturnType<typeof extractSeriesData> | null>(null);

  const seriesTitles = useMemo(() => new Set(series.map(s => normalizeStr(s.title))), [series]);
  const genres = useMemo(() => topGenres(series), [series]);
  const creators = useMemo(() => topCreators(series), [series]);
  const trendingLabel = t('discover.trending');

  const availableIds = useMemo(() => {
    const ids = [trendingLabel, ...genres, ...creators.map(c => t('discover.moreBy', { name: c }))];
    const knownSet = new Set(ids);
    const saved = loadSectionConfig('series');
    for (const s of saved) { if (!knownSet.has(s.id)) ids.push(s.id); }
    return ids;
  }, [trendingLabel, genres, creators, t]);

  const { config, visibleIds, toggle, reorder, addCustom } = useSectionConfig('series', availableIds);

  const configIds = useMemo(() => config.map(c => c.id).join('|'), [config]);
  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      const result: Record<string, TmdbSeries[]> = {};

      let trending = getCache<TmdbSeries[]>('series_trending');
      if (!trending) { trending = await fetchTrendingSeries(12); setCache('series_trending', trending); }
      result[trendingLabel] = trending;

      for (const genre of genres) {
        let items = getCache<TmdbSeries[]>(`series_genre_${genre}`);
        if (!items) { items = await fetchSeriesByGenre(genre, 12); setCache(`series_genre_${genre}`, items); }
        result[genre] = items;
      }
      for (const creator of creators) {
        const label = t('discover.moreBy', { name: creator });
        let items = getCache<TmdbSeries[]>(`series_creator_${creator}`);
        if (!items) { items = await fetchSeriesByGenre(creator, 8); setCache(`series_creator_${creator}`, items); }
        result[label] = items;
      }
      for (const id of configIds.split('|')) {
        if (id && !result[id]) {
          let items = getCache<TmdbSeries[]>(`series_custom_${id}`);
          if (!items) { items = await fetchSeriesByGenre(id, 12); setCache(`series_custom_${id}`, items); }
          result[id] = items;
        }
      }
      if (!cancelled) { setData(result); setLoading(false); }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trendingLabel, genres.join(','), creators.join(','), configIds]);

  const handleClickSeries = useCallback(async (tmdbSeries: TmdbSeries) => {
    const details = await fetchSeriesDetails(tmdbSeries.id);
    setPrefill(extractSeriesData(details ?? tmdbSeries));
  }, []);

  const handleAddCustom = useCallback((label: string) => {
    addCustom(label);
    (async () => {
      let items = getCache<TmdbSeries[]>(`series_custom_${label}`);
      if (!items) { items = await fetchSeriesByGenre(label, 12); setCache(`series_custom_${label}`, items); }
      setData(prev => ({ ...prev, [label]: items! }));
    })();
  }, [addCustom]);

  return (
    <>
      {settingsOpen && <SettingsPanel sections={config} onToggle={toggle} onReorder={reorder} onClose={() => setSettingsOpen(false)} onAddCustom={() => setAddModalOpen(true)} />}
      {addModalOpen && <AddSectionModal existingIds={config.map(s => s.id)} onAdd={handleAddCustom} onClose={() => setAddModalOpen(false)} />}
      {prefill && <AddSeriesModal prefill={prefill} onClose={() => setPrefill(null)} />}
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {visibleIds.map(id => {
            const items = data[id];
            if (!items || items.length === 0) return null;
            const filtered = items.filter(s => !seriesTitles.has(normalizeStr(s.name)));
            if (filtered.length === 0) return null;
            const isTrending = id === trendingLabel;
            return (
              <div key={id}>
                <SectionTitle icon={isTrending ? <TrendingUp size={14} /> : <Sparkles size={14} />} label={id} />
                <PosterRow>{filtered.map(s => <SeriesTmdbCard key={s.id} series={s} onClick={() => handleClickSeries(s)} />)}</PosterRow>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}

export default function Discover() {
  const { mode } = useMediaMode();
  const { t } = useTranslation();

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 pb-36 md:pb-8">
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
