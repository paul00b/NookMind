import type { EpisodeRating } from './imdb';

export type SeasonState = EpisodeRating[] | 'loading' | 'error';

export function getRatingStyle(rating: number | null): { background: string; color: string } {
  if (rating === null) return { background: '#374151', color: '#6b7280' };
  if (rating >= 9) return { background: '#16a34a', color: '#ffffff' };
  if (rating >= 8) return { background: '#4ade80', color: '#14532d' };
  if (rating >= 7) return { background: '#facc15', color: '#713f12' };
  if (rating >= 6) return { background: '#f97316', color: '#ffffff' };
  if (rating >= 5) return { background: '#ef4444', color: '#ffffff' };
  return { background: '#7f1d1d', color: '#fca5a5' };
}
