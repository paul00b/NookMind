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
