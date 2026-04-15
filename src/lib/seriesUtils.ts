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

/**
 * Détermine si une série est en attente de nouveaux épisodes/saisons.
 * Une série est "en attente" si l'utilisateur a regardé toutes les saisons
 * disponibles et qu'une nouvelle saison/un nouvel épisode est attendu.
 */
export function isSeriesWaiting(s: Series): boolean {
  if (s.status === 'watched') return s.next_season_number !== null;
  if (s.status !== 'watching') return false;
  if (s.next_season_number !== null) {
    const nextSeasonKey = String(s.next_season_number);
    const nextSeasonEpisodes = s.watched_episodes?.[nextSeasonKey] ?? [];
    const hasStartedNextSeason = nextSeasonEpisodes.length > 0;
    const hasCompletedPreviousSeasons = s.watched_seasons.length >= s.next_season_number - 1;
    if (!hasCompletedPreviousSeasons) return false;

    if (!hasStartedNextSeason) return true;

    // Season already started: if the next known air date is in the future,
    // the user is caught up and waiting for the next episode.
    if (isFutureAirDate(s.next_air_date)) return true;

    return false;
  }
  return s.seasons !== null && s.watched_seasons.length > 0 && s.watched_seasons.length >= s.seasons - 1;
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
