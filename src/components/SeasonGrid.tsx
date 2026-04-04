import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';

interface SeasonGridProps {
  totalSeasons: number;
  watchedSeasons: number[];
  watchedEpisodes?: Record<string, number[]>;
  onChange?: (watchedSeasons: number[], watchedEpisodes: Record<string, number[]>) => void;
  readonly?: boolean;
  compact?: boolean;
  episodeCounts?: Record<string, number>;
  episodeAirDates?: Record<string, Record<number, string | null>>;
  onSeasonExpand?: (seasonNumber: number) => void;
  loadingEpisodesSeason?: number | null;
  onSeasonToggle?: (season: number | null) => void;
  initialExpandedSeason?: number | null;
}

type PendingFill =
  | {
      type: 'episode';
      season: number;
      episode: number;
      apply: () => void;
    }
  | {
      type: 'episode-remove';
      season: number;
      episode: number;
      nextWatched: number;
      apply: () => void;
    }
  | {
      type: 'season';
      season: number;
      apply: () => void;
    }
  | null;

export default function SeasonGrid({
  totalSeasons, watchedSeasons, watchedEpisodes = {}, onChange, readonly, compact,
  episodeCounts, episodeAirDates, onSeasonExpand, loadingEpisodesSeason, onSeasonToggle,
  initialExpandedSeason,
}: SeasonGridProps) {
  const { t, i18n } = useTranslation();
  const [expandedSeason, setExpandedSeason] = useState<number | null>(initialExpandedSeason ?? null);
  const [pendingFill, setPendingFill] = useState<PendingFill>(null);
  const [episodeMessage, setEpisodeMessage] = useState<string | null>(null);
  const [episodeMessageTarget, setEpisodeMessageTarget] = useState<{ season: number; episode: number } | null>(null);
  const episodesEnabled = !!onSeasonExpand;
  const episodePanelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (expandedSeason !== null && episodePanelRef.current) {
      episodePanelRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [expandedSeason]);

  const formatEpisodeDate = (dateStr: string) => (
    new Date(dateStr).toLocaleDateString(i18n.language === 'fr' ? 'fr-FR' : 'en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    })
  );

  const getFutureEpisodeMessage = (dateStr: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const releaseDate = new Date(dateStr);
    releaseDate.setHours(0, 0, 0, 0);
    const days = Math.round((releaseDate.getTime() - today.getTime()) / 86400000);
    return t(days <= 1 ? 'seriesDetail.availableOnSoon' : 'seriesDetail.availableOn', {
      date: formatEpisodeDate(dateStr),
      days,
    });
  };

  const getResolvedEpisodes = (season: number) => {
    const key = String(season);
    const count = episodeCounts?.[key] ?? 0;
    if ((watchedEpisodes[key]?.length ?? 0) > 0) return watchedEpisodes[key] ?? [];
    if (watchedSeasons.includes(season) && count > 0) {
      return Array.from({ length: count }, (_, i) => i + 1);
    }
    return watchedEpisodes[key] ?? [];
  };

  const getEpisodeAvailability = (season: number, episode: number) => {
    const seasonAirDates = episodeAirDates?.[String(season)];
    if (!seasonAirDates || !Object.prototype.hasOwnProperty.call(seasonAirDates, episode)) return 'available' as const;
    const airDate = seasonAirDates[episode];
    if (!airDate) return 'coming_soon' as const;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const releaseDate = new Date(airDate);
    releaseDate.setHours(0, 0, 0, 0);
    return releaseDate.getTime() > today.getTime() ? 'future' as const : 'available' as const;
  };

  const getAvailableEpisodes = (season: number) => {
    const count = episodeCounts?.[String(season)] ?? 0;
    return Array.from({ length: count }, (_, i) => i + 1).filter(
      ep => getEpisodeAvailability(season, ep) === 'available'
    );
  };

  const hasAnyAvailableEpisode = (season: number) => getAvailableEpisodes(season).length > 0;

  const isSeasonWatched = (season: number) => {
    const key = String(season);
    const count = episodeCounts?.[key];
    if (count != null && count > 0) {
      const watchedCount = watchedEpisodes[key]?.length ?? 0;
      if (watchedCount === 0 && watchedSeasons.includes(season)) return true;
      return watchedCount >= count;
    }
    return watchedSeasons.includes(season);
  };

  const isSeasonPartial = (season: number) => {
    const key = String(season);
    const count = episodeCounts?.[key];
    const watched = watchedEpisodes[key]?.length ?? 0;
    if (count != null && count > 0) {
      return watched > 0 && watched < count;
    }
    // Sans compte chargé, on sait qu'une saison est partielle si des épisodes sont vus sans que la saison soit cochée entièrement
    return watched > 0 && !watchedSeasons.includes(season);
  };

  const isSeasonEmpty = (season: number) => {
    const count = episodeCounts?.[String(season)];
    return count != null && count === 0;
  };

  const handleSeasonClick = (season: number) => {
    if (readonly) return;
    setPendingFill(null);
    setEpisodeMessage(null);
    setEpisodeMessageTarget(null);

    // Always expand/collapse — never toggle the whole season on click
    const newExpanded = expandedSeason === season ? null : season;
    setExpandedSeason(newExpanded);
    if (newExpanded !== null && episodesEnabled && episodeCounts?.[String(season)] == null) {
      onSeasonExpand!(season);
    }
    onSeasonToggle?.(newExpanded);
  };

  const toggleEpisode = (season: number, episode: number) => {
    if (readonly || !onChange) return;
    setPendingFill(null);
    setEpisodeMessage(null);
    setEpisodeMessageTarget(null);

    const key = String(season);
    const availability = getEpisodeAvailability(season, episode);
    const airDate = episodeAirDates?.[key]?.[episode];
    if (availability === 'coming_soon') {
      setEpisodeMessage(t('seriesDetail.comingSoon'));
      setEpisodeMessageTarget({ season, episode });
      return;
    }
    if (availability === 'future' && airDate) {
      setEpisodeMessage(getFutureEpisodeMessage(airDate));
      setEpisodeMessageTarget({ season, episode });
      return;
    }

    const count = episodeCounts?.[key] ?? 0;
    const currentEps = getResolvedEpisodes(season);
    const isMarking = !currentEps.includes(episode);
    const newEps = isMarking
      ? [...currentEps, episode].sort((a, b) => a - b)
      : currentEps.filter(e => e !== episode);
    const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
    const allWatched = count > 0 && newEps.length >= count;
    const newWatchedSeasons = allWatched
      ? [...new Set([...watchedSeasons, season])].sort((a, b) => a - b)
      : watchedSeasons.filter(s => s !== season);
    onChange(newWatchedSeasons, newWatchedEpisodes);

    // Offer to fill previous unwatched episodes
    if (isMarking && episode > 1) {
      const hasPrevUnwatched = Array.from({ length: episode - 1 }, (_, i) => i + 1)
        .some(e => !newEps.includes(e));
      if (hasPrevUnwatched) {
        const filledEpisodes = [
          ...new Set([...newEps, ...Array.from({ length: episode }, (_, i) => i + 1)]),
        ].sort((a, b) => a - b);
        const filledWatchedEpisodes = { ...newWatchedEpisodes, [key]: filledEpisodes };
        const filledAllWatched = count > 0 && filledEpisodes.length >= count;
        const filledWatchedSeasons = filledAllWatched
          ? [...new Set([...newWatchedSeasons, season])].sort((a, b) => a - b)
          : newWatchedSeasons.filter(s => s !== season);
        setPendingFill({
          type: 'episode',
          season,
          episode,
          apply: () => onChange(filledWatchedSeasons, filledWatchedEpisodes),
        });
      }
    }

    // Offer to remove following watched episodes
    if (!isMarking) {
      const nextWatched = newEps.filter(e => e > episode).length;
      if (nextWatched > 0) {
        const trimmedEpisodes = newEps.filter(e => e <= episode);
        const trimmedWatchedEpisodes = { ...newWatchedEpisodes, [key]: trimmedEpisodes };
        const trimmedWatchedSeasons = newWatchedSeasons.filter(s => s !== season);
        setPendingFill({
          type: 'episode-remove',
          season,
          episode,
          nextWatched,
          apply: () => onChange(trimmedWatchedSeasons, trimmedWatchedEpisodes),
        });
      }
    }
  };

  const confirmFill = (yes: boolean) => {
    if (!yes || !pendingFill || !onChange) { setPendingFill(null); return; }

    pendingFill.apply();

    setPendingFill(null);
  };

  const toggleAllEpisodes = (season: number) => {
    if (readonly || !onChange) return;
    setPendingFill(null);
    setEpisodeMessage(null);
    setEpisodeMessageTarget(null);
    const key = String(season);
    const count = episodeCounts?.[key];
    if (!count) return;
    const currentEps = getResolvedEpisodes(season);
    const availableEpisodes = getAvailableEpisodes(season);
    const allAvailableWatched = availableEpisodes.length > 0 && availableEpisodes.every(ep => currentEps.includes(ep));
    const newEps = allAvailableWatched ? [] : availableEpisodes;
    const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
    const seasonFullyWatched = newEps.length >= count;
    const newWatchedSeasons = allAvailableWatched
      ? watchedSeasons.filter(s => s !== season)
      : seasonFullyWatched
      ? [...new Set([...watchedSeasons, season])].sort((a, b) => a - b)
      : watchedSeasons.filter(s => s !== season);
    onChange(newWatchedSeasons, newWatchedEpisodes);

    if (!allAvailableWatched && season > 1) {
      const hasPrevUnwatched = Array.from({ length: season - 1 }, (_, i) => i + 1)
        .some(s => !watchedSeasons.includes(s));
      if (hasPrevUnwatched) {
        const filledWatchedSeasons = [
          ...new Set([...newWatchedSeasons, ...Array.from({ length: season }, (_, i) => i + 1)]),
        ].sort((a, b) => a - b);
        setPendingFill({
          type: 'season',
          season,
          apply: () => onChange(filledWatchedSeasons, newWatchedEpisodes),
        });
      }
    }
  };

  const watchedCount = Array.from({ length: totalSeasons }, (_, i) => i + 1).filter(s => isSeasonWatched(s)).length;

  const getSeasonButtonClass = (season: number) => {
    const watched = isSeasonWatched(season);
    const partial = isSeasonPartial(season);
    const empty = isSeasonEmpty(season);
    const isExpanded = expandedSeason === season;
    const base = compact
      ? 'min-w-[3rem] px-3 py-1 text-xs rounded-xl font-medium whitespace-nowrap transition-all flex items-center justify-center shrink-0'
      : 'min-w-[3.5rem] px-4 py-1.5 text-sm rounded-xl font-medium whitespace-nowrap transition-all flex items-center justify-center shrink-0';
    const ring = isExpanded ? 'ring-2 ring-offset-1 ring-offset-white dark:ring-offset-gray-900' : '';
    if (empty) return `${base} bg-gray-100 dark:bg-gray-800 text-gray-400 dark:text-gray-500 ${ring} ${isExpanded ? 'ring-gray-300' : ''}`;
    if (watched) return `${base} bg-emerald-500 text-white shadow-sm ${ring} ${isExpanded ? 'ring-emerald-300' : ''}`;
    if (partial) return `${base} bg-blue-400 text-white shadow-sm ${ring} ${isExpanded ? 'ring-blue-300' : ''}`;
    return `${base} bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 ${
      !readonly ? 'hover:bg-amber-500/20 hover:text-amber-600 dark:hover:text-amber-400' : ''
    } ${ring} ${isExpanded ? 'ring-amber-300' : ''}`;
  };

  const PendingFillBanner = () => {
    if (!pendingFill || readonly) return null;
    const label = pendingFill.type === 'episode-remove'
      ? t('seriesDetail.removeFollowingEpisodes', { count: pendingFill.nextWatched })
      : pendingFill.type === 'episode'
      ? t('seriesDetail.addPreviousEpisodes', { count: pendingFill.episode - 1 })
      : t('seriesDetail.addPreviousSeasons', { count: pendingFill.season - 1 });
    return (
      <div
        className="relative z-10 mt-2 flex flex-col gap-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/50 rounded-xl px-3 py-3 pointer-events-auto"
        onClick={event => event.stopPropagation()}
        onMouseDown={event => event.stopPropagation()}
      >
        <span className="text-sm text-amber-900 dark:text-amber-200 leading-relaxed font-medium">{label}</span>
        <div className="flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={() => confirmFill(false)}
            className="min-w-[3.5rem] rounded-full px-3 py-1.5 text-sm font-medium border border-gray-200 dark:border-gray-700 bg-white/80 dark:bg-gray-900/50 text-gray-600 dark:text-gray-300 transition-colors hover:bg-white dark:hover:bg-gray-900 shrink-0"
          >
            {t('seriesDetail.no')}
          </button>
          <button
            type="button"
            onClick={() => confirmFill(true)}
            className="min-w-[3.5rem] rounded-full px-3.5 py-1.5 text-sm font-semibold bg-amber-500 text-white shadow-sm transition-colors hover:bg-amber-600 shrink-0"
          >
            {t('seriesDetail.yes')}
          </button>
        </div>
      </div>
    );
  };

  const EpisodeMessageBanner = () => {
    if (!episodeMessage) return null;
    return (
      <div className="mt-2 text-xs bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-2 text-gray-600 dark:text-gray-300">
        {episodeMessage}
      </div>
    );
  };

  return (
    <div>
      {!compact && (
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
          {t('seriesDetail.seasonsProgress', { watched: watchedCount, total: totalSeasons })}
        </p>
      )}
      <div className="flex flex-wrap gap-1.5">
        {Array.from({ length: totalSeasons }, (_, i) => i + 1).map(season => (
          <button
            key={season}
            type="button"
            onClick={() => handleSeasonClick(season)}
            disabled={readonly}
            className={`${getSeasonButtonClass(season)} ${readonly ? 'cursor-default' : 'cursor-pointer'}`}
            title={`${t('seriesDetail.season')} ${season}`}
          >
            S{season}
          </button>
        ))}
      </div>

      {!compact && expandedSeason === null && (
        <p className="mt-1.5 text-[11px] text-gray-400 dark:text-gray-500">
          {t('seriesDetail.clickSeasonHint')}
        </p>
      )}

      {!compact && expandedSeason !== null && (
        <div ref={episodePanelRef} className="mt-3 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-100 dark:border-gray-700">
          {(() => {
            const epCount = episodeCounts?.[String(expandedSeason)];
            const hasEpisodeData = epCount != null;
            const isEmpty = hasEpisodeData && epCount === 0;
            const canMarkAvailableEpisodes = !hasEpisodeData || !epCount || hasAnyAvailableEpisode(expandedSeason);
            return (
              <>
                <div className="flex items-center justify-between mb-2.5">
                  <p className="text-xs font-semibold text-gray-600 dark:text-gray-300">
                    {t('seriesDetail.season')} {expandedSeason}
                    {hasEpisodeData && !isEmpty && (
                      <span className="font-normal text-gray-400 ml-1">
                        — {isSeasonWatched(expandedSeason) ? epCount : watchedEpisodes[String(expandedSeason)]?.length ?? 0}/{epCount} {t('seriesDetail.episodes')}
                      </span>
                    )}
                  </p>
                  {!readonly && onChange && !isEmpty && canMarkAvailableEpisodes && (
                    <button
                      type="button"
                      onClick={() => {
                        if ((epCount ?? 0) > 0) {
                          toggleAllEpisodes(expandedSeason);
                        } else {
                          const isMarking = !watchedSeasons.includes(expandedSeason);
                          const next = isMarking
                            ? [...watchedSeasons, expandedSeason].sort((a, b) => a - b)
                            : watchedSeasons.filter(s => s !== expandedSeason);
                          onChange(next, watchedEpisodes);
                          if (isMarking && expandedSeason > 1) {
                            const hasPrevUnwatched = Array.from({ length: expandedSeason - 1 }, (_, i) => i + 1)
                              .some(s => !watchedSeasons.includes(s));
                            if (hasPrevUnwatched) {
                              const filledWatchedSeasons = [
                                ...new Set([...next, ...Array.from({ length: expandedSeason }, (_, i) => i + 1)]),
                              ].sort((a, b) => a - b);
                              setPendingFill({
                                type: 'season',
                                season: expandedSeason,
                                apply: () => onChange(filledWatchedSeasons, watchedEpisodes),
                              });
                            }
                          }
                        }
                      }}
                      className="text-xs text-amber-600 dark:text-amber-400 hover:underline"
                    >
                      {isSeasonWatched(expandedSeason)
                        ? t('seriesDetail.unmarkAll')
                        : t('seriesDetail.markAll')}
                    </button>
                  )}
                </div>

                {episodesEnabled && loadingEpisodesSeason === expandedSeason ? (
                  <div className="flex items-center gap-2 text-xs text-gray-400">
                    <Loader2 size={13} className="animate-spin" />
                    {t('seriesDetail.loadingEpisodes')}
                  </div>
                ) : isEmpty ? (
                  <p className="text-xs text-gray-400 italic">{t('seriesDetail.comingSoon')}</p>
                ) : hasEpisodeData ? (
                  <>
                    <div className="flex flex-wrap gap-1">
                      {Array.from({ length: epCount! }, (_, i) => i + 1).map(ep => {
                        const resolvedEpisodes = getResolvedEpisodes(expandedSeason);
                        const isWatched = resolvedEpisodes.includes(ep);
                        const airDate = episodeAirDates?.[String(expandedSeason)]?.[ep];
                        const availability = getEpisodeAvailability(expandedSeason, ep);
                        const isUnavailable = availability !== 'available';
                        const isMessageTarget = episodeMessageTarget?.season === expandedSeason && episodeMessageTarget.episode === ep;
                        return (
                          <button
                            key={ep}
                            type="button"
                            onClick={() => toggleEpisode(expandedSeason, ep)}
                            disabled={readonly}
                            className={`w-8 h-8 rounded-md text-xs font-medium transition-all flex items-center justify-center ${
                              isWatched
                                ? 'bg-emerald-500 text-white'
                                : isUnavailable
                                ? 'bg-gray-100 dark:bg-gray-700 text-gray-400 dark:text-gray-500'
                                : `bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 ${!readonly ? 'hover:bg-amber-500/20 hover:text-amber-600 dark:hover:text-amber-400' : ''}`
                            } ${isMessageTarget ? 'ring-2 ring-amber-400' : ''} ${readonly ? 'cursor-default' : 'cursor-pointer'}`}
                            title={`${t('seriesDetail.episode')} ${ep}${isUnavailable ? ` · ${airDate ? formatEpisodeDate(airDate) : t('seriesDetail.comingSoon')}` : ''}`}
                          >
                            {ep}
                          </button>
                        );
                      })}
                    </div>
                    <EpisodeMessageBanner />
                    <PendingFillBanner />
                  </>
                ) : !episodesEnabled ? (
                  <PendingFillBanner />
                ) : (
                  <p className="text-xs text-gray-400">{t('seriesDetail.noEpisodeData')}</p>
                )}
              </>
            );
          })()}
        </div>
      )}
    </div>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function deriveSeriesStatus(watchedSeasons: number[], totalSeasons: number | null): 'watched' | 'watching' | 'want_to_watch' {
  if (watchedSeasons.length === 0) return 'want_to_watch';
  if (totalSeasons && watchedSeasons.length >= totalSeasons) return 'watched';
  return 'watching';
}
