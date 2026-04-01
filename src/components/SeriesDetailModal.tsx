import { useState, useRef, useEffect } from 'react';
import type { Series } from '../types';
import { useSeries } from '../context/SeriesContext';
import { useSeriesCategories } from '../context/SeriesCategoriesContext';
import StarRating from './StarRating';
import SeasonGrid, { deriveSeriesStatus } from './SeasonGrid';
import SheetModal from './SheetModal';
import { fetchSeasonDetails } from '../lib/tmdb';
import { X, Pencil, Check, Trash2, Tv, ChevronDown, ChevronUp, FolderPlus, FolderMinus } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { getRatingStyle, type SeasonState } from './SeriesRatingsModal';
import { fetchSeriesImdbId, fetchSeasonRatings, type EpisodeRating } from '../lib/imdb';

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
  const [showImdbSection, setShowImdbSection] = useState(false);
  const [imdbId, setImdbId] = useState<string | null>(null);
  const [loadingImdb, setLoadingImdb] = useState(false);
  const [imdbError, setImdbError] = useState<'not_found' | 'no_key' | null>(null);
  const [seasonRatings, setSeasonRatings] = useState<Record<number, SeasonState>>({});
  const [fetchKey, setFetchKey] = useState(0);
  const [localSeries, setLocalSeries] = useState<Series>(series);
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (descRef.current) setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [localSeries.description]);

  // Charge l'imdbID quand la section IMDB s'ouvre pour la première fois
  useEffect(() => {
    if (!showImdbSection || imdbId || loadingImdb || imdbError) return;
    setLoadingImdb(true);
    fetchSeriesImdbId(localSeries.title).then(id => {
      setLoadingImdb(false);
      if (!id) { setImdbError('not_found'); return; }
      setImdbId(id);
    });
  }, [showImdbSection, imdbId, loadingImdb, imdbError, localSeries.title, fetchKey]);

  // Charge les saisons en parallèle une fois l'imdbID obtenu
  useEffect(() => {
    if (!imdbId) return;
    const max = localSeries.seasons ?? 20;
    for (let s = 1; s <= max; s++) {
      const season = s;
      setSeasonRatings(prev => ({ ...prev, [season]: 'loading' }));
      fetchSeasonRatings(imdbId, season).then(ratings => {
        if (ratings === null && !localSeries.seasons) {
          setSeasonRatings(prev => {
            const next = { ...prev };
            delete next[season];
            return next;
          });
        } else {
          setSeasonRatings(prev => ({ ...prev, [season]: ratings ?? 'error' }));
        }
      });
    }
  }, [imdbId, localSeries.seasons]);

  const [episodeCounts, setEpisodeCounts] = useState<Record<string, number>>({});
  const [episodeAirDates, setEpisodeAirDates] = useState<Record<string, Record<number, string | null>>>({});
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
      const details = await fetchSeasonDetails(localSeries.tmdb_id, seasonNumber);
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

  const handleDelete = async () => {
    await deleteSeries(series.id);
    onClose();
  };

  const imdbSeasons = Object.keys(seasonRatings).map(Number).sort((a, b) => a - b);
  const allImdbRatings: number[] = [];
  for (const val of Object.values(seasonRatings)) {
    if (Array.isArray(val)) val.forEach(ep => { if (ep.imdbRating !== null) allImdbRatings.push(ep.imdbRating); });
  }
  const imdbStats = allImdbRatings.length > 0 ? {
    average: (allImdbRatings.reduce((a, b) => a + b, 0) / allImdbRatings.length).toFixed(1),
    best: Math.max(...allImdbRatings).toFixed(1),
    worst: Math.min(...allImdbRatings).toFixed(1),
  } : null;

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[90vh] overflow-y-auto"
    >
        <button onClick={onClose} className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </button>

        {/* Header : poster + titre/badges côte à côte */}
        <div className="flex gap-4 p-6 pb-4 pr-14">
          <div className="flex-shrink-0">
            <div className="w-24 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
              {localSeries.poster_url ? (
                <img src={localSeries.poster_url} alt={localSeries.title} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <Tv size={32} className="text-gray-400" />
                </div>
              )}
            </div>
          </div>
          <div className="flex-1 min-w-0 flex flex-col justify-center gap-2">
            <div>
              <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100 leading-tight mb-0.5">
                {localSeries.title}
              </h2>
              <p className="text-gray-600 dark:text-gray-400 font-medium text-sm">{localSeries.creator}</p>
            </div>
            <div className="flex flex-wrap gap-1.5 text-sm">
              {localSeries.genre && (
                <span className="bg-amber-500/10 text-amber-700 dark:text-amber-400 px-2.5 py-0.5 rounded-full font-medium text-xs">
                  {localSeries.genre}
                </span>
              )}
              <span className={`px-2.5 py-0.5 rounded-full font-medium text-white text-xs ${
                localSeries.status === 'watched' ? 'bg-emerald-500' : localSeries.status === 'watching' ? 'bg-blue-500' : 'bg-amber-500'
              }`}>
                {localSeries.status === 'watched' ? t('seriesDetail.watched') : localSeries.status === 'watching' ? t('seriesDetail.watching') : t('seriesDetail.wantToWatch')}
              </span>
              {localSeries.first_air_date && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-2.5 py-0.5 rounded-full text-xs">
                  {localSeries.first_air_date.slice(0, 4)}
                </span>
              )}
              {localSeries.seasons && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-2.5 py-0.5 rounded-full text-xs">
                  {t('seriesDetail.seasons', { count: localSeries.seasons })}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Corps pleine largeur */}
        <div className="px-6 pb-6 space-y-4">
            {/* Season grid */}
            {localSeries.seasons && localSeries.seasons > 0 && (
              <SeasonGrid
                totalSeasons={localSeries.seasons}
                watchedSeasons={localSeries.watched_seasons}
                watchedEpisodes={localSeries.watched_episodes ?? {}}
                onChange={handleSeasonsChange}
                episodeCounts={localSeries.tmdb_id ? episodeCounts : undefined}
                episodeAirDates={localSeries.tmdb_id ? episodeAirDates : undefined}
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

            {/* Section IMDB inline */}
            <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden">
              <button
                className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
                onClick={() => setShowImdbSection(s => !s)}
              >
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{t('seriesDetail.imdbRatings')}</span>
                <ChevronDown size={16} className={`text-gray-400 transition-transform duration-300 ${showImdbSection ? 'rotate-180' : ''}`} />
              </button>

              <div className={`grid transition-[grid-template-rows] duration-300 ease-in-out ${showImdbSection ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'}`}>
              <div className="overflow-hidden">
                <div className="border-t border-black/[0.06] dark:border-white/[0.06]">
                  {imdbError === 'no_key' && (
                    <p className="px-4 py-4 text-sm text-gray-400 text-center">{t('seriesDetail.imdbNoApiKey')}</p>
                  )}
                  {imdbError === 'not_found' && (
                    <div className="px-4 py-4 text-center space-y-2">
                      <p className="text-sm text-gray-400">{t('seriesDetail.imdbNotAvailable')}</p>
                      <button
                        onClick={() => { setImdbError(null); setFetchKey(k => k + 1); }}
                        className="btn-ghost text-sm"
                      >{t('seriesDetail.imdbRetry')}</button>
                    </div>
                  )}

                  {/* Shimmer stats — visible tant que les données ne sont pas là */}
                  {!imdbError && !imdbStats && (
                    <div className="flex border-b border-black/[0.06] dark:border-white/[0.06]">
                      {[true, true, false].map((border, i) => (
                        <div key={i} className={`flex-1 py-3 flex flex-col items-center gap-1.5${border ? ' border-r border-black/[0.06] dark:border-white/[0.06]' : ''}`}>
                          <div className="h-7 w-14 rounded-md bg-gray-200 dark:bg-gray-700 animate-pulse" />
                          <div className="h-2.5 w-10 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
                        </div>
                      ))}
                    </div>
                  )}

                  {imdbStats && (
                    <div className="flex border-b border-black/[0.06] dark:border-white/[0.06]">
                      {[
                        { value: imdbStats.average, label: t('seriesDetail.imdbAverage'), border: true },
                        { value: imdbStats.best,    label: t('seriesDetail.imdbBest'),    border: true },
                        { value: imdbStats.worst,   label: t('seriesDetail.imdbWorst'),   border: false },
                      ].map(({ value, label, border }) => {
                        const style = getRatingStyle(parseFloat(value));
                        return (
                          <div key={label} className={`flex-1 py-3 flex flex-col items-center gap-1.5${border ? ' border-r border-black/[0.06] dark:border-white/[0.06]' : ''}`}>
                            <div className="px-3 py-1 rounded-md text-sm font-extrabold" style={style}>{value}</div>
                            <div className="text-[10px] text-gray-500 dark:text-gray-400 uppercase tracking-wide">{label}</div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {/* Shimmer grille — visible tant qu'aucune saison n'est chargée */}
                  {!imdbError && imdbSeasons.length === 0 && (
                    <div className="px-4 py-3">
                      <div className="overflow-x-auto pb-2">
                        <div className="flex gap-2.5" style={{ minWidth: 'max-content' }}>
                          {/* Colonne numéros d'épisodes */}
                          <div>
                            <div className="h-[18px] mb-1.5" />
                            <div className="flex flex-col gap-1">
                              {Array.from({ length: 6 }).map((_, j) => (
                                <div key={j} className="w-7 h-7 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
                              ))}
                            </div>
                          </div>
                          {Array.from({ length: localSeries.seasons ?? 3 }, (_, i) => (
                            <div key={i}>
                              <div className="h-[18px] w-8 rounded bg-gray-200 dark:bg-gray-700 animate-pulse mb-1.5" />
                              <div className="flex flex-col gap-1">
                                {Array.from({ length: 6 }).map((_, j) => (
                                  <div key={j} className="w-11 h-7 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}

                  {imdbSeasons.length > 0 && (
                    <div className="px-4 py-3">
                      <div className="overflow-x-auto pb-2">
                        <div className="flex gap-2.5" style={{ minWidth: 'max-content' }}>
                          {(() => {
                            const maxEps = Math.max(...imdbSeasons.map(s => Array.isArray(seasonRatings[s]) ? (seasonRatings[s] as EpisodeRating[]).length : 0));
                            const rowCount = maxEps > 0 ? maxEps : 6;
                            return (
                              <div key="ep-labels">
                                <div className="h-[18px] mb-1.5" />
                                <div className="flex flex-col gap-1">
                                  {Array.from({ length: rowCount }, (_, i) => (
                                    <div key={i} className="w-7 h-7 flex items-center justify-center text-[10px] font-medium text-gray-400 dark:text-gray-500">
                                      {maxEps > 0 ? `E${i + 1}` : ''}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            );
                          })()}
                          {imdbSeasons.map(s => {
                            const state = seasonRatings[s];
                            return (
                              <div key={s}>
                                <div className="text-[10px] text-gray-500 dark:text-gray-400 font-semibold text-center mb-1.5">S{s}</div>
                                <div className="flex flex-col gap-1">
                                  {state === 'loading' ? (
                                    Array.from({ length: 6 }).map((_, i) => (
                                      <div key={i} className="w-11 h-7 rounded animate-pulse bg-gray-200 dark:bg-gray-700" />
                                    ))
                                  ) : state === 'error' ? (
                                    <div className="w-11 h-7 rounded bg-gray-200 dark:bg-gray-700 flex items-center justify-center text-[9px] text-gray-400">—</div>
                                  ) : (
                                    state.map(ep => {
                                      const style = getRatingStyle(ep.imdbRating);
                                      const cell = (
                                        <div
                                          className="w-11 h-7 rounded flex items-center justify-center text-xs font-bold select-none"
                                          style={style}
                                        >
                                          {ep.imdbRating?.toFixed(1) ?? 'N/A'}
                                        </div>
                                      );
                                      return ep.imdbId ? (
                                        <a
                                          key={ep.episode}
                                          href={`https://www.imdb.com/title/${ep.imdbId}/`}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          title={`E${ep.episode} · ${ep.title}${ep.imdbRating ? ` · ${ep.imdbRating}` : ''}`}
                                          className="hover:opacity-80 transition-opacity"
                                        >
                                          {cell}
                                        </a>
                                      ) : (
                                        <div key={ep.episode} title={`E${ep.episode} · ${ep.title}`} className="cursor-default">
                                          {cell}
                                        </div>
                                      );
                                    })
                                  )}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-x-3 gap-y-1.5 mt-3">
                        {[
                          { label: '9–10', style: getRatingStyle(9.5) },
                          { label: '8–9',  style: getRatingStyle(8.5) },
                          { label: '7–8',  style: getRatingStyle(7.5) },
                          { label: '6–7',  style: getRatingStyle(6.5) },
                          { label: '5–6',  style: getRatingStyle(5.5) },
                          { label: '<5',   style: getRatingStyle(4)   },
                        ].map(({ label, style }) => (
                          <div key={label} className="flex items-center gap-1.5">
                            <div className="w-3 h-3 rounded-sm flex-shrink-0" style={{ background: style.background }} />
                            <span className="text-[10px] text-gray-500 dark:text-gray-400">{label}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
              </div>
            </div>

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

            <div className="flex flex-wrap gap-2 pt-2 justify-center">
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
    </SheetModal>
  );
}
