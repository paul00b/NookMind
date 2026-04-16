import { useEffect, useMemo, useRef, useState } from 'react';
import { X, Tv, Plus, ChevronDown, Star, CheckCircle2 } from 'lucide-react';
import { fetchSeriesImdbId, fetchSeasonRatings, type EpisodeRating } from '../lib/imdb';
import { getRatingStyle, type SeasonState } from '../lib/imdbRatingStyle';
import { fetchSeasonDetails, fetchSeriesDetails, getPosterUrl } from '../lib/tmdb';
import type { TmdbEpisode, TmdbSeries } from '../types';
import { useTranslation } from 'react-i18next';
import SheetModal, { SheetCloseButton } from './SheetModal';

interface SeriesPreviewSheetProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  creator?: string;
  description?: string | null;
  posterUrl?: string | null;
  firstAirDate?: string | null;
  totalSeasons?: number | null;
  genre?: string | null;
  showAddButton?: boolean;
  alreadyAdded?: boolean;
  onAdd?: () => void;
  tmdbId?: number | null;
}


function computeStats(seasonRatings: Record<number, SeasonState>) {
  const allRatings: number[] = [];
  for (const val of Object.values(seasonRatings)) {
    if (Array.isArray(val)) {
      val.forEach(ep => { if (ep.imdbRating !== null) allRatings.push(ep.imdbRating); });
    }
  }
  if (allRatings.length === 0) return null;
  return {
    average: (allRatings.reduce((a, b) => a + b, 0) / allRatings.length).toFixed(1),
    best: Math.max(...allRatings).toFixed(1),
    worst: Math.min(...allRatings).toFixed(1),
  };
}

const MAX_SEASONS_FALLBACK = 20;

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
        <p className="text-xs text-gray-400 mb-0.5">
          S{info.seasonNum}E{info.episodeNum}
        </p>
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-serif font-bold text-lg text-gray-900 dark:text-gray-100 leading-tight flex-1">
            {name}
          </h3>
          <div className="flex items-center gap-2 flex-shrink-0">
            {typeof info.tmdb?.vote_average === 'number' && info.tmdb.vote_average > 0 && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-700 dark:text-amber-400 text-xs font-semibold">
                <Star size={11} className="fill-current" />
                {info.tmdb.vote_average.toFixed(1)}
              </span>
            )}
            {info.imdb?.imdbRating != null && (
              <div
                className="px-3 py-1.5 rounded-lg text-sm font-extrabold"
                style={getRatingStyle(info.imdb.imdbRating)}
              >
                {info.imdb.imdbRating.toFixed(1)}
              </div>
            )}
          </div>
        </div>

        {(info.tmdb?.air_date || (info.tmdb?.runtime != null && info.tmdb.runtime > 0)) && (
          <div className="flex items-center gap-3 mt-2 text-xs text-gray-400">
            {info.tmdb.air_date && (
              <span>
                {new Date(info.tmdb.air_date).toLocaleDateString(
                  i18n.language.startsWith('fr') ? 'fr-FR' : 'en-US',
                  { day: 'numeric', month: 'long', year: 'numeric' }
                )}
              </span>
            )}
            {info.tmdb.runtime != null && info.tmdb.runtime > 0 && (
              <span>{info.tmdb.runtime} min</span>
            )}
          </div>
        )}

        {info.tmdb?.overview ? (
          <div className="mt-3">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
              {t('nextUp.episodeSynopsis')}
            </p>
            <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
              {info.tmdb.overview}
            </p>
          </div>
        ) : (
          <p className="mt-3 text-sm text-gray-400 italic">{t('nextUp.noEpisodeOverview')}</p>
        )}

        {info.imdb?.imdbId && (
          <a
            href={`https://www.imdb.com/title/${info.imdb.imdbId}/`}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-4 inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 hover:underline"
          >
            {t('seriesDetail.viewOnImdb')}
          </a>
        )}
      </div>
      </div>
    </SheetModal>
  );
}

