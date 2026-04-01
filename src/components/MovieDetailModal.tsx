import { useState, useRef, useEffect } from 'react';
import type { Movie } from '../types';
import { useMovies } from '../context/MoviesContext';
import { useMovieCategories } from '../context/MovieCategoriesContext';
import StarRating from './StarRating';
import SheetModal from './SheetModal';
import { X, Pencil, Check, Trash2, Film, ArrowLeftRight, ChevronDown, ChevronUp, FolderPlus, FolderMinus } from 'lucide-react';
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
  const [editingNote, setEditingNote] = useState(false);
  const [note, setNote] = useState(movie.personal_note || '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [localMovie, setLocalMovie] = useState<Movie>(movie);
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);
  useEffect(() => {
    if (descRef.current) setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [localMovie.description]);

  const handleRatingChange = async (rating: number) => {
    setLocalMovie(m => ({ ...m, rating }));
    await updateMovie(movie.id, { rating });
    toast.success(t('movieDetail.ratingUpdated'));
  };

  const handleSaveNote = async () => {
    setLocalMovie(m => ({ ...m, personal_note: note }));
    await updateMovie(movie.id, { personal_note: note });
    setEditingNote(false);
    toast.success(t('movieDetail.noteSaved'));
  };

  const handleToggleStatus = async () => {
    const newStatus = localMovie.status === 'watched' ? 'want_to_watch' : 'watched';
    const updates: Partial<Movie> = { status: newStatus };
    if (newStatus === 'want_to_watch') updates.rating = null;
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
      panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none md:max-h-[90vh] overflow-y-auto"
    >
        {/* Close */}
        <button onClick={onClose} className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </button>

        <div className="flex flex-col md:flex-row gap-6 p-6">
          {/* Poster */}
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

          {/* Details */}
          <div className="flex-1 min-w-0 space-y-4">
            {/* Title & director */}
            <div>
              <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight mb-1">
                {localMovie.title}
              </h2>
              <p className="text-gray-600 dark:text-gray-400 font-medium">{localMovie.director}</p>
            </div>

            {/* Meta */}
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

            {/* Rating */}
            {localMovie.status === 'watched' && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('movieDetail.yourRating')}</p>
                <StarRating value={localMovie.rating} onChange={handleRatingChange} size={22} />
              </div>
            )}

            {/* Description */}
            {localMovie.description && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('movieDetail.description')}</p>
                <button onClick={() => (descTruncated || descExpanded) && setDescExpanded(e => !e)} className="w-full text-left group">
                  <p ref={descRef} className={`text-sm text-gray-700 dark:text-gray-300 leading-relaxed ${descExpanded ? '' : 'line-clamp-4'}`}>
                    {localMovie.description}
                  </p>
                  {(descTruncated || descExpanded) && (
                    <span className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 mt-1 group-hover:underline">
                      {descExpanded
                        ? <><ChevronUp size={12} />{t('movieDetail.seeLess')}</>
                        : <><ChevronDown size={12} />{t('movieDetail.seeMore')}</>
                      }
                    </span>
                  )}
                </button>
              </div>
            )}

            {/* Personal note */}
            <div>
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm text-gray-500 dark:text-gray-400">{t('movieDetail.personalNote')}</p>
                {!editingNote && (
                  <button onClick={() => setEditingNote(true)} className="text-gray-400 hover:text-amber-500 transition-colors">
                    <Pencil size={13} />
                  </button>
                )}
              </div>
              {editingNote ? (
                <div className="space-y-2">
                  <textarea
                    value={note}
                    onChange={e => setNote(e.target.value)}
                    className="input text-sm h-24 resize-none"
                    placeholder={t('movieDetail.notePlaceholder')}
                    autoFocus
                  />
                  <div className="flex gap-2">
                    <button onClick={handleSaveNote} className="btn-primary text-sm py-1.5 flex items-center gap-1">
                      <Check size={14} /> {t('movieDetail.save')}
                    </button>
                    <button onClick={() => { setNote(movie.personal_note || ''); setEditingNote(false); }} className="btn-ghost text-sm py-1.5">
                      {t('movieDetail.cancel')}
                    </button>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
                  {localMovie.personal_note || <span className="text-gray-400 italic">{t('movieDetail.noNotes')}</span>}
                </p>
              )}
            </div>

            {/* Collections */}
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

            {/* Actions */}
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
        </div>
    </SheetModal>
  );
}
