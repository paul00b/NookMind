import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Compass, Tv, BookOpen, Film, Sparkles, TrendingUp, Settings2, Eye, EyeOff, X, Plus, GripVertical } from 'lucide-react';
import { useMediaMode } from '../context/MediaModeContext';
import { useBooks } from '../context/BooksContext';
import { useMovies } from '../context/MoviesContext';
import { useSeries } from '../context/SeriesContext';
import { useTranslation } from 'react-i18next';
import { fetchTrendingMovies, fetchTrendingSeries, fetchMoviesByGenre, fetchSeriesByGenre, getPosterUrl } from '../lib/tmdb';
import { fetchByGenre as fetchBooksByGenre, fetchByAuthor } from '../lib/googleBooks';
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

// Merge saved config with available sections (new sections get appended as visible)
function mergeConfig(saved: SectionConfig[], available: string[]): SectionConfig[] {
  const map = new Map(saved.map(s => [s.id, s]));
  const ordered: SectionConfig[] = [];
  // Keep saved order for known sections
  for (const s of saved) {
    if (available.includes(s.id)) ordered.push(s);
  }
  // Append new sections not in saved config
  for (const id of available) {
    if (!map.has(id)) ordered.push({ id, visible: true });
  }
  return ordered;
}

// --- Collection analysis helpers ---

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

function MovieCard({ movie }: { movie: TmdbMovie }) {
  const poster = getPosterUrl(movie.poster_path);
  return (
    <div className="flex-shrink-0 snap-start w-28 md:w-32">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {poster ? <img src={poster} alt={movie.title} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><Film size={22} className="text-gray-300 dark:text-gray-600" /></div>}
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight">{movie.title}</p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400">{movie.release_date?.slice(0, 4) || '—'}</p>
    </div>
  );
}

