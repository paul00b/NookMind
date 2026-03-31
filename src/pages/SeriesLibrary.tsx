import { useState, useMemo, useRef, useEffect } from 'react';
import { useSeries } from '../context/SeriesContext';
import { useSeriesCategories } from '../context/SeriesCategoriesContext';
import type { Series, SeriesStatus, SeriesCategory } from '../types';
import SeriesCard from '../components/SeriesCard';
import SeriesDetailModal from '../components/SeriesDetailModal';
import CategorySeriesPickerModal from '../components/CategorySeriesPickerModal';
import StarRating from '../components/StarRating';
import { Tv, ChevronDown, LayoutGrid, List, Plus, X, Check, Trash2, FolderOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import toast from 'react-hot-toast';

type SortKey = 'created_at' | 'title' | 'creator' | 'rating';
type ViewMode = 'grid' | 'list';
type ActiveTab = SeriesStatus | string;

function EmptyState({ isCategoryTab, onAddSeries }: { isCategoryTab?: boolean; onAddSeries?: () => void }) {
  const { t } = useTranslation();
  if (isCategoryTab) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center px-4">
        <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
          <FolderOpen size={36} className="text-amber-500" />
        </div>
        <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">{t('seriesLibrary.categoryEmpty')}</h3>
        <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs mb-5">{t('seriesLibrary.categoryEmptyDesc')}</p>
        {onAddSeries && (
          <button onClick={onAddSeries} className="btn-primary text-sm flex items-center gap-2">
            <Plus size={15} /> {t('seriesLibrary.addSeries')}
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center px-4">
      <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
        <Tv size={36} className="text-amber-500" />
      </div>
      <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">{t('seriesLibrary.noSeriesWatched')}</h3>
    </div>
  );
}

function SelectDropdown({ value, onChange, options }: { value: string; onChange: (v: string) => void; options: { value: string; label: string }[] }) {
  return (
    <div className="relative flex-shrink-0">
      <select value={value} onChange={e => onChange(e.target.value)} className="appearance-none input py-2 pr-8 text-sm cursor-pointer">
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
    </div>
  );
}

function SeriesListRow({ series, onClick, onRemove }: { series: Series; onClick: () => void; onRemove?: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="relative group">
      <button onClick={onClick} className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors text-left">
        <div className="w-10 h-14 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
          {series.poster_url ? (
            <img src={series.poster_url} alt={series.title} className="w-full h-full object-cover" loading="lazy" />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Tv size={16} className="text-gray-300 dark:text-gray-600" />
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-serif font-semibold text-sm text-gray-900 dark:text-gray-100 truncate group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">{series.title}</p>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{series.creator}</p>
        </div>
        {series.genre && (
          <span className="hidden sm:inline-flex flex-shrink-0 text-xs bg-amber-500/10 text-amber-700 dark:text-amber-400 px-2.5 py-1 rounded-full font-medium">{series.genre}</span>
        )}
        {series.status === 'watched' && series.rating && (
          <div className="hidden sm:flex flex-shrink-0">
            <StarRating value={series.rating} readonly size={12} />
          </div>
        )}
        <span className={`flex-shrink-0 text-xs font-medium px-2.5 py-1 rounded-full text-white ${
          series.status === 'watched' ? 'bg-emerald-500' : series.status === 'watching' ? 'bg-blue-500' : 'bg-amber-500'
        }`}>
          {series.status === 'watched' ? t('seriesCard.watched') : series.status === 'watching' ? t('seriesCard.watching') : t('seriesCard.wantToWatch')}
        </span>
      </button>
      {onRemove && (
        <button
          onClick={e => { e.stopPropagation(); onRemove(); }}
          className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10"
          title={t('seriesLibrary.removeFromCategory')}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}

export default function SeriesLibrary() {
  const { t } = useTranslation();
  const { series, loading } = useSeries();
  const { seriesCategories, createSeriesCategory, deleteSeriesCategory, addSeriesToCategory, removeSeriesFromCategory } = useSeriesCategories();

  const [activeTab, setActiveTab] = useState<ActiveTab>('watching');
  const [genreFilter, setGenreFilter] = useState('');
  const [creatorFilter, setCreatorFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('created_at');
  const [selectedSeries, setSelectedSeries] = useState<Series | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [creatingCategory, setCreatingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const nameInputRef = useRef<HTMLInputElement>(null);
  const [pickerCategory, setPickerCategory] = useState<SeriesCategory | null>(null);
  const [deletingCategoryId, setDeletingCategoryId] = useState<string | null>(null);

  useEffect(() => { if (creatingCategory) nameInputRef.current?.focus(); }, [creatingCategory]);

  const activeCategory = useMemo(() => seriesCategories.find(c => c.id === activeTab) ?? null, [seriesCategories, activeTab]);
  const isStatusTab = activeTab === 'watched' || activeTab === 'want_to_watch' || activeTab === 'watching';

  const filtered = useMemo(() => {
    if (!isStatusTab) return [];
    let list = series.filter(s => s.status === (activeTab as SeriesStatus));
    if (genreFilter) list = list.filter(s => s.genre === genreFilter);
    if (creatorFilter) list = list.filter(s => s.creator === creatorFilter);
    return [...list].sort((a, b) => {
      if (sortKey === 'created_at') return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
      if (sortKey === 'title') return a.title.localeCompare(b.title);
      if (sortKey === 'creator') return a.creator.localeCompare(b.creator);
      if (sortKey === 'rating') return (b.rating ?? 0) - (a.rating ?? 0);
      return 0;
    });
  }, [series, activeTab, isStatusTab, genreFilter, creatorFilter, sortKey]);

  const categorySeries = useMemo(() => {
    if (!activeCategory) return [];
    return series.filter(s => activeCategory.series_ids.includes(s.id));
  }, [series, activeCategory]);

  const tabSeries = series.filter(s => isStatusTab && s.status === (activeTab as SeriesStatus));
  const genres = [...new Set(tabSeries.map(s => s.genre).filter(Boolean))] as string[];
  const creators = [...new Set(tabSeries.map(s => s.creator).filter(Boolean))] as string[];

  const counts = {
    watched: series.filter(s => s.status === 'watched').length,
    watching: series.filter(s => s.status === 'watching').length,
    want_to_watch: series.filter(s => s.status === 'want_to_watch').length,
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    const cat = await createSeriesCategory(newCategoryName.trim());
    setNewCategoryName('');
    setCreatingCategory(false);
    if (cat) setActiveTab(cat.id);
  };

  const handleDeleteCategory = async (id: string) => {
    const cat = seriesCategories.find(c => c.id === id);
    if (activeTab === id) setActiveTab('watching');
    await deleteSeriesCategory(id);
    setDeletingCategoryId(null);
    if (cat) toast.success(`"${cat.title}" deleted`);
  };

  const handlePickerConfirm = async (newSeriesIds: string[]) => {
    if (!pickerCategory) return;
    await addSeriesToCategory(pickerCategory.id, newSeriesIds);
    setPickerCategory(null);
  };

  const tabClass = (isActive: boolean) =>
    `pb-3 px-1 text-sm font-medium border-b-2 transition-all -mb-px flex-shrink-0 ${
      isActive ? 'border-amber-500 text-amber-600 dark:text-amber-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
    }`;

  const countBadge = (count: number, isActive: boolean) => (
    <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${isActive ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}>
      {count}
    </span>
  );

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-1">{t('seriesLibrary.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">{t('seriesLibrary.seriesCount', { count: series.length })}</p>
        </div>
        <div className="flex items-center bg-gray-100 dark:bg-gray-800 rounded-xl p-1 gap-0.5">
          <button onClick={() => setViewMode('grid')} className={`p-2 rounded-lg transition-all ${viewMode === 'grid' ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm' : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'}`}>
            <LayoutGrid size={16} />
          </button>
          <button onClick={() => setViewMode('list')} className={`p-2 rounded-lg transition-all ${viewMode === 'list' ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm' : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'}`}>
            <List size={16} />
          </button>
        </div>
      </div>

      <div className="flex gap-2 mb-6 border-b border-black/[0.06] dark:border-white/[0.06] overflow-x-auto pb-0" style={{ scrollbarWidth: 'none' }}>
        {([['watching', t('seriesLibrary.watching')], ['want_to_watch', t('seriesLibrary.wantToWatch')], ['watched', t('seriesLibrary.watched')]] as const).map(([status, label]) => (
          <button key={status} onClick={() => { setActiveTab(status); setGenreFilter(''); setCreatorFilter(''); }} className={tabClass(activeTab === status)}>
            {label}{countBadge(counts[status], activeTab === status)}
          </button>
        ))}
        {seriesCategories.map(cat => (
          <div key={cat.id} className="relative group flex-shrink-0 flex items-end">
            <button onClick={() => setActiveTab(cat.id)} className={tabClass(activeTab === cat.id) + ' pr-5'}>
              {cat.title}{countBadge(cat.series_ids.length, activeTab === cat.id)}
            </button>
            <button onClick={() => setDeletingCategoryId(cat.id)} className="absolute right-0 bottom-3 opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-gray-400 hover:text-red-500">
              <X size={12} />
            </button>
          </div>
        ))}
        {creatingCategory ? (
          <form onSubmit={handleCreateCategory} className="flex items-center gap-1 pb-3 flex-shrink-0">
            <input ref={nameInputRef} value={newCategoryName} onChange={e => setNewCategoryName(e.target.value)} placeholder={t('seriesLibrary.newCategoryPlaceholder')} className="input py-1 text-sm w-36" onBlur={() => { if (!newCategoryName.trim()) setCreatingCategory(false); }} />
            <button type="submit" className="p-1 text-amber-600 hover:text-amber-700"><Check size={16} /></button>
            <button type="button" onClick={() => { setCreatingCategory(false); setNewCategoryName(''); }} className="p-1 text-gray-400 hover:text-gray-600"><X size={16} /></button>
          </form>
        ) : (
          <button onClick={() => setCreatingCategory(true)} className="pb-3 px-1 text-sm font-medium border-b-2 border-transparent text-gray-400 hover:text-amber-500 dark:hover:text-amber-400 -mb-px flex-shrink-0 flex items-center gap-1 transition-colors">
            <Plus size={14} />{t('seriesLibrary.newCategory')}
          </button>
        )}
      </div>

      {deletingCategoryId && (() => {
        const cat = seriesCategories.find(c => c.id === deletingCategoryId)!;
        return (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl flex items-center gap-3 text-sm">
            <Trash2 size={15} className="text-red-500 flex-shrink-0" />
            <span className="flex-1 text-red-700 dark:text-red-400">{t('seriesLibrary.confirmDeleteCategory', { name: cat.title })}</span>
            <button onClick={() => handleDeleteCategory(deletingCategoryId)} className="font-medium text-red-600 hover:underline">{t('seriesLibrary.yesDelete')}</button>
            <button onClick={() => setDeletingCategoryId(null)} className="text-gray-500 hover:underline">{t('seriesLibrary.cancel')}</button>
          </div>
        );
      })()}

      {activeCategory && (
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">{activeCategory.series_ids.length} series</p>
          <button onClick={() => setPickerCategory(activeCategory)} className="btn-primary text-sm flex items-center gap-2 py-2">
            <Plus size={15} /> {t('seriesLibrary.addSeries')}
          </button>
        </div>
      )}

      {isStatusTab && tabSeries.length > 0 && (
        <div className="mb-6 flex items-center gap-2 overflow-x-auto pb-1" style={{ scrollbarWidth: 'none' }}>
          <SelectDropdown value={genreFilter} onChange={setGenreFilter} options={[{ value: '', label: t('seriesLibrary.allGenres') }, ...genres.map(g => ({ value: g, label: g }))]} />
          <SelectDropdown value={creatorFilter} onChange={setCreatorFilter} options={[{ value: '', label: t('seriesLibrary.allCreators') }, ...creators.map(c => ({ value: c, label: c }))]} />
          <SelectDropdown value={sortKey} onChange={v => setSortKey(v as SortKey)} options={[
            { value: 'created_at', label: t('seriesLibrary.dateAdded') },
            { value: 'title', label: t('seriesLibrary.titleAZ') },
            { value: 'creator', label: t('seriesLibrary.creatorAZ') },
            ...(activeTab === 'watched' ? [{ value: 'rating', label: t('seriesLibrary.ratingDesc') }] : []),
          ]} />
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="card overflow-hidden flex flex-col animate-pulse">
              <div className="w-full aspect-[2/3] bg-gray-200 dark:bg-gray-700 rounded-xl mb-3" />
              <div className="px-1 pb-1 space-y-2">
                <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-4/5" />
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-3/5" />
              </div>
            </div>
          ))}
        </div>
      ) : activeCategory ? (
        categorySeries.length === 0 ? (
          <EmptyState isCategoryTab onAddSeries={() => setPickerCategory(activeCategory)} />
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            {categorySeries.map(s => (
              <div key={s.id} className="relative group/card">
                <SeriesCard series={s} onClick={() => setSelectedSeries(s)} />
                <button onClick={() => removeSeriesFromCategory(activeCategory.id, s.id)} className="absolute top-2 left-2 opacity-0 group-hover/card:opacity-100 transition-opacity p-1 rounded-full bg-black/50 text-white hover:bg-red-500" title={t('seriesLibrary.removeFromCategory')}>
                  <X size={11} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
            {categorySeries.map(s => (
              <SeriesListRow key={s.id} series={s} onClick={() => setSelectedSeries(s)} onRemove={() => removeSeriesFromCategory(activeCategory.id, s.id)} />
            ))}
          </div>
        )
      ) : filtered.length === 0 ? (
        <EmptyState />
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {filtered.map(s => (
            <SeriesCard key={s.id} series={s} onClick={() => setSelectedSeries(s)} />
          ))}
        </div>
      ) : (
        <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
          {filtered.map(s => (
            <SeriesListRow key={s.id} series={s} onClick={() => setSelectedSeries(s)} />
          ))}
        </div>
      )}

      {selectedSeries && <SeriesDetailModal series={selectedSeries} onClose={() => setSelectedSeries(null)} />}
      {pickerCategory && <CategorySeriesPickerModal category={pickerCategory} series={series} onConfirm={handlePickerConfirm} onClose={() => setPickerCategory(null)} />}
    </div>
  );
}
