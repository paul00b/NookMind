import {useEffect, useRef, useState} from 'react';
import type { Series, TmdbEpisode } from '../types';
import { useSeries } from '../context/SeriesContext';
import { useSeriesCategories } from '../context/SeriesCategoriesContext';
import StarRating from './StarRating';
import SeasonGrid, { deriveSeriesStatus } from './SeasonGrid';
import SheetModal, { SheetCloseButton } from './SheetModal';
import ExpandableDescription from './ExpandableDescription';
import EditableNote from './EditableNote';
import { fetchSeasonDetails, fetchSeriesDetails, extractSeriesData } from '../lib/tmdb';
import { X, Trash2, Tv, ChevronDown, FolderPlus, FolderMinus, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { getRatingStyle, type SeasonState } from '../lib/imdbRatingStyle';
import { fetchSeriesImdbId, fetchSeasonRatings, type EpisodeRating } from '../lib/imdb';

interface SelectedEpisodeInfo {
  episodeNum: number;
  seasonNum: number;
  tmdb?: TmdbEpisode;
  imdb?: EpisodeRating;
}

function EpisodeDetailSheet({ info, onClose }: { info: SelectedEpisodeInfo; onClose: () => void }) {
  const { t, i18n } = useTranslation();
  const stillUrl = info.tmdb?.still_path ? `https://image.tmdb.org/t/p/w400${info.tmdb.still_path}` : null;
  const name = info.tmdb?.name ?? info.imdb?.title ?? `Episode ${info.episodeNum}`;

  return (
    <SheetModal
      onClose={onClose}
      rootClassName="z-[70]"
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[80vh] flex flex-col overflow-hidden"
    >
      <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
        <X size={20} />
      </SheetCloseButton>
      <div className="flex-shrink-0 h-6" />
      <div className="overflow-y-auto flex-1 pb-4">
        {stillUrl && (
          <div className="rounded-xl mx-3 overflow-hidden aspect-video bg-gray-100 dark:bg-gray-800">
            <img src={stillUrl} alt={name} className="w-full h-full object-cover" loading="lazy" />
          </div>
        )}
        <div className={`p-6 ${!stillUrl ? 'pt-6' : 'pt-4'}`}>
          <p className="text-xs text-gray-400 mb-0.5">S{info.seasonNum}E{info.episodeNum}</p>
          <div className="flex items-start justify-between gap-3">
            <h3 className="font-serif font-bold text-lg text-gray-900 dark:text-gray-100 leading-tight flex-1">{name}</h3>
            <div className="flex items-center gap-2 flex-shrink-0">
              {typeof info.tmdb?.vote_average === 'number' && info.tmdb.vote_average > 0 && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-400 text-xs font-semibold">
                  <Star size={11} className="fill-current" />
                  {info.tmdb.vote_average.toFixed(1)}
                </span>
              )}
              {info.imdb?.imdbRating != null && (
                <div className="px-3 py-1.5 rounded-lg text-sm font-extrabold" style={getRatingStyle(info.imdb.imdbRating)}>
                  {info.imdb.imdbRating.toFixed(1)}
                </div>
              )}
            </div>
          </div>
          {(info.tmdb?.air_date || (info.tmdb?.runtime != null && info.tmdb.runtime > 0)) && (
            <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
              {info.tmdb.air_date && (
                <span>{new Date(info.tmdb.air_date).toLocaleDateString(
                  i18n.language.startsWith('fr') ? 'fr-FR' : 'en-US',
                  { day: 'numeric', month: 'long', year: 'numeric' }
                )}</span>
              )}
              {info.tmdb.runtime != null && info.tmdb.runtime > 0 && <span>{info.tmdb.runtime} min</span>}
            </div>
          )}
          {info.tmdb?.overview ? (
            <div className="mt-3">
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">{t('nextUp.episodeSynopsis')}</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">{info.tmdb.overview}</p>
            </div>
          ) : (
            <p className="mt-3 text-sm text-gray-400 italic">{t('nextUp.noEpisodeOverview')}</p>
          )}
          {info.imdb?.imdbId && (
            <a href={`https://www.imdb.com/title/${info.imdb.imdbId}/`} target="_blank" rel="noopener noreferrer"
              className="mt-4 inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 hover:underline">
              {t('seriesDetail.viewOnImdb')}
            </a>
          )}
        </div>
      </div>
    </SheetModal>
  );
}

interface Props {
  series: Series;
  onClose: () => void;
}

