import { describe, expect, it } from 'vitest';
import type { Series } from '../types';
import { getEffectiveSeriesStatus, isSeriesWaiting } from './seriesUtils';

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
    next_episode_number: 1,
    created_at: '2026-04-01T00:00:00.000Z',
    ...overrides,
  };
}

describe('isSeriesWaiting', () => {
  it('derives watching when episodes are watched even if the stored status says want_to_watch', () => {
    expect(getEffectiveSeriesStatus(makeSeries({
      status: 'want_to_watch',
      seasons: 1,
      watched_seasons: [],
      watched_episodes: { '1': [1, 2, 3, 4] },
    }))).toBe('watching');
  });

  it('returns true when the next season has not started yet', () => {
    expect(isSeriesWaiting(makeSeries({
      seasons: 1,
      watched_seasons: [1],
      watched_episodes: {},
      next_air_date: '2099-04-22',
      next_season_number: 2,
    }))).toBe(true);
  });

  it('returns true when TMDB already counts a future season in the total', () => {
    expect(isSeriesWaiting(makeSeries({
      seasons: 3,
      watched_seasons: [1, 2],
      watched_episodes: {},
      next_air_date: '2026-07-02',
      next_season_number: 3,
    }))).toBe(true);
  });

  it('returns false when the next season has already started', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [1],
      seasons: 2,
      watched_episodes: {},
      next_air_date: '2099-04-22',
      next_season_number: 2,
      next_episode_number: 4,
    }))).toBe(false);
  });

  it('returns true when the user is caught up in the current season and the next episode is in the future', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [1, 2, 3, 4],
      seasons: 5,
      watched_episodes: { '5': [1, 2, 3, 4] },
      next_air_date: '2026-04-29',
      next_season_number: 5,
      next_episode_number: 5,
    }))).toBe(true);
  });

  it('returns false when a new season is already airing even if no episode has been marked yet', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [1],
      seasons: 2,
      watched_episodes: { '2': [1, 2] },
      next_air_date: '2099-04-22',
      next_season_number: 2,
    }))).toBe(false);
  });

  it('returns false when the series is still being watched within the current season', () => {
    expect(isSeriesWaiting(makeSeries({
      watched_seasons: [],
      watched_episodes: { '1': [1, 2] },
      next_air_date: '2026-04-15',
      next_season_number: 1,
      next_episode_number: 4,
    }))).toBe(false);
  });

  it('returns true for watched series when a future season is announced', () => {
    expect(isSeriesWaiting(makeSeries({
      status: 'watched',
      watched_seasons: [1, 2, 3],
      seasons: 3,
      next_air_date: '2099-04-22',
      next_season_number: 4,
    }))).toBe(true);
  });
});
