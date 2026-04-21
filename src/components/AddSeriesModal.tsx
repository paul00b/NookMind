import { useState, useMemo } from 'react';
import { X, Tv, AlertTriangle } from 'lucide-react';
import { useSeries } from '../context/SeriesContext';
import type { Series } from '../types';
import StarRating from './StarRating';
import SeasonGrid, { deriveSeriesStatus } from './SeasonGrid';
import SheetModal, { SheetCloseButton } from './SheetModal';
import { useTranslation } from 'react-i18next';
import { fetchSeasonDetails } from '../lib/tmdb';

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

type SeriesFormData = Omit<Series, 'id' | 'user_id' | 'created_at'>;

const EMPTY: SeriesFormData = {
  tmdb_id: null,
  title: '',
  creator: '',
  description: null,
  poster_url: null,
  first_air_date: null,
  seasons: null,
  watched_seasons: [],
  watched_episodes: {},
  genre: null,
  status: 'want_to_watch',
  rating: null,
  personal_note: null,
  next_air_date: null,
  next_season_number: null,
  next_episode_number: null,
};

interface Props {
  prefill?: Partial<SeriesFormData>;
  onClose: () => void;
}

const readonlyInput = 'input bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-500 pointer-events-none';

export default function AddSeriesModal({ prefill, onClose }: Props) {
  const { addSeries, series: allSeries } = useSeries();
  const { t } = useTranslation();
  const isFromSearch = !!prefill;
  const [form, setForm] = useState<SeriesFormData>({ ...EMPTY, ...prefill });
  const [saving, setSaving] = useState(false);
  const [episodeCounts, setEpisodeCounts] = useState<Record<string, number>>({});
  const [episodeAirDates, setEpisodeAirDates] = useState<Record<string, Record<number, string | null>>>({});
  const [loadingEpisodesSeason, setLoadingEpisodesSeason] = useState<number | null>(null);

  const isDuplicate = useMemo(() => {
    if (!form.title.trim()) return false;
    return allSeries.some(s => normalize(s.title) === normalize(form.title));
  }, [allSeries, form.title]);

  const set = <K extends keyof SeriesFormData>(k: K, v: SeriesFormData[K]) =>
    setForm(f => ({ ...f, [k]: v }));

  const derivedStatus = deriveSeriesStatus(form.watched_seasons, form.seasons);
  const showRating = derivedStatus === 'watched';

  const handleSeasonsChange = (watched: number[], episodes: Record<string, number[]>) => {
    const newStatus = deriveSeriesStatus(watched, form.seasons);
    setForm(f => ({ ...f, watched_seasons: watched, watched_episodes: episodes, status: newStatus, ...(newStatus === 'want_to_watch' ? { rating: null } : {}) }));
  };

  const handleSeasonExpand = async (seasonNumber: number) => {
    if (!form.tmdb_id || episodeCounts[String(seasonNumber)] != null) return;
    setLoadingEpisodesSeason(seasonNumber);
    try {
      const details = await fetchSeasonDetails(form.tmdb_id, seasonNumber);
      if (!details) return;
      setEpisodeCounts(prev => ({ ...prev, [String(seasonNumber)]: details.episodes.length }));
      setEpisodeAirDates(prev => ({
        ...prev,
        [String(seasonNumber)]: Object.fromEntries(
          details.episodes.map(ep => [ep.episode_number, ep.air_date ?? null])
        ),
      }));
    } finally {
      setLoadingEpisodesSeason(null);
    }
  };

  const handleTotalSeasonsChange = (total: number | null) => {
    // If total decreased, remove watched seasons above new total
    const newWatched = total ? form.watched_seasons.filter(s => s <= total) : form.watched_seasons;
    const newStatus = deriveSeriesStatus(newWatched, total);
    setForm(f => ({ ...f, seasons: total, watched_seasons: newWatched, status: newStatus }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    setSaving(true);
    const result = await addSeries({ ...form, status: derivedStatus });
    setSaving(false);
    if (result) onClose();
  };

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh]"
      scrollable
      header={
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('addSeries.title')}</h2>
          <SheetCloseButton className="btn-ghost p-2"><X size={18} /></SheetCloseButton>
        </div>
      }
    >
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {form.poster_url && (
            <div className="flex justify-center mb-2">
              <div className="w-20 aspect-[2/3] rounded-xl overflow-hidden shadow-md">
                <img src={form.poster_url} alt="Poster" className="w-full h-full object-cover" />
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.titleLabel')}</label>
              <input className="input" value={form.title} onChange={e => set('title', e.target.value)} placeholder={t('addSeries.titlePlaceholder')} required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.creatorLabel')}</label>
              <input className={isFromSearch ? readonlyInput : 'input'} value={form.creator} onChange={e => set('creator', e.target.value)} placeholder={t('addSeries.creatorPlaceholder')} readOnly={isFromSearch} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.genreLabel')}</label>
              <input className="input" value={form.genre || ''} onChange={e => set('genre', e.target.value || null)} placeholder={t('addSeries.genrePlaceholder')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.firstAirDateLabel')}</label>
              <input className={isFromSearch ? readonlyInput : 'input'} value={form.first_air_date || ''} onChange={e => set('first_air_date', e.target.value || null)} placeholder={t('addSeries.firstAirDatePlaceholder')} readOnly={isFromSearch} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.seasonsLabel')}</label>
              <input className={isFromSearch ? readonlyInput : 'input'} type="number" min="1" value={form.seasons || ''} onChange={e => handleTotalSeasonsChange(e.target.value ? parseInt(e.target.value) : null)} placeholder={t('addSeries.seasonsPlaceholder')} readOnly={isFromSearch} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.posterUrlLabel')}</label>
              <input className="input" value={form.poster_url || ''} onChange={e => set('poster_url', e.target.value || null)} placeholder={t('addSeries.posterUrlPlaceholder')} />
            </div>
          </div>

          {/* Season grid */}
          {form.seasons && form.seasons > 0 && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('addSeries.watchedSeasonsLabel')}</label>
              <SeasonGrid
                totalSeasons={form.seasons}
                watchedSeasons={form.watched_seasons}
                watchedEpisodes={form.watched_episodes ?? {}}
                onChange={handleSeasonsChange}
                episodeCounts={form.tmdb_id ? episodeCounts : undefined}
                episodeAirDates={form.tmdb_id ? episodeAirDates : undefined}
                onSeasonExpand={form.tmdb_id ? handleSeasonExpand : undefined}
                loadingEpisodesSeason={loadingEpisodesSeason}
              />
            </div>
          )}

          {/* Rating (only if all seasons watched) */}
          {showRating && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('addSeries.ratingLabel')}</label>
              <StarRating value={form.rating} onChange={v => set('rating', v)} size={28} />
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.noteLabel')}</label>
            <textarea className="input resize-none h-20 text-sm" value={form.personal_note || ''} onChange={e => set('personal_note', e.target.value || null)} placeholder={t('addSeries.notePlaceholder')} />
          </div>

          {form.description && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addSeries.descriptionLabel')}</label>
              <p className={`${readonlyInput} text-sm whitespace-pre-wrap`}>{form.description}</p>
            </div>
          )}

          {isDuplicate && (
            <div className="flex items-start gap-2.5 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl px-4 py-3">
              <AlertTriangle size={15} className="text-amber-500 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-amber-700 dark:text-amber-300">{t('addSeries.alreadyInList')}</p>
            </div>
          )}

          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">{t('addSeries.cancel')}</button>
            <button type="submit" disabled={saving || !form.title.trim()} className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
              {saving ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <Tv size={16} />
              )}
              {saving ? t('addSeries.saving') : isDuplicate ? t('addSeries.addAnyway') : t('addSeries.addToList')}
            </button>
          </div>
        </form>
    </SheetModal>
  );
}
