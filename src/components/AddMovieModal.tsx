import { useEffect, useMemo, useState } from 'react';
import { X, Film, AlertTriangle, Pencil, ChevronDown } from 'lucide-react';
import { useMovies } from '../context/MoviesContext';
import type { Movie, MovieStatus, TmdbMovie } from '../types';
import StarRating from './StarRating';
import SheetModal, { SheetCloseButton } from './SheetModal';
import ExpandableDescription from './ExpandableDescription';
import { fetchMovieDetails, getPosterUrl } from '../lib/tmdb';
import { useTranslation } from 'react-i18next';

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

type MovieFormData = Omit<Movie, 'id' | 'user_id' | 'created_at'>;

const today = () => new Date().toISOString().slice(0, 10);

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
  watched_date: null,
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
  const [editing, setEditing] = useState(false);
  const [tmdbMovie, setTmdbMovie] = useState<TmdbMovie | null>(null);
  const [castOpen, setCastOpen] = useState(false);
  const isFromSearch = !!prefill;

  useEffect(() => {
    if (!isFromSearch || !form.tmdb_id) return;
    let active = true;
    fetchMovieDetails(form.tmdb_id).then(details => {
      if (active) setTmdbMovie(details);
    });
    return () => { active = false; };
  }, [isFromSearch, form.tmdb_id]);

  const cast = (tmdbMovie?.credits?.cast ?? []).slice(0, 12);

  const isDuplicate = useMemo(() => {
    if (!form.title.trim()) return false;
    return movies.some(m => normalize(m.title) === normalize(form.title));
  }, [movies, form.title]);

  const set = <K extends keyof MovieFormData>(k: K, v: MovieFormData[K]) =>
    setForm(f => ({ ...f, [k]: v }));

  const handleAdd = async () => {
    if (!form.title.trim()) return;
    setSaving(true);
    const result = await addMovie(form);
    setSaving(false);
    if (result) onClose();
  };

  const addButton = (
    <button
      type={isFromSearch && !editing ? 'button' : 'submit'}
      onClick={isFromSearch && !editing ? handleAdd : undefined}
      disabled={saving || !form.title.trim()}
      className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
    >
      {saving ? (
        <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      ) : (
        <Film size={16} />
      )}
      {saving ? t('addMovie.saving') : isDuplicate ? t('addMovie.addAnyway') : t('addMovie.addToWatchlist')}
    </button>
  );

  const duplicateWarning = isDuplicate && (
    <div className="flex items-start gap-2.5 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl px-4 py-3">
      <AlertTriangle size={15} className="text-amber-500 flex-shrink-0 mt-0.5" />
      <p className="text-sm text-amber-700 dark:text-amber-300">{t('addMovie.alreadyInWatchlist')}</p>
    </div>
  );

  const statusSelector = (
    <div>
      <label className="block text-sm text-gray-500 dark:text-gray-400 mb-2">{t('addMovie.statusLabel')}</label>
      <div className="flex gap-2">
        {(['want_to_watch', 'watched'] as MovieStatus[]).map(s => (
          <button
            key={s}
            type="button"
            onClick={() => {
              set('status', s);
              if (s === 'want_to_watch') { set('rating', null); set('watched_date', null); }
              if (s === 'watched' && !form.watched_date) set('watched_date', today());
            }}
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
  );

  const watchedFields = form.status === 'watched' && (
    <>
      <div>
        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-1">{t('addMovie.watchedDateLabel')}</label>
        <input
          className="input"
          type="date"
          value={form.watched_date || ''}
          max={today()}
          onChange={e => set('watched_date', e.target.value || null)}
        />
      </div>
      <div>
        <label className="block text-sm text-gray-500 dark:text-gray-400 mb-1">{t('addMovie.ratingLabel')}</label>
        <StarRating value={form.rating} onChange={v => set('rating', v)} size={26} />
      </div>
    </>
  );

  const noteField = (
    <div>
      <label className="block text-sm text-gray-500 dark:text-gray-400 mb-1">{t('addMovie.noteLabel')}</label>
      <textarea
        className="input resize-none h-20 text-sm"
        value={form.personal_note || ''}
        onChange={e => set('personal_note', e.target.value || null)}
        placeholder={t('addMovie.notePlaceholder')}
      />
    </div>
  );

  /* ── Preview mode (from search, not editing) ── */
  if (isFromSearch && !editing) {
    return (
      <SheetModal
        onClose={onClose}
        panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh]"
        scrollable
      >
        <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </SheetCloseButton>

        <div className="p-6 space-y-6">
          <div className="flex flex-col md:flex-row gap-6">
            <div className="flex-shrink-0 mx-auto md:mx-0">
              <div className="w-32 md:w-40 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
                {form.poster_url ? (
                  <img src={form.poster_url} alt={form.title} className="w-full h-full object-cover" />
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
                  {form.title}
                </h2>
                {form.director && (
                  <p className="text-gray-600 dark:text-gray-400 font-medium">{form.director}</p>
                )}
              </div>

              <div className="flex flex-wrap gap-2 text-sm">
                {form.genre && (
                  <span className="bg-amber-500/10 text-amber-700 dark:text-amber-400 px-3 py-1 rounded-full font-medium">
                    {form.genre}
                  </span>
                )}
                <span className={`px-3 py-1 rounded-full font-medium text-white ${
                  form.status === 'watched' ? 'bg-emerald-500' : 'bg-amber-500'
                }`}>
                  {form.status === 'watched' ? t('movieDetail.watched') : t('movieDetail.wantToWatch')}
                </span>
                {form.release_date && (
                  <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                    {form.release_date.slice(0, 4)}
                  </span>
                )}
                {form.runtime && (
                  <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                    {t('movieDetail.runtime', { count: form.runtime })}
                  </span>
                )}
              </div>

              {form.description && (
                <ExpandableDescription
                  description={form.description}
                  label={t('movieDetail.description')}
                  seeMoreText={t('movieDetail.seeMore')}
                  seeLessText={t('movieDetail.seeLess')}
                />
              )}
            </div>
          </div>

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

          {statusSelector}

          {watchedFields}

          {noteField}

          {duplicateWarning}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={() => setEditing(true)}
              className="btn-ghost flex-1 flex items-center justify-center gap-1.5"
            >
              <Pencil size={14} />
              {t('addMovie.edit')}
            </button>
            {addButton}
          </div>
        </div>
      </SheetModal>
    );
  }

  /* ── Form mode (manual add, or editing from search) ── */
  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh]"
      scrollable
      header={
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('addMovie.title')}</h2>
          <SheetCloseButton className="btn-ghost p-2"><X size={18} /></SheetCloseButton>
        </div>
      }
    >
      <form onSubmit={e => { e.preventDefault(); handleAdd(); }} className="p-6 space-y-4">
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

        {/* Description */}
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addMovie.descriptionLabel')}</label>
          <textarea
            className="input resize-none h-24 text-sm"
            value={form.description || ''}
            onChange={e => set('description', e.target.value || null)}
            placeholder={t('addMovie.descriptionPlaceholder')}
          />
        </div>

        {/* Status */}
        {statusSelector}

        {/* Watched date & Rating */}
        {watchedFields}

        {/* Note */}
        {noteField}

        {/* Duplicate warning */}
        {duplicateWarning}

        {/* Actions */}
        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={isFromSearch ? () => setEditing(false) : onClose}
            className="btn-ghost flex-1"
          >
            {t('addMovie.cancel')}
          </button>
          {addButton}
        </div>
      </form>
    </SheetModal>
  );
}
