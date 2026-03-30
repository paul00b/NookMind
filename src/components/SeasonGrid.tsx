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
  onSeasonExpand?: (seasonNumber: number) => void;
  loadingEpisodesSeason?: number | null;
}

type PendingFill =
  | { type: 'episode'; season: number; episode: number }
  | { type: 'episode-remove'; season: number; episode: number; nextWatched: number }
  | { type: 'season'; season: number }
  | null;

export default function SeasonGrid({
  totalSeasons, watchedSeasons, watchedEpisodes = {}, onChange, readonly, compact,
  episodeCounts, onSeasonExpand, loadingEpisodesSeason,
}: SeasonGridProps) {
  const { t } = useTranslation();
  const [expandedSeason, setExpandedSeason] = useState<number | null>(null);
  const [pendingFill, setPendingFill] = useState<PendingFill>(null);
  const episodesEnabled = !!onSeasonExpand;
  const episodePanelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (expandedSeason !== null && episodePanelRef.current) {
      episodePanelRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [expandedSeason]);

  const isSeasonWatched = (season: number) => {
    const key = String(season);
    const count = episodeCounts?.[key];
    if (count != null && count > 0) return (watchedEpisodes[key]?.length ?? 0) >= count;
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

  const handleSeasonClick = (season: number) => {
    if (readonly) return;
    setPendingFill(null);

    if (episodesEnabled) {
      const newExpanded = expandedSeason === season ? null : season;
      setExpandedSeason(newExpanded);
      if (newExpanded !== null && episodeCounts?.[String(season)] == null) {
        onSeasonExpand!(season);
      }
    } else {
      if (!onChange) return;
      const isMarking = !watchedSeasons.includes(season);
      const next = isMarking
        ? [...watchedSeasons, season].sort((a, b) => a - b)
        : watchedSeasons.filter(s => s !== season);
      onChange(next, watchedEpisodes);

      // Offer to fill previous unwatched seasons
      if (isMarking && season > 1) {
        const hasPrevUnwatched = Array.from({ length: season - 1 }, (_, i) => i + 1)
          .some(s => !watchedSeasons.includes(s));
        if (hasPrevUnwatched) setPendingFill({ type: 'season', season });
      }
    }
  };

  const toggleEpisode = (season: number, episode: number) => {
    if (readonly || !onChange) return;
    setPendingFill(null);

    const key = String(season);
    const currentEps = watchedEpisodes[key] ?? [];
    const isMarking = !currentEps.includes(episode);
    const newEps = isMarking
      ? [...currentEps, episode].sort((a, b) => a - b)
      : currentEps.filter(e => e !== episode);
    const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
    const count = episodeCounts?.[key] ?? 0;
    const allWatched = count > 0 && newEps.length >= count;
    const newWatchedSeasons = allWatched
      ? [...new Set([...watchedSeasons, season])].sort((a, b) => a - b)
      : watchedSeasons.filter(s => s !== season);
    onChange(newWatchedSeasons, newWatchedEpisodes);

    // Offer to fill previous unwatched episodes
    if (isMarking && episode > 1) {
      const hasPrevUnwatched = Array.from({ length: episode - 1 }, (_, i) => i + 1)
        .some(e => !newEps.includes(e));
      if (hasPrevUnwatched) setPendingFill({ type: 'episode', season, episode });
    }

    // Offer to remove following watched episodes
    if (!isMarking) {
      const nextWatched = newEps.filter(e => e > episode).length;
      if (nextWatched > 0) setPendingFill({ type: 'episode-remove', season, episode, nextWatched });
    }
  };

  const confirmFill = (yes: boolean) => {
    if (!yes || !pendingFill || !onChange) { setPendingFill(null); return; }

    if (pendingFill.type === 'episode-remove') {
      const { season, episode } = pendingFill;
      const key = String(season);
      const currentEps = watchedEpisodes[key] ?? [];
      const newEps = currentEps.filter(e => e <= episode);
      const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
      const newWatchedSeasons = watchedSeasons.filter(s => s !== season);
      onChange(newWatchedSeasons, newWatchedEpisodes);
    } else if (pendingFill.type === 'episode') {
      const { season, episode } = pendingFill;
      const key = String(season);
      const currentEps = watchedEpisodes[key] ?? [];
      const newEps = [
        ...new Set([...currentEps, ...Array.from({ length: episode }, (_, i) => i + 1)]),
      ].sort((a, b) => a - b);
      const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
      const count = episodeCounts?.[key] ?? 0;
      const allWatched = count > 0 && newEps.length >= count;
      const newWatchedSeasons = allWatched
        ? [...new Set([...watchedSeasons, season])].sort((a, b) => a - b)
        : watchedSeasons.filter(s => s !== season);
      onChange(newWatchedSeasons, newWatchedEpisodes);
    } else {
      const { season } = pendingFill;
      const newWatchedSeasons = [
        ...new Set([...watchedSeasons, ...Array.from({ length: season }, (_, i) => i + 1)]),
      ].sort((a, b) => a - b);
      onChange(newWatchedSeasons, watchedEpisodes);
    }

    setPendingFill(null);
  };

  const toggleAllEpisodes = (season: number) => {
    if (readonly || !onChange) return;
    setPendingFill(null);
    const key = String(season);
    const count = episodeCounts?.[key];
    if (!count) return;
    const currentEps = watchedEpisodes[key] ?? [];
    const allWatched = currentEps.length >= count;
    const newEps = allWatched ? [] : Array.from({ length: count }, (_, i) => i + 1);
    const newWatchedEpisodes = { ...watchedEpisodes, [key]: newEps };
    const newWatchedSeasons = allWatched
      ? watchedSeasons.filter(s => s !== season)
      : [...new Set([...watchedSeasons, season])].sort((a, b) => a - b);
    onChange(newWatchedSeasons, newWatchedEpisodes);
  };

  const size = compact ? 'w-6 h-6 text-[10px]' : 'w-8 h-8 text-xs';
  const watchedCount = Array.from({ length: totalSeasons }, (_, i) => i + 1).filter(s => isSeasonWatched(s)).length;

  const getSeasonButtonClass = (season: number) => {
    const watched = isSeasonWatched(season);
    const partial = isSeasonPartial(season);
    const isExpanded = expandedSeason === season;
    const ring = isExpanded ? 'ring-2 ring-offset-1 ring-offset-white dark:ring-offset-gray-900' : '';
    if (watched) return `${size} rounded-lg font-semibold transition-all flex items-center justify-center bg-emerald-500 text-white shadow-sm ${ring} ${isExpanded ? 'ring-emerald-300' : ''}`;
    if (partial) return `${size} rounded-lg font-semibold transition-all flex items-center justify-center bg-blue-400 text-white shadow-sm ${ring} ${isExpanded ? 'ring-blue-300' : ''}`;
    return `${size} rounded-lg font-semibold transition-all flex items-center justify-center bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 ${
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
      <div className="mt-2 flex items-center gap-2 text-xs bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/50 rounded-lg px-3 py-2">
        <span className="flex-1 text-amber-800 dark:text-amber-300">{label}</span>
        <button type="button" onClick={() => confirmFill(true)} className="font-semibold text-amber-700 dark:text-amber-400 hover:underline shrink-0">
          {t('seriesDetail.yes')}
        </button>
        <button type="button" onClick={() => confirmFill(false)} className="text-gray-500 dark:text-gray-400 hover:underline shrink-0">
          {t('seriesDetail.no')}
        </button>
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
            {season}
          </button>
        ))}
      </div>

      {/* "Also add previous seasons?" banner (season-only mode) */}
      {!compact && !episodesEnabled && <PendingFillBanner />}

      {!compact && episodesEnabled && expandedSeason === null && (
        <p className="mt-1.5 text-[11px] text-gray-400 dark:text-gray-500">
          {t('seriesDetail.clickSeasonHint')}
        </p>
      )}

      {!compact && episodesEnabled && expandedSeason !== null && (
        <div ref={episodePanelRef} className="mt-3 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-xl border border-gray-100 dark:border-gray-700">
          <div className="flex items-center justify-between mb-2.5">
            <p className="text-xs font-semibold text-gray-600 dark:text-gray-300">
              {t('seriesDetail.season')} {expandedSeason}
              {episodeCounts?.[String(expandedSeason)] != null && (
                <span className="font-normal text-gray-400 ml-1">
                  — {watchedEpisodes[String(expandedSeason)]?.length ?? 0}/{episodeCounts?.[String(expandedSeason)]} {t('seriesDetail.episodes')}
                </span>
              )}
            </p>
            {!readonly && (episodeCounts?.[String(expandedSeason)] ?? 0) > 0 && (
              <button
                type="button"
                onClick={() => toggleAllEpisodes(expandedSeason)}
                className="text-xs text-amber-600 dark:text-amber-400 hover:underline"
              >
                {(watchedEpisodes[String(expandedSeason)]?.length ?? 0) >= (episodeCounts[String(expandedSeason)] ?? 0)
                  ? t('seriesDetail.unmarkAll')
                  : t('seriesDetail.markAll')}
              </button>
            )}
          </div>

          {loadingEpisodesSeason === expandedSeason ? (
            <div className="flex items-center gap-2 text-xs text-gray-400">
              <Loader2 size={13} className="animate-spin" />
              {t('seriesDetail.loadingEpisodes')}
            </div>
          ) : episodeCounts?.[String(expandedSeason)] != null ? (
            <>
              <div className="flex flex-wrap gap-1">
                {Array.from({ length: episodeCounts[String(expandedSeason)] }, (_, i) => i + 1).map(ep => {
                  const isWatched = watchedEpisodes[String(expandedSeason)]?.includes(ep) ?? false;
                  return (
                    <button
                      key={ep}
                      type="button"
                      onClick={() => toggleEpisode(expandedSeason, ep)}
                      disabled={readonly}
                      className={`w-7 h-7 rounded-md text-[11px] font-medium transition-all flex items-center justify-center ${
                        isWatched
                          ? 'bg-emerald-500 text-white'
                          : `bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 ${!readonly ? 'hover:bg-amber-500/20 hover:text-amber-600 dark:hover:text-amber-400' : ''}`
                      } ${readonly ? 'cursor-default' : 'cursor-pointer'}`}
                      title={`${t('seriesDetail.episode')} ${ep}`}
                    >
                      {ep}
                    </button>
                  );
                })}
              </div>
              {/* "Also add previous episodes?" banner */}
              <PendingFillBanner />
            </>
          ) : (
            <p className="text-xs text-gray-400">{t('seriesDetail.noEpisodeData')}</p>
          )}
        </div>
      )}
    </div>
  );
}

export function deriveSeriesStatus(watchedSeasons: number[], totalSeasons: number | null): 'watched' | 'watching' | 'want_to_watch' {
  if (watchedSeasons.length === 0) return 'want_to_watch';
  if (totalSeasons && watchedSeasons.length >= totalSeasons) return 'watched';
  return 'watching';
}
