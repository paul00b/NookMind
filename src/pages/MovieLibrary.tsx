import { useState, useMemo, useRef, useEffect } from 'react';
import { useMovies } from '../context/MoviesContext';
import { useMovieCategories } from '../context/MovieCategoriesContext';
import type { Movie, MovieStatus, MovieCategory } from '../types';
import MovieCard from '../components/MovieCard';
import MovieDetailModal from '../components/MovieDetailModal';
import CategoryMoviePickerModal from '../components/CategoryMoviePickerModal';
import StarRating from '../components/StarRating';
import { Film, ChevronDown, LayoutGrid, List, Plus, X, Check, Trash2, FolderOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import toast from 'react-hot-toast';

type SortKey = 'created_at' | 'title' | 'director' | 'rating' | 'watched_date';
type ViewMode = 'grid' | 'list';
type ActiveTab = MovieStatus | string;

function YearDivider({ year, count }: { year: string; count: number }) {
  const { t } = useTranslation();
  return (
    <div className="flex items-center gap-2.5 mb-4 mt-1">
      <span className="text-xs font-semibold text-gray-400 dark:text-gray-500 tracking-wide">{year}</span>
      <span className="text-xs text-gray-300 dark:text-gray-600">·</span>
      <span className="text-xs text-gray-400 dark:text-gray-500">{t('movieLibrary.yearCount', { count })}</span>
      <div className="flex-1 h-px bg-black/[0.06] dark:bg-white/[0.06]" />
    </div>
  );
}

function EmptyState({ isCategoryTab, onAddMovies }: { isCategoryTab?: boolean; onAddMovies?: () => void }) {
  const { t } = useTranslation();
  if (isCategoryTab) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center px-4">
        <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
          <FolderOpen size={36} className="text-amber-500" />
        </div>
        <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
          {t('movieLibrary.categoryEmpty')}
        </h3>
        <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs mb-5">
          {t('movieLibrary.categoryEmptyDesc')}
        </p>
        {onAddMovies && (
          <button onClick={onAddMovies} className="btn-primary text-sm flex items-center gap-2">
            <Plus size={15} /> {t('movieLibrary.addMovies')}
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center px-4">
      <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
        <Film size={36} className="text-amber-500" />
      </div>
      <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
        {t('movieLibrary.noMoviesWatched')}
      </h3>
    </div>
  );
}

function SelectDropdown({ value, onChange, options }: { value: string; onChange: (v: string) => void; options: { value: string; label: string }[] }) {
  return (
    <div className="relative flex-shrink-0">
      <select
        value={value}
        onChange={e => onChange(e.target.value)}
        className="appearance-none input py-2 pr-8 text-sm cursor-pointer"
      >
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
    </div>
  );
}

function MovieListRow({ movie, onClick, onRemove }: { movie: Movie; onClick: () => void; onRemove?: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="relative group">
      <button
        onClick={onClick}
        className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors text-left"
      >
        <div className="w-10 h-14 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
          {movie.poster_url ? (
            <img src={movie.poster_url} alt={movie.title} className="w-full h-full object-cover" loading="lazy" />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <Film size={16} className="text-gray-300 dark:text-gray-600" />
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-serif font-semibold text-sm text-gray-900 dark:text-gray-100 truncate group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
            {movie.title}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{movie.director}</p>
        </div>
        {movie.genre && (
          <span className="hidden sm:inline-flex flex-shrink-0 text-xs bg-amber-500/10 text-amber-700 dark:text-amber-400 px-2.5 py-1 rounded-full font-medium">
            {movie.genre}
          </span>
        )}
        {movie.status === 'watched' && movie.rating && (
          <div className="hidden sm:flex flex-shrink-0">
            <StarRating value={movie.rating} readonly size={12} />
          </div>
        )}
        <span className={`flex-shrink-0 text-xs font-medium px-2.5 py-1 rounded-full text-white ${
          movie.status === 'watched' ? 'bg-emerald-500' : 'bg-amber-500'
        }`}>
          {movie.status === 'watched' ? t('movieCard.watched') : t('movieCard.wantToWatch')}
        </span>
      </button>
      {onRemove && (
        <button
          onClick={e => { e.stopPropagation(); onRemove(); }}
          className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10"
          title={t('movieLibrary.removeFromCategory')}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}

export default function MovieLibrary() {
  const { t } = useTranslation();
  const { movies, loading } = useMovies();
  const { movieCategories, createMovieCategory, deleteMovieCategory, addMoviesToCategory, removeMovieFromCategory } = useMovieCategories();

  const [activeTab, setActiveTab] = useState<ActiveTab>('want_to_watch');
  const [genreFilter, setGenreFilter] = useState('');
  const [directorFilter, setDirectorFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('created_at');
  const [selectedMovie, setSelectedMovie] = useState<Movie | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    const stored = localStorage.getItem('movie-library-view-mode');
    return stored === 'list' ? 'list' : 'grid';
  });

  const handleSetViewMode = (mode: ViewMode) => {
    setViewMode(mode);
    localStorage.setItem('movie-library-view-mode', mode);
  };

  const [creatingCategory, setCreatingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const nameInputRef = useRef<HTMLInputElement>(null);

  const [pickerCategory, setPickerCategory] = useState<MovieCategory | null>(null);
  const [deletingCategoryId, setDeletingCategoryId] = useState<string | null>(null);

  useEffect(() => {
    if (creatingCategory) nameInputRef.current?.focus();
  }, [creatingCategory]);

  const activeCategory = useMemo(
    () => movieCategories.find(c => c.id === activeTab) ?? null,
    [movieCategories, activeTab]
  );

  const isStatusTab = activeTab === 'watched' || activeTab === 'want_to_watch';

  const filtered = useMemo(() => {
    if (!isStatusTab) return [];
    let list = movies.filter(m => m.status === (activeTab as MovieStatus));
    if (genreFilter) list = list.filter(m => m.genre === genreFilter);
    if (directorFilter) list = list.filter(m => m.director === directorFilter);
    return [...list].sort((a, b) => {
      if (sortKey === 'created_at') return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
      if (sortKey === 'title') return a.title.localeCompare(b.title);
      if (sortKey === 'director') return a.director.localeCompare(b.director);
      if (sortKey === 'rating') return (b.rating ?? 0) - (a.rating ?? 0);
      if (sortKey === 'watched_date') {
        if (!a.watched_date && !b.watched_date) return 0;
        if (!a.watched_date) return 1;
        if (!b.watched_date) return -1;
        return b.watched_date.localeCompare(a.watched_date);
      }
      return 0;
    });
  }, [movies, activeTab, isStatusTab, genreFilter, directorFilter, sortKey]);

  const categoryMovies = useMemo(() => {
    if (!activeCategory) return [];
    return movies.filter(m => activeCategory.movie_ids.includes(m.id));
  }, [movies, activeCategory]);

  const tabMovies = movies.filter(m => isStatusTab && m.status === (activeTab as MovieStatus));
  const genres = [...new Set(tabMovies.map(m => m.genre).filter(Boolean))] as string[];
  const directors = [...new Set(tabMovies.map(m => m.director).filter(Boolean))] as string[];

  const counts = {
    watched: movies.filter(m => m.status === 'watched').length,
    want_to_watch: movies.filter(m => m.status === 'want_to_watch').length,
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    const cat = await createMovieCategory(newCategoryName.trim());
    setNewCategoryName('');
    setCreatingCategory(false);
    if (cat) setActiveTab(cat.id);
  };

  const handleDeleteCategory = async (id: string) => {
    const cat = movieCategories.find(c => c.id === id);
    if (activeTab === id) setActiveTab('want_to_watch');
    await deleteMovieCategory(id);
    setDeletingCategoryId(null);
    if (cat) toast.success(`"${cat.title}" deleted`);
  };

  const handlePickerConfirm = async (newMovieIds: string[]) => {
    if (!pickerCategory) return;
    await addMoviesToCategory(pickerCategory.id, newMovieIds);
    setPickerCategory(null);
  };

  const tabClass = (isActive: boolean) =>
    `pb-3 px-1 text-sm font-medium border-b-2 transition-all -mb-px flex-shrink-0 ${
      isActive
        ? 'border-amber-500 text-amber-600 dark:text-amber-400'
        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
    }`;

  const countBadge = (count: number, isActive: boolean) => (
    <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
      isActive ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'
    }`}>
      {count}
    </span>
  );

  return (
    <div className="p-4 pb-32 md:p-8 md:pb-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-serif text-3xl font-bold text-indigo-600 dark:text-indigo-400 mb-1">{t('movieLibrary.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">{t('movieLibrary.moviesCount', { count: movies.length })}</p>
        </div>
        <div className="flex items-center bg-gray-100 dark:bg-gray-800 rounded-xl p-1 gap-0.5">
          <button
            onClick={() => handleSetViewMode('grid')}
            className={`p-2 rounded-lg transition-all ${
              viewMode === 'grid'
                ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
                : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
            }`}
          >
            <LayoutGrid size={16} />
          </button>
          <button
            onClick={() => handleSetViewMode('list')}
            className={`p-2 rounded-lg transition-all ${
              viewMode === 'list'
                ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
                : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
            }`}
          >
            <List size={16} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="-mx-4 mb-6 overflow-x-auto overflow-y-hidden border-b border-black/[0.06] px-4 pb-0 [touch-action:pan-x] dark:border-white/[0.06] md:mx-0 md:px-0" style={{ scrollbarWidth: 'none' }}>
        <div className="flex min-w-max gap-2">
          {([['want_to_watch', t('movieLibrary.wantToWatch')], ['watched', t('movieLibrary.watched')]] as const).map(([status, label]) => (
            <button
              key={status}
              onClick={() => { setActiveTab(status); setGenreFilter(''); setDirectorFilter(''); setSortKey(status === 'watched' ? 'watched_date' : 'created_at'); }}
              className={tabClass(activeTab === status)}
            >
              {label}{countBadge(counts[status], activeTab === status)}
            </button>
          ))}

          {movieCategories.map(cat => (
            <div key={cat.id} className="relative group flex-shrink-0 flex items-end">
              <button
                onClick={() => setActiveTab(cat.id)}
                className={tabClass(activeTab === cat.id) + ' pr-5'}
              >
                {cat.title}{countBadge(cat.movie_ids.length, activeTab === cat.id)}
              </button>
              <button
                onClick={() => setDeletingCategoryId(cat.id)}
                className="absolute right-0 bottom-3 opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-gray-400 hover:text-red-500"
              >
                <X size={12} />
              </button>
            </div>
          ))}

          {creatingCategory ? (
            <form onSubmit={handleCreateCategory} className="flex items-center gap-1 pb-3 flex-shrink-0">
              <input
                ref={nameInputRef}
                value={newCategoryName}
                onChange={e => setNewCategoryName(e.target.value)}
                placeholder={t('movieLibrary.newCategoryPlaceholder')}
                className="input py-1 text-sm w-36"
                onBlur={() => { if (!newCategoryName.trim()) setCreatingCategory(false); }}
              />
              <button type="submit" className="p-1 text-amber-600 hover:text-amber-700">
                <Check size={16} />
              </button>
              <button type="button" onClick={() => { setCreatingCategory(false); setNewCategoryName(''); }} className="p-1 text-gray-400 hover:text-gray-600">
                <X size={16} />
              </button>
            </form>
          ) : (
            <button
              onClick={() => setCreatingCategory(true)}
              className="pb-3 px-1 text-sm font-medium border-b-2 border-transparent text-gray-400 hover:text-amber-500 dark:hover:text-amber-400 -mb-px flex-shrink-0 flex items-center gap-1 transition-colors"
            >
              <Plus size={14} />{t('movieLibrary.newCategory')}
            </button>
          )}
        </div>
      </div>

      {/* Delete category confirm */}
      {deletingCategoryId && (() => {
        const cat = movieCategories.find(c => c.id === deletingCategoryId)!;
        return (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl flex items-center gap-3 text-sm">
            <Trash2 size={15} className="text-red-500 flex-shrink-0" />
            <span className="flex-1 text-red-700 dark:text-red-400">{t('movieLibrary.confirmDeleteCategory', { name: cat.title })}</span>
            <button onClick={() => handleDeleteCategory(deletingCategoryId)} className="font-medium text-red-600 hover:underline">{t('movieLibrary.yesDelete')}</button>
            <button onClick={() => setDeletingCategoryId(null)} className="text-gray-500 hover:underline">{t('movieLibrary.cancel')}</button>
          </div>
        );
      })()}

      {/* Category toolbar */}
      {activeCategory && (
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {activeCategory.movie_ids.length} movie{activeCategory.movie_ids.length !== 1 ? 's' : ''}
          </p>
          <button
            onClick={() => setPickerCategory(activeCategory)}
            className="btn-primary text-sm flex items-center gap-2 py-2"
          >
            <Plus size={15} /> {t('movieLibrary.addMovies')}
          </button>
        </div>
      )}

      {/* Filters */}
      {isStatusTab && tabMovies.length > 0 && (
        <div className="-mx-4 mb-6 overflow-x-auto overflow-y-hidden px-4 [touch-action:pan-x] md:mx-0 md:px-0" style={{ scrollbarWidth: 'none' }}>
          <div className="flex min-w-max items-center gap-2 pb-1">
            <SelectDropdown
              value={genreFilter}
              onChange={setGenreFilter}
              options={[{ value: '', label: t('movieLibrary.allGenres') }, ...genres.map(g => ({ value: g, label: g }))]}
            />
            <SelectDropdown
              value={directorFilter}
              onChange={setDirectorFilter}
              options={[{ value: '', label: t('movieLibrary.allDirectors') }, ...directors.map(d => ({ value: d, label: d }))]}
            />
            <SelectDropdown
              value={sortKey}
              onChange={v => setSortKey(v as SortKey)}
              options={[
                ...(activeTab === 'watched' ? [{ value: 'watched_date', label: t('movieLibrary.watchedDate') }] : []),
                { value: 'created_at', label: t('movieLibrary.dateAdded') },
                { value: 'title', label: t('movieLibrary.titleAZ') },
                { value: 'director', label: t('movieLibrary.directorAZ') },
                ...(activeTab === 'watched' ? [{ value: 'rating', label: t('movieLibrary.ratingDesc') }] : []),
              ]}
            />
          </div>
        </div>
      )}

      {/* Content */}
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
        categoryMovies.length === 0 ? (
          <EmptyState isCategoryTab onAddMovies={() => setPickerCategory(activeCategory)} />
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            {categoryMovies.map(movie => (
              <div key={movie.id} className="relative group/card">
                <MovieCard movie={movie} onClick={() => setSelectedMovie(movie)} />
                <button
                  onClick={() => removeMovieFromCategory(activeCategory.id, movie.id)}
                  className="absolute top-2 left-2 opacity-0 group-hover/card:opacity-100 transition-opacity p-1 rounded-full bg-black/50 text-white hover:bg-red-500"
                  title={t('movieLibrary.removeFromCategory')}
                >
                  <X size={11} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
            {categoryMovies.map(movie => (
              <MovieListRow
                key={movie.id}
                movie={movie}
                onClick={() => setSelectedMovie(movie)}
                onRemove={() => removeMovieFromCategory(activeCategory.id, movie.id)}
              />
            ))}
          </div>
        )
      ) : filtered.length === 0 ? (
        <EmptyState />
      ) : (activeTab === 'watched' && sortKey === 'watched_date') ? (
        <div className="space-y-8">
          {(() => {
            const groups: { year: string; movies: Movie[] }[] = [];
            for (const movie of filtered) {
              const year = movie.watched_date ? movie.watched_date.slice(0, 4) : t('movieLibrary.noDateGroup');
              const g = groups.find(g => g.year === year);
              if (g) g.movies.push(movie);
              else groups.push({ year, movies: [movie] });
            }
            return groups.map(({ year, movies: yearMovies }) => (
              <div key={year}>
                <YearDivider year={year} count={yearMovies.length} />
                {viewMode === 'grid' ? (
                  <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
                    {yearMovies.map(movie => (
                      <MovieCard key={movie.id} movie={movie} onClick={() => setSelectedMovie(movie)} />
                    ))}
                  </div>
                ) : (
                  <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
                    {yearMovies.map(movie => (
                      <MovieListRow key={movie.id} movie={movie} onClick={() => setSelectedMovie(movie)} />
                    ))}
                  </div>
                )}
              </div>
            ));
          })()}
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {filtered.map(movie => (
            <MovieCard key={movie.id} movie={movie} onClick={() => setSelectedMovie(movie)} />
          ))}
        </div>
      ) : (
        <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
          {filtered.map(movie => (
            <MovieListRow key={movie.id} movie={movie} onClick={() => setSelectedMovie(movie)} />
          ))}
        </div>
      )}

      {/* Movie detail modal */}
      {selectedMovie && (
        <MovieDetailModal
          movie={selectedMovie}
          onClose={() => setSelectedMovie(null)}
        />
      )}

      {/* Category picker */}
      {pickerCategory && (
        <CategoryMoviePickerModal
          category={pickerCategory}
          movies={movies}
          onConfirm={handlePickerConfirm}
          onClose={() => setPickerCategory(null)}
        />
      )}
    </div>
  );
}