export default function SeriesDetailModal({ series, onClose }: Props) {
  const { updateSeries, deleteSeries } = useSeries();
  const { seriesCategories, addSeriesToCategory, removeSeriesFromCategory } = useSeriesCategories();
  const { t } = useTranslation();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [showImdbSection, setShowImdbSection] = useState(false);
  const [imdbId, setImdbId] = useState<string | null>(null);
  const [loadingImdb, setLoadingImdb] = useState(false);
  const [imdbError, setImdbError] = useState<'not_found' | 'no_key' | null>(null);
  const [seasonRatings, setSeasonRatings] = useState<Record<number, SeasonState>>({});
  const [fetchKey, setFetchKey] = useState(0);
  const [localSeries, setLocalSeries] = useState<Series>(series);
  const [showSeasonsSection, setShowSeasonsSection] = useState(true);

  // Rafraîchissement silencieux des données TMDB à l'ouverture pour les séries en cours
  useEffect(() => {
    if (series.status !== 'watching' || !series.tmdb_id) return;
    fetchSeriesDetails(series.tmdb_id).then(tmdbData => {
      if (!tmdbData) return;
      const extracted = extractSeriesData(tmdbData);
      const nextAirDate = extracted.next_air_date ?? null;
      const nextSeasonNumber = extracted.next_season_number ?? null;
      const seasons = extracted.seasons;
      // Ne mettre à jour que si les données ont changé
      if (
        nextAirDate === series.next_air_date &&
        nextSeasonNumber === series.next_season_number &&
        seasons === series.seasons
      ) return;
      const updates: Partial<Series> = { seasons, next_air_date: nextAirDate, next_season_number: nextSeasonNumber };
      setLocalSeries(s => ({ ...s, ...updates }));
      updateSeries(series.id, updates);
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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

  // Section épisodes (tuiles)
  const [selectedSeason, setSelectedSeason] = useState<number | null>(null);
  const [tmdbEpisodes, setTmdbEpisodes] = useState<Record<number, TmdbEpisode[]>>({});
  const [loadingTmdbSeason, setLoadingTmdbSeason] = useState<number | null>(null);
  const [selectedEpisode, setSelectedEpisode] = useState<SelectedEpisodeInfo | null>(null);
  const loadedSeasonsRef = useRef<Set<number>>(new Set());

  const handleRatingChange = async (rating: number) => {
    setLocalSeries(s => ({ ...s, rating }));
    await updateSeries(series.id, { rating });
    toast.success(t('seriesDetail.ratingUpdated'));
  };

  const handleSaveNote = async (newNote: string) => {
    setLocalSeries(s => ({ ...s, personal_note: newNote }));
    await updateSeries(series.id, { personal_note: newNote });
    toast.success(t('seriesDetail.noteSaved'));
  };

  const handleSeasonsChange = async (watchedSeasons: number[], watchedEpisodes: Record<string, number[]>) => {
    const newStatus = deriveSeriesStatus(watchedSeasons, localSeries.seasons);
    const updates: Partial<Series> = { watched_seasons: watchedSeasons, watched_episodes: watchedEpisodes, status: newStatus };
    if (newStatus === 'want_to_watch') updates.rating = null;
    setLocalSeries(s => ({ ...s, ...updates }));
    await updateSeries(series.id, updates);
  };

  const currentTmdbEps = selectedSeason !== null ? (tmdbEpisodes[selectedSeason] ?? []) : [];
  const currentImdbEps = selectedSeason !== null && Array.isArray(seasonRatings[selectedSeason])
    ? (seasonRatings[selectedSeason] as EpisodeRating[])
    : [];
  const episodesToShow = currentTmdbEps.length > 0
    ? currentTmdbEps.map(ep => ({ episodeNum: ep.episode_number, tmdb: ep, imdb: currentImdbEps.find(ie => ie.episode === ep.episode_number) }))
    : currentImdbEps.map(ep => ({ episodeNum: ep.episode, tmdb: undefined, imdb: ep }));

  const handleSelectSeason = async (season: number) => {
    setSelectedSeason(season);
    if (!localSeries.tmdb_id || loadedSeasonsRef.current.has(season)) return;
    loadedSeasonsRef.current.add(season);
    setLoadingTmdbSeason(season);
    try {
      const details = await fetchSeasonDetails(localSeries.tmdb_id, season);
      if (details) setTmdbEpisodes(prev => ({ ...prev, [season]: details.episodes }));
    } finally {
      setLoadingTmdbSeason(null);
    }
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

  const isWaitingForNextSeason =
    localSeries.status === 'watching' && (
      localSeries.next_season_number !== null
        ? localSeries.watched_seasons.length >= localSeries.next_season_number - 1
        : localSeries.seasons !== null && localSeries.watched_seasons.length >= localSeries.seasons
    );

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
    <>
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[90vh] flex flex-col overflow-hidden"
    >
        <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </SheetCloseButton>

        {/* Header : poster + titre/badges côte à côte */}
        <div className="flex gap-4 p-6 pb-4 pr-14 flex-shrink-0">
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
                localSeries.status === 'watched' ? 'bg-emerald-500' : isWaitingForNextSeason ? 'bg-purple-500' : localSeries.status === 'watching' ? 'bg-blue-500' : 'bg-amber-500'
              }`}>
                {localSeries.status === 'watched'
                  ? t('seriesDetail.watched')
                  : isWaitingForNextSeason
                  ? t('seriesDetail.waitingNextSeason')
                  : localSeries.status === 'watching'
                  ? t('seriesDetail.watching')
                  : t('seriesDetail.wantToWatch')}
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

        {/* Tout le contenu scrollable */}
        <div className="overflow-y-auto flex-1 min-h-0">

          {/* Description */}
          {localSeries.description && (
            <div className="px-6 pt-1 pb-4">
              <ExpandableDescription
                description={localSeries.description}
                seeMoreText={t('seriesDetail.seeMore')}
                seeLessText={t('seriesDetail.seeLess')}
                clampClass="line-clamp-3"
              />
            </div>
          )}

          {/* Section Saisons + Épisodes */}
          {localSeries.seasons && localSeries.seasons > 0 && (
            <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden mx-4 mb-3">
              <button
                className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
                onClick={() => {
                  const opening = !showSeasonsSection;
                  setShowSeasonsSection(opening);
                  if (opening) setShowImdbSection(false);
                }}
              >
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{t('seriesDetail.episodesSection')}</span>
                <ChevronDown size={16} className={`text-gray-400 transition-transform duration-300 ${showSeasonsSection ? 'rotate-180' : ''}`} />
              </button>
              <div className={`overflow-hidden transition-[max-height] duration-300 ease-in-out ${showSeasonsSection ? 'max-h-[9999px]' : 'max-h-0'}`}>
                <div className="border-t border-black/[0.06] dark:border-white/[0.06]">
                  {/* SeasonGrid */}
                  <div className="px-4 py-4">
                    <SeasonGrid
                      totalSeasons={localSeries.seasons}
                      watchedSeasons={localSeries.watched_seasons}
                      watchedEpisodes={localSeries.watched_episodes ?? {}}
                      onChange={handleSeasonsChange}
                      episodeCounts={localSeries.tmdb_id ? episodeCounts : undefined}
                      episodeAirDates={localSeries.tmdb_id ? episodeAirDates : undefined}
                      onSeasonExpand={localSeries.tmdb_id ? handleSeasonExpand : undefined}
                      loadingEpisodesSeason={loadingEpisodesSeason}
                      onSeasonToggle={localSeries.tmdb_id ? (s) => { if (s !== null) void handleSelectSeason(s); else setSelectedSeason(null); } : undefined}
                    />
                  </div>

                  {/* Tuiles d'épisodes */}
                  {localSeries.tmdb_id && selectedSeason !== null && (
                    <div className="border-t border-black/[0.06] dark:border-white/[0.06]">
                      {loadingTmdbSeason === selectedSeason ? (
                        <div className="grid grid-cols-2 gap-2 p-4">
                          {Array.from({ length: 6 }).map((_, i) => (
                            <div key={i} className="rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 animate-pulse">
                              <div className="aspect-video" />
                              <div className="p-2 space-y-1.5">
                                <div className="h-2 w-8 bg-gray-200 dark:bg-gray-700 rounded" />
                                <div className="h-3 w-full bg-gray-200 dark:bg-gray-700 rounded" />
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : episodesToShow.length > 0 ? (
                        <div className="grid grid-cols-2 gap-2 p-4">
                          {episodesToShow.map(({ episodeNum, tmdb, imdb }) => (
                            <button
                              key={episodeNum}
                              onClick={() => setSelectedEpisode({ episodeNum, seasonNum: selectedSeason, tmdb, imdb })}
                              className="text-left bg-gray-50 dark:bg-gray-800/50 rounded-xl overflow-hidden hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors"
                            >
                              <div className="w-full aspect-video bg-gray-200 dark:bg-gray-700 overflow-hidden">
                                {tmdb?.still_path ? (
                                  <img src={`https://image.tmdb.org/t/p/w185${tmdb.still_path}`} alt={tmdb.name} className="w-full h-full object-cover" loading="lazy" />
                                ) : (
                                  <div className="w-full h-full flex items-center justify-center">
                                    <Tv size={18} className="text-gray-400 dark:text-gray-500" />
                                  </div>
                                )}
                              </div>
                              <div className="p-2">
                                <p className="text-[10px] text-gray-400 mb-0.5">E{episodeNum}</p>
                                <p className="text-xs font-medium text-gray-800 dark:text-gray-200 line-clamp-1">
                                  {tmdb?.name ?? imdb?.title ?? `Episode ${episodeNum}`}
                                </p>
                                {imdb?.imdbRating != null && (
                                  <div className="mt-1 px-1.5 py-0.5 rounded text-[10px] font-bold inline-flex" style={getRatingStyle(imdb.imdbRating)}>
                                    {imdb.imdbRating.toFixed(1)}
                                  </div>
                                )}
                              </div>
                            </button>
                          ))}
                        </div>
                      ) : (
                        <p className="px-4 py-4 text-sm text-gray-400 text-center">{t('seriesDetail.noEpisodeData')}</p>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Section Notes IMDB */}
          <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden mx-4 mb-3">
            <button
              className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
              onClick={() => { const opening = !showImdbSection; setShowImdbSection(opening); if (opening) setShowSeasonsSection(false); }}
            >
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{t('seriesDetail.imdbRatings')}</span>
              <ChevronDown size={16} className={`text-gray-400 transition-transform duration-300 ${showImdbSection ? 'rotate-180' : ''}`} />
            </button>
            <div className={`overflow-hidden transition-[max-height] duration-300 ease-in-out ${showImdbSection ? 'max-h-[2000px]' : 'max-h-0'}`}>
              <div className="border-t border-black/[0.06] dark:border-white/[0.06]">
                {imdbError === 'no_key' && (
                  <p className="px-4 py-4 text-sm text-gray-400 text-center">{t('seriesDetail.imdbNoApiKey')}</p>
                )}
                {imdbError === 'not_found' && (
                  <div className="px-4 py-4 text-center space-y-2">
                    <p className="text-sm text-gray-400">{t('seriesDetail.imdbNotAvailable')}</p>
                    <button onClick={() => { setImdbError(null); setFetchKey(k => k + 1); }} className="btn-ghost text-sm">{t('seriesDetail.imdbRetry')}</button>
                  </div>
                )}
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
                {!imdbError && imdbSeasons.length === 0 && (
                  <div className="px-4 py-3">
                    <div className="overflow-x-auto pb-2">
                      <div className="flex gap-2.5" style={{ minWidth: 'max-content' }}>
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
                                      <div className="w-11 h-7 rounded flex items-center justify-center text-xs font-bold select-none" style={style}>
                                        {ep.imdbRating?.toFixed(1) ?? 'N/A'}
                                      </div>
                                    );
                                    return ep.imdbId ? (
                                      <a key={ep.episode} href={`https://www.imdb.com/title/${ep.imdbId}/`} target="_blank" rel="noopener noreferrer" title={`E${ep.episode} · ${ep.title}${ep.imdbRating ? ` · ${ep.imdbRating}` : ''}`} className="hover:opacity-80 transition-opacity">
                                        {cell}
                                      </a>
                                    ) : (
                                      <div key={ep.episode} title={`E${ep.episode} · ${ep.title}`} className="cursor-default">{cell}</div>
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

          {/* Note, rating, catégories, suppression */}
          <div className="px-6 pb-6 space-y-4">
            {localSeries.status === 'watched' && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('seriesDetail.yourRating')}</p>
                <StarRating value={localSeries.rating} onChange={handleRatingChange} size={26} />
              </div>
            )}

            <EditableNote
              note={localSeries.personal_note}
              labelText={t('seriesDetail.personalNote')}
              placeholderText={t('seriesDetail.notePlaceholder')}
              saveText={t('seriesDetail.save')}
              cancelText={t('seriesDetail.cancel')}
              noNotesText={t('seriesDetail.noNotes')}
              onSave={handleSaveNote}
            />

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
        </div>
    </SheetModal>

    {selectedEpisode && (
      <EpisodeDetailSheet info={selectedEpisode} onClose={() => setSelectedEpisode(null)} />
    )}
    </>
  );
}
