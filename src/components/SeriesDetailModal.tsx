import { useState, useRef, useEffect } from 'react';
import type { Series } from '../types';
import { useSeries } from '../context/SeriesContext';
import { useSeriesCategories } from '../context/SeriesCategoriesContext';
import StarRating from './StarRating';
import SeasonGrid, { deriveSeriesStatus } from './SeasonGrid';
import { fetchSeasonEpisodeCount } from '../lib/tmdb';
import { X, Pencil, Check, Trash2, Tv, ChevronDown, ChevronUp, FolderPlus, FolderMinus, Eye } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import SeriesRatingsModal from './SeriesRatingsModal';

interface Props {
  series: Series;
  onClose: () => void;
}

export default function SeriesDetailModal({ series, onClose }: Props) {
  const { updateSeries, deleteSeries } = useSeries();
  const { seriesCategories, addSeriesToCategory, removeSeriesFromCategory } = useSeriesCategories();
  const { t } = useTranslation();
  const [editingNote, setEditingNote] = useState(false);
  const [note, setNote] = useState(series.personal_note || '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [showRatings, setShowRatings] = useState(false);
  const [localSeries, setLocalSeries] = useState<Series>(series);
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);
  useEffect(() => {
    if (descRef.current) setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [localSeries.description]);
  const [episodeCounts, setEpisodeCounts] = useState<Record<string, number>>({});
  const [loadingEpisodesSeason, setLoadingEpisodesSeason] = useState<number | null>(null);

  const handleRatingChange = async (rating: number) => {
    setLocalSeries(s => ({ ...s, rating }));
    await updateSeries(series.id, { rating });
    toast.success(t('seriesDetail.ratingUpdated'));
  };

  const handleSaveNote = async () => {
    setLocalSeries(s => ({ ...s, personal_note: note }));
    await updateSeries(series.id, { personal_note: note });
    setEditingNote(false);
    toast.success(t('seriesDetail.noteSaved'));
  };

  const handleSeasonsChange = async (watchedSeasons: number[], watchedEpisodes: Record<string, number[]>) => {
    const newStatus = deriveSeriesStatus(watchedSeasons, localSeries.seasons);
    const updates: Partial<Series> = { watched_seasons: watchedSeasons, watched_episodes: watchedEpisodes, status: newStatus };
    if (newStatus === 'want_to_watch') updates.rating = null;
    setLocalSeries(s => ({ ...s, ...updates }));
    await updateSeries(series.id, updates);
  };

  const handleSeasonExpand = async (seasonNumber: number) => {
    if (!localSeries.tmdb_id || episodeCounts[String(seasonNumber)] != null) return;
    setLoadingEpisodesSeason(seasonNumber);
    try {
      const count = await fetchSeasonEpisodeCount(localSeries.tmdb_id, seasonNumber);
      if (count != null) setEpisodeCounts(prev => ({ ...prev, [String(seasonNumber)]: count }));
    } finally {
      setLoadingEpisodesSeason(null);
    }
  };

  const handleDelete = async () => {
    await deleteSeries(series.id);
    onClose();
  };

  return (
    <>
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-0 md:p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      <div className="relative z-10 w-full md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none md:max-h-[90vh] overflow-y-auto">
        <div className="absolute top-4 right-4 flex items-center gap-1 z-10">
          <button
            onClick={() => setShowRatings(true)}
            className="btn-ghost p-2"
            title={t('seriesDetail.viewImdbRatings')}
          >
            <Eye size={18} />
          </button>
          <button onClick={onClose} className="btn-ghost p-2">
            <X size={20} />
          </button>
        </div>

        <div className="flex flex-col md:flex-row gap-6 p-6">
          {/* Poster */}
          <div className="flex-shrink-0 mx-auto md:mx-0">
            <div className="w-32 md:w-40 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
              {localSeries.poster_url ? (
                <img src={localSeries.poster_url} alt={localSeries.title} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={40} className="text-gray-400" />
                </div>
              )}
            </div>
          </div>

          {/* Details */}
          <div className="flex-1 min-w-0 space-y-4">
            <div>
              <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight mb-1">
                {localSeries.title}
              </h2>
              <p className="text-gray-600 dark:text-gray-400 font-medium">{localSeries.creator}</p>
            </div>

            <div className="flex flex-wrap gap-2 text-sm">
              {localSeries.genre && (
                <span className="bg-amber-500/10 text-amber-700 dark:text-amber-400 px-3 py-1 rounded-full font-medium">
                  {localSeries.genre}
                </span>
              )}
              <span className={`px-3 py-1 rounded-full font-medium text-white ${
                localSeries.status === 'watched' ? 'bg-emerald-500' : localSeries.status === 'watching' ? 'bg-blue-500' : 'bg-amber-500'
              }`}>
                {localSeries.status === 'watched' ? t('seriesDetail.watched') : localSeries.status === 'watching' ? t('seriesDetail.watching') : t('seriesDetail.wantToWatch')}
              </span>
              {localSeries.first_air_date && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                  {localSeries.first_air_date.slice(0, 4)}
                </span>
              )}
              {localSeries.seasons && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                  {t('seriesDetail.seasons', { count: localSeries.seasons })}
                </span>
              )}
            </div>

            {/* Season grid */}
            {localSeries.seasons && localSeries.seasons > 0 && (
              <SeasonGrid
                totalSeasons={localSeries.seasons}
                watchedSeasons={localSeries.watched_seasons}
                watchedEpisodes={localSeries.watched_episodes ?? {}}
                onChange={handleSeasonsChange}
                episodeCounts={localSeries.tmdb_id ? episodeCounts : undefined}
                onSeasonExpand={localSeries.tmdb_id ? handleSeasonExpand : undefined}
                loadingEpisodesSeason={loadingEpisodesSeason}
              />
            )}

            {localSeries.status === 'watched' && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('seriesDetail.yourRating')}</p>
                <StarRating value={localSeries.rating} onChange={handleRatingChange} size={22} />
              </div>
            )}

            {localSeries.description && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('seriesDetail.description')}</p>
                <button onClick={() => (descTruncated || descExpanded) && setDescExpanded(e => !e)} className="w-full text-left group">
                  <p ref={descRef} className={`text-sm text-gray-700 dark:text-gray-300 leading-relaxed ${descExpanded ? '' : 'line-clamp-4'}`}>
                    {localSeries.description}
                  </p>
                  {(descTruncated || descExpanded) && (
                    <span className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 mt-1 group-hover:underline">
                      {descExpanded
                        ? <><ChevronUp size={12} />{t('seriesDetail.seeLess')}</>
                        : <><ChevronDown size={12} />{t('seriesDetail.seeMore')}</>
                      }
                    </span>
                  )}
                </button>
              </div>
            )}

            <div>
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm text-gray-500 dark:text-gray-400">{t('seriesDetail.personalNote')}</p>
                {!editingNote && (
                  <button onClick={() => setEditingNote(true)} className="text-gray-400 hover:text-amber-500 transition-colors">
                    <Pencil size={13} />
                  </button>
                )}
              </div>
              {editingNote ? (
                <div className="space-y-2">
                  <textarea value={note} onChange={e => setNote(e.target.value)} className="input text-sm h-24 resize-none" placeholder={t('seriesDetail.notePlaceholder')} autoFocus />
                  <div className="flex gap-2">
                    <button onClick={handleSaveNote} className="btn-primary text-sm py-1.5 flex items-center gap-1">
                      <Check size={14} /> {t('seriesDetail.save')}
                    </button>
                    <button onClick={() => { setNote(series.personal_note || ''); setEditingNote(false); }} className="btn-ghost text-sm py-1.5">
                      {t('seriesDetail.cancel')}
                    </button>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
                  {localSeries.personal_note || <span className="text-gray-400 italic">{t('seriesDetail.noNotes')}</span>}
                </p>
              )}
            </div>

            {seriesCategories.length > 0 && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">{t('seriesDetail.collections')}</p>
                <div className="flex flex-wrap gap-2">
                  {seriesCategories.map(cat => {
                    const isIn = cat.series_ids.includes(localSeries.id);
                    return (
                      <button
                        key={cat.id}
                        onClick={() => isIn
                          ? removeSeriesFromCategory(cat.id, localSeries.id)
                          : addSeriesToCategory(cat.id, [localSeries.id])
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
              {!confirmDelete ? (
                <button onClick={() => setConfirmDelete(true)} className="btn-ghost text-sm text-red-500 hover:bg-red-500/10 flex items-center gap-1.5">
                  <Trash2 size={14} /> {t('seriesDetail.delete')}
                </button>
              ) : (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-600 dark:text-gray-400">{t('seriesDetail.areYouSure')}</span>
                  <button onClick={handleDelete} className="text-red-500 font-medium hover:underline">{t('seriesDetail.yesDelete')}</button>
                  <button onClick={() => setConfirmDelete(false)} className="text-gray-500 hover:underline">{t('seriesDetail.cancel')}</button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>

    <SeriesRatingsModal
      isOpen={showRatings}
      onClose={() => setShowRatings(false)}
      title={localSeries.title}
      creator={localSeries.creator || undefined}
      description={localSeries.description}
      posterUrl={localSeries.poster_url}
      firstAirDate={localSeries.first_air_date}
      totalSeasons={localSeries.seasons}
      genre={localSeries.genre || undefined}
      showAddButton={false}
    />
    </>
  );
}