export default function SeriesPreviewSheet({
  isOpen,
  onClose,
  title,
  creator,
  description,
  posterUrl,
  firstAirDate,
  totalSeasons,
  genre,
  showAddButton = false,
  alreadyAdded = false,
  onAdd,
  tmdbId,
}: SeriesPreviewSheetProps) {
  const { t } = useTranslation();
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);

  // IMDB state
  const [imdbId, setImdbId] = useState<string | null>(null);
  const [loadingImdb, setLoadingImdb] = useState(false);
  const [imdbError, setImdbError] = useState<'not_found' | 'no_key' | null>(null);
  const [seasonRatings, setSeasonRatings] = useState<Record<number, SeasonState>>({});
  const [fetchKey, setFetchKey] = useState(0);

  // Accordion state
  const [imdbSectionOpen, setImdbSectionOpen] = useState(false);
  const [episodesSectionOpen, setEpisodesSectionOpen] = useState(false);

  // Episodes state
  const [selectedSeason, setSelectedSeason] = useState<number | null>(null);
  const [tmdbEpisodes, setTmdbEpisodes] = useState<Record<number, TmdbEpisode[]>>({});
  const [loadingTmdbSeason, setLoadingTmdbSeason] = useState<number | null>(null);
  const [selectedEpisode, setSelectedEpisode] = useState<SelectedEpisodeInfo | null>(null);
  const [tmdbSeasonCount, setTmdbSeasonCount] = useState<number | null>(null);
  const [loadingSeasonCount, setLoadingSeasonCount] = useState(false);
  const loadedSeasonsRef = useRef<Set<number>>(new Set());
  const [tmdbSeries, setTmdbSeries] = useState<TmdbSeries | null>(null);
  const [castSectionOpen, setCastSectionOpen] = useState(false);

  useEffect(() => {
    if (!descRef.current) return;
    setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [description, isOpen]);

  // Reset et recherche imdbID à l'ouverture
  useEffect(() => {
    if (!isOpen) return;
    setImdbId(null);
    setImdbError(null);
    setSeasonRatings({});
    setLoadingImdb(true);
    setSelectedSeason(null);
    setTmdbEpisodes({});
    setSelectedEpisode(null);
    setTmdbSeasonCount(null);
    setTmdbSeries(null);
    setLoadingSeasonCount(false);
    setEpisodesSectionOpen(false);
    setImdbSectionOpen(false);
    setCastSectionOpen(false);
    loadedSeasonsRef.current = new Set();

    fetchSeriesImdbId(title).then(id => {
      setLoadingImdb(false);
      if (!id) { setImdbError('not_found'); return; }
      setImdbId(id);
    });
  }, [isOpen, title, fetchKey]);

  // Charge les saisons IMDB en parallèle une fois l'imdbID obtenu
  useEffect(() => {
    if (!imdbId) return;
    const max = totalSeasons ?? MAX_SEASONS_FALLBACK;

    for (let s = 1; s <= max; s++) {
      const season = s;
      setSeasonRatings(prev => ({ ...prev, [season]: 'loading' }));
      fetchSeasonRatings(imdbId, season).then(ratings => {
        if (ratings === null && !totalSeasons) {
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
  }, [imdbId, totalSeasons]);

  const effectiveSeasonCount = totalSeasons ?? tmdbSeasonCount;
  const cast = (tmdbSeries?.credits?.cast ?? []).slice(0, 12);
  const availableSeasons = useMemo(() => {
    if (effectiveSeasonCount && effectiveSeasonCount > 0) return Array.from({ length: effectiveSeasonCount }, (_, i) => i + 1);
    return Object.keys(seasonRatings).map(Number).sort((a, b) => a - b);
  }, [effectiveSeasonCount, seasonRatings]);

  const handleSelectSeason = async (season: number) => {
    setSelectedSeason(season);
    if (!tmdbId || loadedSeasonsRef.current.has(season)) return;
    loadedSeasonsRef.current.add(season);
    setLoadingTmdbSeason(season);
    try {
      const details = await fetchSeasonDetails(tmdbId, season);
      if (details) setTmdbEpisodes(prev => ({ ...prev, [season]: details.episodes }));
    } finally {
      setLoadingTmdbSeason(null);
    }
  };

  const handleToggleEpisodesSection = async () => {
    const newOpen = !episodesSectionOpen;
    setEpisodesSectionOpen(newOpen);
    if (!newOpen) return;

    // Si on ne connaît pas encore le nb de saisons, le récupérer via TMDB
    const knownCount = totalSeasons ?? tmdbSeasonCount;
    if (!knownCount && tmdbId && !loadingSeasonCount) {
      setLoadingSeasonCount(true);
      try {
        const details = await fetchSeriesDetails(tmdbId);
        if (details) {
          setTmdbSeries(details);
          const count = details.number_of_seasons ?? null;
          setTmdbSeasonCount(count);
          if (selectedSeason === null && count && count > 0) {
            void handleSelectSeason(1);
          }
        }
      } finally {
        setLoadingSeasonCount(false);
      }
      return;
    }

    if (selectedSeason === null) {
      const firstSeason = knownCount && knownCount > 0
        ? 1
        : (Object.keys(seasonRatings).map(Number).sort((a, b) => a - b)[0] ?? null);
      if (firstSeason !== null) void handleSelectSeason(firstSeason);
    }
  };

  useEffect(() => {
    if (!isOpen || !tmdbId) return;
    let active = true;
    fetchSeriesDetails(tmdbId).then(details => {
      if (!active || !details) return;
      setTmdbSeries(details);
      if (!tmdbSeasonCount) setTmdbSeasonCount(details.number_of_seasons ?? null);
    });
    return () => { active = false; };
  }, [isOpen, tmdbId, tmdbSeasonCount]);

  if (!isOpen) return null;

  const stats = computeStats(seasonRatings);
  const imdbSeasons = Object.keys(seasonRatings).map(Number).sort((a, b) => a - b);
  const hasImdbData = imdbSeasons.length > 0;

  const currentTmdbEps = selectedSeason !== null ? (tmdbEpisodes[selectedSeason] ?? []) : [];
  const currentImdbEps = selectedSeason !== null && Array.isArray(seasonRatings[selectedSeason])
    ? (seasonRatings[selectedSeason] as EpisodeRating[])
    : [];

  const episodesToShow = currentTmdbEps.length > 0
    ? currentTmdbEps.map(ep => ({
        episodeNum: ep.episode_number,
        tmdb: ep,
        imdb: currentImdbEps.find(ie => ie.episode === ep.episode_number),
      }))
    : currentImdbEps.map(ep => ({
        episodeNum: ep.episode,
        tmdb: undefined,
        imdb: ep,
      }));

  return (
    <>
      <SheetModal
        onClose={onClose}
        rootClassName="z-[60]"
        panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[90vh] flex flex-col overflow-hidden"
      >
        {/* Close */}
        <SheetCloseButton className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </SheetCloseButton>

        {/* Header */}
        <div className="flex items-start gap-4 p-6 pb-4 flex-shrink-0">
          <div className="w-14 md:w-16 aspect-[2/3] rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
            {posterUrl ? (
              <img src={posterUrl} alt={title} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <Tv size={22} className="text-gray-400" />
              </div>
            )}
          </div>
          <div className="flex-1 min-w-0 pr-8">
            <h2 className="font-bold text-base text-gray-900 dark:text-gray-100 leading-tight">{title}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {[creator, genre, firstAirDate?.slice(0, 4), totalSeasons ? t('seriesDetail.seasons', { count: totalSeasons }) : null]
                .filter(Boolean)
                .join(' · ')}
            </p>
            {showAddButton && (
              alreadyAdded ? (
                <div className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-emerald-500/30 text-xs font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-500/5">
                  <CheckCircle2 size={13} strokeWidth={2.5} />
                  {t('seriesHome.inList')}
                </div>
              ) : onAdd && (
                <button
                  onClick={() => { onClose(); onAdd(); }}
                  className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-amber-500/40 text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-500/10 transition-colors"
                >
                  <Plus size={13} strokeWidth={2.5} />
                  {t('seriesDetail.addToCollection')}
                </button>
              )
            )}
          </div>
        </div>

        <div className="overflow-y-auto flex-1 min-h-0">

        {description && (
          <div className="px-6 pb-4">
            <p
              ref={descRef}
              className={`text-xs text-gray-500 dark:text-gray-400 leading-relaxed ${descExpanded ? '' : 'line-clamp-3'}`}
            >
              {description}
            </p>
            {(descTruncated || descExpanded) && (
              <button
                onClick={() => setDescExpanded(e => !e)}
                className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 mt-1 hover:underline"
              >
                <ChevronDown size={12} className={`transition-transform duration-200 ${descExpanded ? 'rotate-180' : ''}`} />
                {descExpanded ? t('seriesDetail.seeLess') : t('seriesDetail.seeMore')}
              </button>
            )}
          </div>
        )}

          {cast.length > 0 && (
            <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden mx-4 mb-3">
              <button
                className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
                onClick={() => setCastSectionOpen(open => !open)}
              >
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {t('seriesDetail.cast')} ({cast.length})
                </span>
                <ChevronDown
                  size={16}
                  className={`text-gray-400 transition-transform duration-300 ${castSectionOpen ? 'rotate-180' : ''}`}
                />
              </button>
              <div className={`overflow-clip transition-[max-height] duration-300 ease-in-out ${castSectionOpen ? 'max-h-64' : 'max-h-0'}`}>
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

          {/* Section 1 : Épisodes */}
          {(!!tmdbId || availableSeasons.length > 0) && (
            <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden mx-4 mb-3">
              <button
                className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
                onClick={handleToggleEpisodesSection}
              >
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                  {t('seriesDetail.episodesSection')}
                </span>
                <ChevronDown
                  size={16}
                  className={`text-gray-400 transition-transform duration-300 ${episodesSectionOpen ? 'rotate-180' : ''}`}
                />
              </button>

              <div className={`overflow-clip transition-[max-height] duration-300 ease-in-out ${episodesSectionOpen ? 'max-h-[200vh]' : 'max-h-0'}`}>
                <div className="border-t border-black/[0.06] dark:border-white/[0.06]">

                  {/* Sélecteur de saison */}
                  {loadingSeasonCount ? (
                    <div className="px-4 py-4 text-center text-sm text-gray-400 animate-pulse">
                      {t('seriesDetail.loadingEpisodes')}
                    </div>
                  ) : availableSeasons.length === 0 ? (
                    <div className="px-4 py-4 text-center text-sm text-gray-400">
                      {t('seriesDetail.noEpisodeData')}
                    </div>
                  ) : null}
                  {availableSeasons.length > 0 && (
                    <div
                      className="flex gap-2 overflow-x-auto px-4 py-3 border-b border-black/[0.06] dark:border-white/[0.06]"
                      style={{ scrollbarWidth: 'none' }}
                    >
                      {availableSeasons.map(s => (
                        <button
                          key={s}
                          onClick={() => handleSelectSeason(s)}
                          className={`flex-shrink-0 px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                            selectedSeason === s
                              ? 'bg-amber-500 text-white'
                              : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-amber-500/10 hover:text-amber-600 dark:hover:text-amber-400'
                          }`}
                        >
                          S{s}
                        </button>
                      ))}
                    </div>
                  )}

                  {/* Tuiles d'épisodes */}
                  {selectedSeason !== null && (
                    loadingTmdbSeason === selectedSeason ? (
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
                                <img
                                  src={`https://image.tmdb.org/t/p/w185${tmdb.still_path}`}
                                  alt={tmdb.name}
                                  className="w-full h-full object-cover"
                                  loading="lazy"
                                />
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
                                <div
                                  className="mt-1 px-1.5 py-0.5 rounded text-[10px] font-bold inline-flex"
                                  style={getRatingStyle(imdb.imdbRating)}
                                >
                                  {imdb.imdbRating.toFixed(1)}
                                </div>
                              )}
                            </div>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="px-4 py-4 text-sm text-gray-400 text-center">
                        {t('seriesDetail.noEpisodeData')}
                      </p>
                    )
                  )}

                </div>
              </div>
            </div>
          )}

          {/* Section 2 : Notes IMDB */}
          <div className="border border-black/[0.06] dark:border-white/[0.06] rounded-xl overflow-hidden mx-4 mb-3">
            <button
              className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] dark:hover:bg-white/[0.02] transition-colors"
              onClick={() => setImdbSectionOpen(open => !open)}
            >
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                {t('seriesDetail.imdbRatings')}
              </span>
              <ChevronDown
                size={16}
                className={`text-gray-400 transition-transform duration-300 ${imdbSectionOpen ? 'rotate-180' : ''}`}
              />
            </button>

              <div className={`overflow-clip transition-[max-height] duration-300 ease-in-out ${imdbSectionOpen ? 'max-h-[200vh]' : 'max-h-0'}`}>
              <div className="border-t border-black/[0.06] dark:border-white/[0.06]">

                  {imdbError === 'no_key' && (
                    <p className="px-4 py-4 text-sm text-gray-400 text-center">{t('seriesDetail.imdbNoApiKey')}</p>
                  )}
                  {imdbError === 'not_found' && (
                    <div className="px-4 py-4 text-center space-y-2">
                      <p className="text-sm text-gray-400">{t('seriesDetail.imdbNotAvailable')}</p>
                      <button onClick={() => setFetchKey(k => k + 1)} className="btn-ghost text-sm">
                        {t('seriesDetail.imdbRetry')}
                      </button>
                    </div>
                  )}
                  {loadingImdb && (
                    <div className="px-6 py-6 text-center text-sm text-gray-400 animate-pulse">
                      {t('seriesDetail.imdbLoading')}
                    </div>
                  )}

                  {/* Shimmer stats */}
                  {!imdbError && !stats && !loadingImdb && (
                    <div className="flex border-b border-black/[0.06] dark:border-white/[0.06]">
                      {[true, true, false].map((border, i) => (
                        <div key={i} className={`flex-1 py-3 flex flex-col items-center gap-1.5${border ? ' border-r border-black/[0.06] dark:border-white/[0.06]' : ''}`}>
                          <div className="h-7 w-14 rounded-md bg-gray-200 dark:bg-gray-700 animate-pulse" />
                          <div className="h-2.5 w-10 rounded bg-gray-200 dark:bg-gray-700 animate-pulse" />
                        </div>
                      ))}
                    </div>
                  )}

                  {stats && (
                    <div className="flex border-b border-black/[0.06] dark:border-white/[0.06]">
                      {[
                        { value: stats.average, label: t('seriesDetail.imdbAverage'), border: true },
                        { value: stats.best,    label: t('seriesDetail.imdbBest'),    border: true },
                        { value: stats.worst,   label: t('seriesDetail.imdbWorst'),   border: false },
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

                  {/* Shimmer grille */}
                  {!imdbError && !hasImdbData && !loadingImdb && (
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
                          {Array.from({ length: totalSeasons ?? 3 }, (_, i) => (
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

                  {hasImdbData && (
                    <div className="px-4 py-3">
                      <div className="overflow-x-auto pb-2">
                        <div className="flex gap-2.5" style={{ minWidth: 'max-content' }}>
                          {imdbSeasons.some(s => Array.isArray(seasonRatings[s]) && (seasonRatings[s] as EpisodeRating[]).length > 0) && (() => {
                            const maxEps = Math.max(...imdbSeasons.map(s => Array.isArray(seasonRatings[s]) ? (seasonRatings[s] as EpisodeRating[]).length : 0));
                            return (
                              <div key="ep-labels">
                                <div className="h-[18px] mb-1.5" />
                                <div className="flex flex-col gap-1">
                                  {Array.from({ length: maxEps }, (_, i) => (
                                    <div key={i} className="w-7 h-7 flex items-center justify-center text-[10px] font-medium text-gray-400 dark:text-gray-500">
                                      E{i + 1}
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
                      <div className="flex flex-wrap gap-x-3 gap-y-1.5 mt-4">
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

          <div className="h-6 pb-safe" />

        </div>{/* end outer scroll wrapper */}

      </SheetModal>

      {selectedEpisode && (
        <EpisodeDetailSheet info={selectedEpisode} onClose={() => setSelectedEpisode(null)} />
      )}
    </>
  );
}
