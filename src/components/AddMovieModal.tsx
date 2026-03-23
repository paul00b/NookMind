import { useState, useMemo } from 'react';
import { X, Film, AlertTriangle } from 'lucide-react';
import { useMovies } from '../context/MoviesContext';
import type { Movie, MovieStatus } from '../types';
import StarRating from './StarRating';
import { useTranslation } from 'react-i18next';

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

type MovieFormData = Omit<Movie, 'id' | 'user_id' | 'created_at'>;

const EMPTY: MovieFormData = {
  tmdb_id: null,
  title: '',
  director: '',
  description: null,
  poster_url: null,
  release_date: null,
  runtime: null,
  genre: null,
  status: 'want_to_watch',
  rating: null,
  personal_note: null,
};

interface Props {
  prefill?: Partial<MovieFormData>;
  onClose: () => void;
}

export default function AddMovieModal({ prefill, onClose }: Props) {
  const { addMovie, movies } = useMovies();
  const { t } = useTranslation();
  const [form, setForm] = useState<MovieFormData>({ ...EMPTY, ...prefill });
  const [saving, setSaving] = useState(false);

  const isDuplicate = useMemo(() => {
    if (!form.title.trim()) return false;
    return movies.some(m => normalize(m.title) === normalize(form.title));
  }, [movies, form.title]);

  const set = <K extends keyof MovieFormData>(k: K, v: MovieFormData[K]) =>
    setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    setSaving(true);
    const result = await addMovie(form);
    setSaving(false);
    if (result) onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-0 md:p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      {/* Modal */}
      <div className="relative z-10 w-full md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('addMovie.title')}</h2>
          <button onClick={onClose} className="btn-ghost p-2"><X size={18} /></button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Poster preview */}
          {form.poster_url && (
            <div className="flex justify-center mb-2">
              <div className="w-20 aspect-[2/3] rounded-xl overflow-hidden shadow-md">
                <img src={form.poster_url} alt="Poster" className="w-full h-full object-cover" />
              </div>
            </div>
          )}

          {/* Title & Director */}
          <div className="grid grid-cols-1 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.titleLabel')}</label>
              <input
                className="input"
                value={form.title}
                onChange={e => set('title', e.target.value)}
                placeholder={t('addMovie.titlePlaceholder')}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.directorLabel')}</label>
              <input
                className="input"
                value={form.director}
                onChange={e => set('director', e.target.value)}
                placeholder={t('addMovie.directorPlaceholder')}
              />
            </div>
          </div>

          {/* Genre & Year */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.genreLabel')}</label>
              <input className="input" value={form.genre || ''} onChange={e => set('genre', e.target.value || null)} placeholder={t('addMovie.genrePlaceholder')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.releasedLabel')}</label>
              <input className="input" value={form.release_date || ''} onChange={e => set('release_date', e.target.value || null)} placeholder={t('addMovie.releasedPlaceholder')} />
            </div>
          </div>

          {/* Runtime & Poster URL */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.runtimeLabel')}</label>
              <input className="input" type="number" min="1" value={form.runtime || ''} onChange={e => set('runtime', e.target.value ? parseInt(e.target.value) : null)} placeholder={t('addMovie.runtimePlaceholder')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.posterUrlLabel')}</label>
              <input className="input" value={form.poster_url || ''} onChange={e => set('poster_url', e.target.value || null)} placeholder={t('addMovie.posterUrlPlaceholder')} />
            </div>
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('addMovie.statusLabel')}</label>
            <div className="flex gap-2">
              {(['want_to_watch', 'watched'] as MovieStatus[]).map(s => (
                <button
                  key={s}
                  type="button"
                  onClick={() => { set('status', s); if (s === 'want_to_watch') set('rating', null); }}
                  className={`flex-1 py-2.5 rounded-full text-sm font-medium border transition-all ${
                    form.status === s
                      ? 'bg-amber-500 text-white border-amber-500'
                      : 'bg-transparent text-gray-600 dark:text-gray-400 border-black/10 dark:border-white/10 hover:border-amber-500/50'
                  }`}
                >
                  {s === 'watched' ? t('addMovie.alreadyWatched') : t('addMovie.wantToWatch')}
                </button>
              ))}
            </div>
          </div>

          {/* Rating (only if watched) */}
          {form.status === 'watched' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('addMovie.ratingLabel')}</label>
              <StarRating value={form.rating} onChange={v => set('rating', v)} size={24} />
            </div>
          )}

          {/* Note */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.noteLabel')}</label>
            <textarea
              className="input resize-none h-20 text-sm"
              value={form.personal_note || ''}
              onChange={e => set('personal_note', e.target.value || null)}
              placeholder={t('addMovie.notePlaceholder')}
            />
          </div>

          {/* Description */}
          {form.description && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.descriptionLabel')}</label>
              <textarea
                className="input resize-none h-20 text-sm"
                value={form.description}
                onChange={e => set('description', e.target.value || null)}
              />
            </div>
          )}

          {/* Duplicate warning */}
          {isDuplicate && (
            <div className="flex items-start gap-2.5 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl px-4 py-3">
              <AlertTriangle size={15} className="text-amber-500 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-amber-700 dark:text-amber-300">{t('addMovie.alreadyInWatchlist')}</p>
            </div>
          )}

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">{t('addMovie.cancel')}</button>
            <button type="submit" disabled={saving || !form.title.trim()} className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
              {saving ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Film size={16} />
              )}
              {saving ? t('addMovie.saving') : isDuplicate ? t('addMovie.addAnyway') : t('addMovie.addToWatchlist')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
