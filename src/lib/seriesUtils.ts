import type { Series } from '../types';

function parseDateOnly(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number);
  return new Date(year, (month ?? 1) - 1, day ?? 1);
}

function isFutureAirDate(dateStr: string | null): boolean {
  if (!dateStr) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return parseDateOnly(dateStr).getTime() > today.getTime();
}

function getReleasedSeasonCount(series: Series): number | null {
  if (series.seasons === null) return null;

  const hasFutureSeasonAnnounced =
    series.next_season_number !== null &&
    isFutureAirDate(series.next_air_date) &&
    series.next_season_number <= series.seasons;

  if (!hasFutureSeasonAnnounced) return series.seasons;

  return Math.max(0, (series.next_season_number ?? 1) - 1);
}

function hasWatchedAllPreviousEpisodesInNextSeason(series: Series): boolean {
  if (series.next_season_number === null || series.next_episode_number === null) return false;
  if (series.next_episode_number <= 1) return true;

  const watchedEpisodes = new Set(series.watched_episodes?.[String(series.next_season_number)] ?? []);
  for (let episode = 1; episode < series.next_episode_number; episode += 1) {
    if (!watchedEpisodes.has(episode)) return false;
  }

  return true;
}

export function deriveSeriesStatus(
  watchedSeasons: number[],
  totalSeasons: number | null,
  hasUnreleasedEpisodes = false,
  watchedEpisodes: Record<string, number[]> = {}
): 'watched' | 'watching' | 'want_to_watch' {
  const hasAnyEpisodeWatched =
    watchedSeasons.length > 0 || Object.values(watchedEpisodes).some(eps => eps.length > 0);
  if (!hasAnyEpisodeWatched) return 'want_to_watch';
  if (!hasUnreleasedEpisodes && totalSeasons && watchedSeasons.length >= totalSeasons) return 'watched';
  return 'watching';
}

export function getEffectiveSeriesStatus(series: Series): 'watched' | 'watching' | 'want_to_watch' {
  return deriveSeriesStatus(
    series.watched_seasons ?? [],
    series.seasons,
    false,
    series.watched_episodes ?? {}
  );
}

/**
 * Détermine si une série est en attente de nouveaux épisodes/saisons.
 * Une série est "en attente" si l'utilisateur a regardé toutes les saisons
 * disponibles et qu'une nouvelle saison/un nouvel épisode est attendu.
 */
export function isSeriesWaiting(s: Series): boolean {
  const effectiveStatus = getEffectiveSeriesStatus(s);
  if (effectiveStatus === 'want_to_watch') return false;
  if (!isFutureAirDate(s.next_air_date) || s.next_season_number === null) return false;
  if (effectiveStatus === 'watched') return true;

  if (s.next_episode_number !== null && s.next_episode_number > 1) {
    return hasWatchedAllPreviousEpisodesInNextSeason(s);
  }

  const nextSeasonKey = String(s.next_season_number);
  const hasStartedNextSeason =
    (s.watched_episodes?.[nextSeasonKey]?.length ?? 0) > 0 ||
    s.watched_seasons.includes(s.next_season_number);
  if (hasStartedNextSeason) return false;

  const releasedSeasonCount = getReleasedSeasonCount(s);
  if (releasedSeasonCount !== null && s.watched_seasons.length < releasedSeasonCount) return false;

  return s.next_season_number > s.watched_seasons.length;
}

/**
 * Formate le label du badge "En attente" avec la date si disponible.
 * - Pas de date : "En attente"
 * - Demain      : "demain"
 * - < 60 jours  : "dans Xj"
 * - >= 60 jours : "15 avr." / "avr. 2026"
 * - Date passée : "En attente" (la saison a probablement commencé)
 */
export function formatWaitingLabel(
  nextAirDate: string | null,
  fallback: string,
  tomorrowLabel: string,
  inDaysLabel: (days: number) => string,
  locale: string
): string {
  if (!nextAirDate) return fallback;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const date = new Date(nextAirDate);
  date.setHours(0, 0, 0, 0);
  const days = Math.round((date.getTime() - today.getTime()) / 86_400_000);
  if (days <= 0) return fallback;
  if (days === 1) return tomorrowLabel;
  if (days < 60) return inDaysLabel(days);
  // Même année → "15 avr.", sinon → "avr. 2026"
  if (date.getFullYear() === today.getFullYear()) {
    return date.toLocaleDateString(locale, { day: 'numeric', month: 'short' });
  }
  return date.toLocaleDateString(locale, { month: 'short', year: 'numeric' });
}
