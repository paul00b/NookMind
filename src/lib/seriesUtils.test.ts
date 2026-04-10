import { describe, expect, it } from 'vitest';
import type { Series } from '../types';
import { isSeriesWaiting } from './seriesUtils';

function makeSeries(overrides: Partial<Series>): Series {
  return {
    id: 'series-1',
    user_id: 'user-1',
    tmdb_id: 101,
    title: 'Test Show',
    creator: 'Creator',
    description: null,
    poster_url: null,
    first_air_date: '2024-01-01',
    seasons: 3,
    watched_seasons: [],
    watched_episodes: {},
    genre: null,
    status: 'watching',
    rating: null,
    personal_note: null,
    next_air_date: '2026-04-15',
    next_season_number: 2,
    created_at: '2026-04-01T00:00:00.000Z',
    ...overrides,
  };
}

describe('isSeriesWaiting', () => {
  it('returns true when the next season has not started yet', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [1],
      watched_episodes: {},
      next_season_number: 2,
    }))).toBe(true);
  });

  it('returns false when the current airing season has already started', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [1],
      watched_episodes: { '2': [1, 2] },
      next_season_number: 2,
    }))).toBe(false);
  });
});
