import { useState, useMemo } from 'react';
import { X, Search, Check, Film } from 'lucide-react';
import type { Movie, MovieCategory } from '../types';
import { useTranslation } from 'react-i18next';
import SheetModal from './SheetModal';

interface Props {
  category: MovieCategory;
  movies: Movie[];
  onConfirm: (movieIds: string[]) => void;
  onClose: () => void;
}

export default function CategoryMoviePickerModal({ category, movies, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set(category.movie_ids));

  const filtered = useMemo(() => {
    const q = query.toLowerCase();
    return movies.filter(m =>
      !q || m.title.toLowerCase().includes(q) || m.director.toLowerCase().includes(q)
    );
  }, [movies, query]);

  const toggle = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleConfirm = () => {
    const newIds = [...selected].filter(id => !category.movie_ids.includes(id));
    onConfirm(newIds);
  };

  const newCount = [...selected].filter(id => !category.movie_ids.includes(id)).length;

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[85vh] flex flex-col"
    >
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-black/[0.06] dark:border-white/[0.06] flex-shrink-0">
          <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
            {t('movieLibrary.addMoviesTo', { name: category.title })}
          </h2>
          <button onClick={onClose} className="btn-ghost p-2">
            <X size={18} />
          </button>
        </div>

        {/* Search */}
        <div className="px-5 py-3 flex-shrink-0">
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder={t('movieLibrary.searchMovies')}
              className="input pl-9 text-sm py-2"
              autoFocus
            />
          </div>
        </div>

        {/* Movie list */}
        <div className="flex-1 overflow-y-auto px-3 pb-3">
          {filtered.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-10">{t('movieLibrary.noMoviesFound')}</p>
          ) : (
            filtered.map(movie => {
              const isSelected = selected.has(movie.id);
              const alreadyIn = category.movie_ids.includes(movie.id);
              return (
                <button
                  key={movie.id}
                  onClick={() => toggle(movie.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors text-left ${
                    isSelected
                      ? 'bg-amber-500/10 dark:bg-amber-500/15'
                      : 'hover:bg-gray-50 dark:hover:bg-white/5'
                  }`}
                >
                  {/* Poster */}
                  <div className="w-9 h-12 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
                    {movie.poster_url ? (
                      <img src={movie.poster_url} alt={movie.title} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <Film size={14} className="text-gray-400" />
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{movie.title}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{movie.director}</p>
                  </div>

                  {/* Already-in badge or checkbox */}
                  {alreadyIn ? (
                    <span className="flex-shrink-0 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                      {t('movieLibrary.alreadyInCategory')}
                    </span>
                  ) : (
                    <div className={`w-5 h-5 rounded-md border-2 flex-shrink-0 flex items-center justify-center transition-colors ${
                      isSelected
                        ? 'bg-amber-500 border-amber-500'
                        : 'border-gray-300 dark:border-gray-600'
                    }`}>
                      {isSelected && <Check size={12} className="text-white" strokeWidth={3} />}
                    </div>
                  )}
                </button>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="p-5 border-t border-black/[0.06] dark:border-white/[0.06] flex gap-3 flex-shrink-0">
          <button onClick={onClose} className="btn-ghost flex-1 text-sm">
            {t('movieLibrary.cancel')}
          </button>
          <button
            onClick={handleConfirm}
            disabled={newCount === 0}
            className="btn-primary flex-1 text-sm disabled:opacity-40"
          >
            {newCount > 0
              ? t('movieLibrary.addNMovies', { count: newCount })
              : t('movieLibrary.confirmAdd')}
          </button>
        </div>
    </SheetModal>
  );
}