function SeriesTmdbCard({ series }: { series: TmdbSeries }) {
  const poster = getPosterUrl(series.poster_path);
  return (
    <div className="flex-shrink-0 snap-start w-28 md:w-32">
      <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {poster ? <img src={poster} alt={series.name} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><Tv size={22} className="text-gray-300 dark:text-gray-600" /></div>}
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
        {cover ? <img src={cover} alt={info.title} className="w-full h-full object-cover" loading="lazy" />
          : <div className="w-full h-full flex items-center justify-center"><BookOpen size={22} className="text-gray-300 dark:text-gray-600" /></div>}
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

// --- Settings panel ---

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
  const listRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLDivElement | null)[]>([]);

  // Touch drag state
  const touchState = useRef<{ startY: number; idx: number } | null>(null);

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

  // Mouse drag
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

  // Touch drag
  const handleTouchStart = useCallback((idx: number) => (e: React.TouchEvent) => {
    const touch = e.touches[0];
    touchState.current = { startY: touch.clientY, idx };
    setDragIdx(idx);
    setOverIdx(idx);
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (touchState.current === null) return;
    e.preventDefault();
    const touch = e.touches[0];
    const target = getItemIndexAtY(touch.clientY);
    if (target !== null) setOverIdx(target);
  }, [getItemIndexAtY]);

  const handleTouchEnd = useCallback(() => {
    if (touchState.current === null || dragIdx === null) return;
    finishDrag(dragIdx, overIdx);
  }, [dragIdx, overIdx, finishDrag]);

  // Compute display order with drag preview
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
      <div ref={listRef} className="space-y-1" onTouchMove={handleTouchMove} onTouchEnd={handleTouchEnd}>
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
            <input
              className="input"
              value={value}
              onChange={e => setValue(e.target.value)}
              placeholder={t('discover.sectionPlaceholder')}
              autoFocus
            />
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

// --- Generic section-based discover ---

interface DiscoverSection {
  id: string;
  icon: React.ReactNode;
  render: () => React.ReactNode;
}

function useDiscoverSections(mode: string, buildSections: () => DiscoverSection[]) {
  const sections = useMemo(buildSections, [buildSections]);
  const availableIds = useMemo(() => sections.map(s => s.id), [sections]);

  const [config, setConfig] = useState<SectionConfig[]>(() =>
    mergeConfig(loadSectionConfig(mode), availableIds)
  );

  // Re-merge when available sections change (e.g. user adds books to a new genre)
  useEffect(() => {
    setConfig(prev => mergeConfig(prev, availableIds));
  }, [availableIds]);

  // Persist on change
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

  const orderedVisible = useMemo(() => {
    return config
      .filter(c => c.visible)
      .map(c => sections.find(s => s.id === c.id))
      .filter(Boolean) as DiscoverSection[];
  }, [config, sections]);

  return { config, orderedVisible, toggle, reorder, addCustom, sections };
}

// --- Mode-specific discover ---

function DiscoverBooks() {
  const { books } = useBooks();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, GoogleBookVolume[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);

  const genres = useMemo(() => topGenres(books), [books]);
  const authors = useMemo(() => topAuthors(books), [books]);

  const buildSections = useCallback((): DiscoverSection[] => {
    const sections: DiscoverSection[] = [
      { id: t('discover.trending'), icon: <TrendingUp size={14} />, render: () => {
        const items = data[t('discover.trending')];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
      }},
    ];
    for (const genre of genres) {
      sections.push({ id: genre, icon: <Sparkles size={14} />, render: () => {
        const items = data[genre];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow> : null;
      }});
    }
    for (const author of authors) {
      const label = t('discover.moreBy', { name: author });
      sections.push({ id: label, icon: <Sparkles size={14} />, render: () => {
        const items = data[label];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow> : null;
      }});
    }
    // Custom sections from config that aren't in the default list
    const knownIds = new Set([t('discover.trending'), ...genres, ...authors.map(a => t('discover.moreBy', { name: a }))]);
    const savedConfig = loadSectionConfig('books');
    for (const s of savedConfig) {
      if (!knownIds.has(s.id)) {
        sections.push({ id: s.id, icon: <Sparkles size={14} />, render: () => {
          const items = data[s.id];
          if (!items) return <SkeletonRow />;
          return items.length > 0 ? <PosterRow>{items.map(v => <BookCard key={v.id} volume={v} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
        }});
      }
    }
    return sections;
  }, [genres, authors, data, t]);

  const { config, orderedVisible, toggle, reorder, addCustom } = useDiscoverSections('books', buildSections);

  const load = useCallback(async () => {
    setLoading(true);
    const result: Record<string, GoogleBookVolume[]> = {};

    const trendingKey = t('discover.trending');
    let trending = getCache<GoogleBookVolume[]>('books_trending');
    if (!trending) { trending = await fetchBooksByGenre('bestseller', 12); setCache('books_trending', trending); }
    result[trendingKey] = trending;

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

    // Load custom sections
    for (const s of config) {
      if (!result[s.id]) {
        let items = getCache<GoogleBookVolume[]>(`books_custom_${s.id}`);
        if (!items) { items = await fetchBooksByGenre(s.id, 12); setCache(`books_custom_${s.id}`, items); }
        result[s.id] = items;
      }
    }

    setData(result);
    setLoading(false);
  }, [genres, authors, config, t]);

  useEffect(() => { load(); }, [load]);

  const handleAddCustom = useCallback((label: string) => {
    addCustom(label);
    // Trigger fetch for the new section
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
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading && orderedVisible.length === 0 ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {orderedVisible.map(s => {
            const content = s.render();
            if (!content) return null;
            return (
              <div key={s.id}>
                <SectionTitle icon={s.icon} label={s.id} />
                {content}
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}

function DiscoverMovies() {
  const { movies } = useMovies();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, TmdbMovie[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);

  const genres = useMemo(() => topGenres(movies), [movies]);
  const directors = useMemo(() => topDirectors(movies), [movies]);

  const buildSections = useCallback((): DiscoverSection[] => {
    const sections: DiscoverSection[] = [
      { id: t('discover.trending'), icon: <TrendingUp size={14} />, render: () => {
        const items = data[t('discover.trending')];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
      }},
    ];
    for (const genre of genres) {
      sections.push({ id: genre, icon: <Sparkles size={14} />, render: () => {
        const items = data[genre];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow> : null;
      }});
    }
    for (const director of directors) {
      const label = t('discover.moreBy', { name: director });
      sections.push({ id: label, icon: <Sparkles size={14} />, render: () => {
        const items = data[label];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow> : null;
      }});
    }
    const knownIds = new Set([t('discover.trending'), ...genres, ...directors.map(d => t('discover.moreBy', { name: d }))]);
    const savedConfig = loadSectionConfig('movies');
    for (const s of savedConfig) {
      if (!knownIds.has(s.id)) {
        sections.push({ id: s.id, icon: <Sparkles size={14} />, render: () => {
          const items = data[s.id];
          if (!items) return <SkeletonRow />;
          return items.length > 0 ? <PosterRow>{items.map(m => <MovieCard key={m.id} movie={m} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
        }});
      }
    }
    return sections;
  }, [genres, directors, data, t]);

  const { config, orderedVisible, toggle, reorder, addCustom } = useDiscoverSections('movies', buildSections);

  const load = useCallback(async () => {
    setLoading(true);
    const result: Record<string, TmdbMovie[]> = {};

    const trendingKey = t('discover.trending');
    let trending = getCache<TmdbMovie[]>('movies_trending');
    if (!trending) { trending = await fetchTrendingMovies(12); setCache('movies_trending', trending); }
    result[trendingKey] = trending;

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

    for (const s of config) {
      if (!result[s.id]) {
        let items = getCache<TmdbMovie[]>(`movies_custom_${s.id}`);
        if (!items) { items = await fetchMoviesByGenre(s.id, 12); setCache(`movies_custom_${s.id}`, items); }
        result[s.id] = items;
      }
    }

    setData(result);
    setLoading(false);
  }, [genres, directors, config, t]);

  useEffect(() => { load(); }, [load]);

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
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading && orderedVisible.length === 0 ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {orderedVisible.map(s => {
            const content = s.render();
            if (!content) return null;
            return <div key={s.id}><SectionTitle icon={s.icon} label={s.id} />{content}</div>;
          })}
        </div>
      )}
    </>
  );
}

function DiscoverSeries() {
  const { series } = useSeries();
  const { t } = useTranslation();
  const [data, setData] = useState<Record<string, TmdbSeries[]>>({});
  const [loading, setLoading] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);

  const genres = useMemo(() => topGenres(series), [series]);
  const creators = useMemo(() => topCreators(series), [series]);

  const buildSections = useCallback((): DiscoverSection[] => {
    const sections: DiscoverSection[] = [
      { id: t('discover.trending'), icon: <TrendingUp size={14} />, render: () => {
        const items = data[t('discover.trending')];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(s => <SeriesTmdbCard key={s.id} series={s} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
      }},
    ];
    for (const genre of genres) {
      sections.push({ id: genre, icon: <Sparkles size={14} />, render: () => {
        const items = data[genre];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(s => <SeriesTmdbCard key={s.id} series={s} />)}</PosterRow> : null;
      }});
    }
    for (const creator of creators) {
      const label = t('discover.moreBy', { name: creator });
      sections.push({ id: label, icon: <Sparkles size={14} />, render: () => {
        const items = data[label];
        if (!items) return <SkeletonRow />;
        return items.length > 0 ? <PosterRow>{items.map(s => <SeriesTmdbCard key={s.id} series={s} />)}</PosterRow> : null;
      }});
    }
    const knownIds = new Set([t('discover.trending'), ...genres, ...creators.map(c => t('discover.moreBy', { name: c }))]);
    const savedConfig = loadSectionConfig('series');
    for (const s of savedConfig) {
      if (!knownIds.has(s.id)) {
        sections.push({ id: s.id, icon: <Sparkles size={14} />, render: () => {
          const items = data[s.id];
          if (!items) return <SkeletonRow />;
          return items.length > 0 ? <PosterRow>{items.map(s2 => <SeriesTmdbCard key={s2.id} series={s2} />)}</PosterRow> : <p className="text-sm text-gray-400">{t('discover.noResults')}</p>;
        }});
      }
    }
    return sections;
  }, [genres, creators, data, t]);

  const { config, orderedVisible, toggle, reorder, addCustom } = useDiscoverSections('series', buildSections);

  const load = useCallback(async () => {
    setLoading(true);
    const result: Record<string, TmdbSeries[]> = {};

    const trendingKey = t('discover.trending');
    let trending = getCache<TmdbSeries[]>('series_trending');
    if (!trending) { trending = await fetchTrendingSeries(12); setCache('series_trending', trending); }
    result[trendingKey] = trending;

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

    for (const s of config) {
      if (!result[s.id]) {
        let items = getCache<TmdbSeries[]>(`series_custom_${s.id}`);
        if (!items) { items = await fetchSeriesByGenre(s.id, 12); setCache(`series_custom_${s.id}`, items); }
        result[s.id] = items;
      }
    }

    setData(result);
    setLoading(false);
  }, [genres, creators, config, t]);

  useEffect(() => { load(); }, [load]);

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
      <div className="flex justify-end mb-4">
        <button onClick={() => setSettingsOpen(o => !o)} className={`btn-ghost p-2 transition-colors ${settingsOpen ? 'text-amber-500' : ''}`}>
          <Settings2 size={18} />
        </button>
      </div>
      {loading && orderedVisible.length === 0 ? (
        <div className="space-y-8"><SkeletonRow /><SkeletonRow /></div>
      ) : (
        <div className="space-y-8">
          {orderedVisible.map(s => {
            const content = s.render();
            if (!content) return null;
            return <div key={s.id}><SectionTitle icon={s.icon} label={s.id} />{content}</div>;
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
