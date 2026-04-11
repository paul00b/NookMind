import { useEffect, useState } from 'react';
import type { Movie, TmdbMovie } from '../types';
import { useMovies } from '../context/MoviesContext';
import { useMovieCategories } from '../context/MovieCategoriesContext';
import StarRating from './StarRating';
import SheetModal, { SheetCloseButton } from './SheetModal';
import ExpandableDescription from './ExpandableDescription';
import EditableNote from './EditableNote';
import { fetchMovieDetails, getPosterUrl } from '../lib/tmdb';
import { X, Pencil, Check, Trash2, Film, ArrowLeftRight, FolderPlus, FolderMinus, ChevronDown } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

interface Props {
  movie: Movie;
  onClose: () => void;
}

export default function MovieDetailModal({ movie, onClose }: Props) {
  const { updateMovie, deleteMovie } = useMovies();
  const { movieCategories, addMoviesToCategory, removeMovieFromCategory } = useMovieCategories();
  const { t } = useTranslation();
  const [editingDate, setEditingDate] = useState(false);
  const [watchedDate, setWatchedDate] = useState(movie.watched_date || '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [localMovie, setLocalMovie] = useState<Movie>(movie);
  const [tmdbMovie, setTmdbMovie] = useState<TmdbMovie | null>(null);
  const [castOpen, setCastOpen] = useState(false);

  useEffect(() => {
    if (!movie.tmdb_id) return;
    let active = true;
    fetchMovieDetails(movie.tmdb_id).then(details => {
      if (active) setTmdbMovie(details);
    });
    return () => { active = false; };
  }, [movie.tmdb_id]);

  const cast = (tmdbMovie?.credits?.cast ?? []).slice(0, 12);

  const handleRatingChange = async (rating: number) => {
    setLocalMovie(m => ({ ...m, rating }));
    await updateMovie(movie.id, { rating });
    toast.success(t('movieDetail.ratingUpdated'));
  };

  const handleSaveNote = async (newNote: string) => {
    setLocalMovie(m => ({ ...m, personal_note: newNote }));
    await updateMovie(movie.id, { personal_note: newNote });
    toast.success(t('movieDetail.noteSaved'));
  };

  const handleSaveDate = async () => {
    const val = watchedDate || null;
    setLocalMovie(m => ({ ...m, watched_date: val }));
    await updateMovie(movie.id, { watched_date: val });
    setEditingDate(false);
    toast.success(t('movieDetail.dateSaved'));
  };

  const handleToggleStatus = async () => {
    const newStatus = localMovie.status === 'watched' ? 'want_to_watch' : 'watched';
    const updates: Partial<Movie> = { status: newStatus };
    if (newStatus === 'want_to_watch') { updates.rating = null; updates.watched_date = null; }
    if (newStatus === 'watched' && !localMovie.watched_date) updates.watched_date = new Date().toISOString().slice(0, 10);
    setLocalMovie(m => ({ ...m, ...updates }));
    await updateMovie(movie.id, updates);
    toast.success(newStatus === 'watched' ? t('movieDetail.movedToWatched') : t('movieDetail.movedToWantToWatch'));
  };

  const handleDelete = async () => {
    await deleteMovie(movie.id);
    onClose();
  };

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh]"
      scrollable
    >
        {/* Close */}
        <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </SheetCloseButton>

        <div className="p-6 space-y-6">
          <div className="flex flex-col md:flex-row gap-6">
            <div className="flex-shrink-0 mx-auto md:mx-0">
              <div className="w-32 md:w-40 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
                {localMovie.poster_url ? (
                  <img src={localMovie.poster_url} alt={localMovie.title} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <Film size={40} className="text-gray-400" />
                  </div>
                )}
              </div>
            </div>

            <div className="flex-1 min-w-0 space-y-4">
              <div>
                <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight mb-1">
                  {localMovie.title}
                </h2>
                <p className="text-gray-600 dark:text-gray-400 font-medium">{localMovie.director}</p>
              </div>

              <div className="flex flex-wrap gap-2 text-sm">
                {localMovie.genre && (
                  <span className="bg-amber-500/10 text-amber-700 dark:text-amber-400 px-3 py-1 rounded-full font-medium">
                    {localMovie.genre}
                  </span>
                )}
                <span className={`px-3 py-1 rounded-full font-medium text-white ${
                  localMovie.status === 'watched' ? 'bg-emerald-500' : 'bg-amber-500'
                }`}>
                  {localMovie.status === 'watched' ? t('movieDetail.watched') : t('movieDetail.wantToWatch')}
                </span>
                {localMovie.release_date && (
                  <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                    {localMovie.release_date.slice(0, 4)}
                  </span>
                )}
                {localMovie.runtime && (
                  <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                    {t('movieDetail.runtime', { count: localMovie.runtime })}
                  </span>
                )}
              </div>

              {localMovie.description && (
                <ExpandableDescription
                  description={localMovie.description}
                  label={t('movieDetail.description')}
                  seeMoreText={t('movieDetail.seeMore')}
                  seeLessText={t('movieDetail.seeLess')}
                />
              )}
            </div>
          </div>

          {localMovie.status === 'watched' && (
            <div className="space-y-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('movieDetail.yourRating')}</p>
                <StarRating value={localMovie.rating} onChange={handleRatingChange} size={26} />
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-sm text-gray-500 dark:text-gray-400">{t('movieDetail.watchedOnLabel')}</p>
                  {!editingDate && (
                    <button onClick={() => setEditingDate(true)} className="text-gray-400 hover:text-amber-500 transition-colors">
                      <Pencil size={13} />
                    </button>
                  )}
                </div>
                {editingDate ? (
                  <div className="flex items-center gap-2">
                    <input
                      type="date"
                      className="input py-1.5 text-sm"
                      value={watchedDate}
                      max={new Date().toISOString().slice(0, 10)}
                      onChange={e => setWatchedDate(e.target.value)}
                      autoFocus
                    />
                    <button onClick={handleSaveDate} className="btn-primary text-sm py-1.5 px-3 flex items-center gap-1">
                      <Check size={14} />
                    </button>
                    <button onClick={() => { setWatchedDate(localMovie.watched_date || ''); setEditingDate(false); }} className="btn-ghost text-sm py-1.5 px-3">
                      <X size={14} />
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-gray-700 dark:text-gray-300">
                    {localMovie.watched_date
                      ? new Date(localMovie.watched_date + 'T00:00:00').toLocaleDateString()
                      : <span className="text-gray-400 italic">{t('movieDetail.noDate')}</span>
                    }
                  </p>
                )}
              </div>
            </div>
          )}

          {cast.length > 0 && (
              <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden">
                <button
                  type="button"
                  onClick={() => setCastOpen(open => !open)}
                  className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
                >
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {t('movieDetail.cast')} ({cast.length})
                  </span>
                  <ChevronDown
                    size={16}
                    className={`text-gray-400 transition-transform duration-300 ${castOpen ? 'rotate-180' : ''}`}
                  />
                </button>
                <div className={`overflow-hidden transition-[max-height] duration-300 ease-in-out ${castOpen ? 'max-h-64' : 'max-h-0'}`}>
                  <div className="border-t border-black/[0.06] dark:border-white/[0.06] py-4">
                    <div className="flex gap-3 overflow-x-auto px-4 pb-1" style={{ scrollbarWidth: 'none' }}>
                      {cast.map(person => {
                        const photoUrl = getPosterUrl(person.profile_path ?? null);
                        return (
                          <div key={person.id} className="w-24 flex-shrink-0">
                            <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
                              {photoUrl ? (
                                <img src={photoUrl} alt={person.name} className="w-full h-full object-cover" loading="lazy" />
                              ) : (
                                <div className="w-full h-full flex items-center justify-center text-lg font-semibold text-gray-400 dark:text-gray-500">
                                  {person.name.charAt(0)}
                                </div>
                              )}
                            </div>
                            <p className="mt-2 text-xs font-semibold text-gray-800 dark:text-gray-200 leading-tight line-clamp-2">{person.name}</p>
                            {person.character && (
                              <p className="mt-0.5 text-[11px] text-gray-500 dark:text-gray-400 leading-tight line-clamp-2">{person.character}</p>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
          )}

          <EditableNote
            note={localMovie.personal_note}
            labelText={t('movieDetail.personalNote')}
            placeholderText={t('movieDetail.notePlaceholder')}
            saveText={t('movieDetail.save')}
            cancelText={t('movieDetail.cancel')}
            noNotesText={t('movieDetail.noNotes')}
            onSave={handleSaveNote}
          />

          {movieCategories.length > 0 && (
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">{t('movieDetail.collections')}</p>
              <div className="flex flex-wrap gap-2">
                {movieCategories.map(cat => {
                  const isIn = cat.movie_ids.includes(localMovie.id);
                  return (
                    <button
                      key={cat.id}
                      onClick={() => isIn
                        ? removeMovieFromCategory(cat.id, localMovie.id)
                        : addMoviesToCategory(cat.id, [localMovie.id])
                      }
                      className={`inline-flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border transition-colors ${
                        isIn
                          ? 'bg-amber-500/15 border-amber-400/50 text-amber-700 dark:text-amber-400 hover:bg-red-500/10 hover:border-red-400/50 hover:text-red-600 dark:hover:text-red-400'
                          : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-amber-500/10 hover:border-amber-400/50 hover:text-amber-700 dark:hover:text-amber-400'
                      }`}
                    >
                      {isIn ? <FolderMinus size={12} /> : <FolderPlus size={12} />}
                      {cat.title}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="flex flex-wrap gap-2 pt-2">
            <button onClick={handleToggleStatus} className="btn-ghost text-sm flex items-center gap-1.5">
              <ArrowLeftRight size={14} />
              {localMovie.status === 'watched' ? t('movieDetail.moveToWantToWatch') : t('movieDetail.moveToWatched')}
            </button>
            {!confirmDelete ? (
              <button onClick={() => setConfirmDelete(true)} className="btn-ghost text-sm text-red-500 hover:bg-red-500/10 flex items-center gap-1.5">
                <Trash2 size={14} /> {t('movieDetail.delete')}
              </button>
            ) : (
              <div className="flex items-center gap-2 text-sm">
                <span className="text-gray-600 dark:text-gray-400">{t('movieDetail.areYouSure')}</span>
                <button onClick={handleDelete} className="text-red-500 font-medium hover:underline">{t('movieDetail.yesDelete')}</button>
                <button onClick={() => setConfirmDelete(false)} className="text-gray-500 hover:underline">{t('movieDetail.cancel')}</button>
              </div>
            )}
          </div>
        </div>
    </SheetModal>
  );
}
