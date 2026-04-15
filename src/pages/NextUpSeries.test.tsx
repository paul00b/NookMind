import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useState, type ReactNode } from 'react';
import NextUpSeries from './NextUpSeries';
import type { Series, TmdbSeasonDetails, TmdbSeries } from '../types';

let seriesState: Series[] = [];
let updateSeriesImpl: (id: string, updates: Partial<Series>) => Promise<void> = async () => {};

vi.mock('../context/SeriesContext', () => ({
  useSeries: () => ({
    series: seriesState,
    updateSeries: updateSeriesImpl,
  }),
}));

vi.mock('../components/SheetModal', () => ({
  default: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  SheetCloseButton: ({ children }: { children: ReactNode }) => <button type="button">{children}</button>,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: { language: 'fr' },
    t: (key: string, opts?: Record<string, unknown>) => {
      if (key === 'discover.title') return 'A suivre';
      if (key === 'nextUp.noWatching') return 'Aucune serie en cours';
      if (key === 'nextUp.available') return 'Disponible';
      if (key === 'nextUp.upToDate') return 'A jour';
      if (key === 'nextUp.upToDateSection') return 'A jour';
      if (key === 'nextUp.markEpisodeWatched') return 'Marquer comme vu';
      if (key === 'nextUp.seasonEpisode') return `S${opts?.season}E${opts?.episode}`;
      if (key === 'nextUp.today') return `Disponible aujourd'hui ${opts?.date}`;
      if (key === 'nextUp.comingSoon') return `Dans ${opts?.days} jour`;
      if (key === 'nextUp.comingSoonPlural') return `Dans ${opts?.days} jours`;
      if (key === 'nextUp.detailComingSoon') return 'Bientot';
      if (key === 'nextUp.episodeSynopsis') return 'Synopsis';
      if (key === 'nextUp.noEpisodeOverview') return 'Aucun synopsis';
      return key;
    },
  }),
}));

const fetchSeriesDetailsMock = vi.fn<(tmdbId: number) => Promise<TmdbSeries | null>>();
const fetchSeasonDetailsMock = vi.fn<(tmdbId: number, seasonNumber: number) => Promise<TmdbSeasonDetails | null>>();

vi.mock('../lib/tmdb', () => ({
  fetchSeriesDetails: (tmdbId: number) => fetchSeriesDetailsMock(tmdbId),
  fetchSeasonDetails: (tmdbId: number, seasonNumber: number) => fetchSeasonDetailsMock(tmdbId, seasonNumber),
  getPosterUrl: vi.fn(() => null),
}));

function Harness({ initialSeries }: { initialSeries: Series[] }) {
  const [series, setSeries] = useState(initialSeries);
  // eslint-disable-next-line react-hooks/globals
  seriesState = series;
  // eslint-disable-next-line react-hooks/globals
  updateSeriesImpl = async (id, updates) => {
    setSeries(current =>
      current.map(item => (item.id === id ? { ...item, ...updates } : item))
    );
  };

  return <NextUpSeries />;
}

describe('NextUpSeries', () => {
  beforeEach(() => {
    fetchSeriesDetailsMock.mockResolvedValue({
      id: 101,
      name: 'Test Show',
      overview: 'Overview',
      poster_path: null,
      first_air_date: '2024-01-01',
      seasons: [
        { season_number: 1, episode_count: 3, air_date: '2024-01-01' },
      ],
      last_episode_to_air: {
        season_number: 1,
        episode_number: 2,
        air_date: '2024-01-08',
        name: 'Episode 2',
      },
      next_episode_to_air: {
        season_number: 1,
        episode_number: 3,
        air_date: '2026-04-20',
        name: 'Episode 3',
      },
    });
    fetchSeasonDetailsMock.mockResolvedValue(null);
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.clearAllMocks();
    seriesState = [];
    updateSeriesImpl = async () => {};
  });

  it('shows the following upcoming episode after marking the current one as watched', async () => {
    const initialSeries: Series[] = [{
      id: 'series-1',
      user_id: 'user-1',
      tmdb_id: 101,
      title: 'Test Show',
      creator: 'Creator',
      description: null,
      poster_url: null,
      first_air_date: '2024-01-01',
      seasons: 1,
      watched_seasons: [],
      watched_episodes: { '1': [1] },
      genre: null,
      status: 'watching',
      rating: null,
      personal_note: null,
      next_air_date: '2026-04-20',
      next_season_number: 1,
      created_at: '2026-04-01T00:00:00.000Z',
    }];

    render(<Harness initialSeries={initialSeries} />);

    expect(await screen.findByText('S1E2')).toBeTruthy();

    vi.useFakeTimers();
    fetchSeasonDetailsMock.mockResolvedValueOnce({
      episodes: [
        { season_number: 1, episode_number: 1, air_date: '2024-01-01', name: 'Episode 1' },
        { season_number: 1, episode_number: 2, air_date: '2024-01-08', name: 'Episode 2' },
        { season_number: 1, episode_number: 3, air_date: '2026-04-20', name: 'Episode 3' },
      ],
    });
    fireEvent.click(screen.getByRole('button', { name: 'Marquer comme vu' }));

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    expect(screen.getByText('Dans 5 jours')).toBeTruthy();
    expect(screen.getByText(/Episode 3/)).toBeTruthy();
  });

  it('treats an episode airing today as available so it can be marked watched', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-04-15T10:00:00+02:00'));

    fetchSeriesDetailsMock.mockResolvedValueOnce({
      id: 101,
      name: 'Test Show',
      overview: 'Overview',
      poster_path: null,
      first_air_date: '2024-01-01',
      seasons: [
        { season_number: 1, episode_count: 3, air_date: '2024-01-01' },
      ],
      last_episode_to_air: {
        season_number: 1,
        episode_number: 2,
        air_date: '2026-04-08',
        name: 'Episode 2',
      },
      next_episode_to_air: {
        season_number: 1,
        episode_number: 3,
        air_date: '2026-04-15',
        name: 'Episode 3',
      },
    });

    const initialSeries: Series[] = [{
      id: 'series-1',
      user_id: 'user-1',
      tmdb_id: 101,
      title: 'Test Show',
      creator: 'Creator',
      description: null,
      poster_url: null,
      first_air_date: '2024-01-01',
      seasons: 1,
      watched_seasons: [],
      watched_episodes: { '1': [1, 2] },
      genre: null,
      status: 'watching',
      rating: null,
      personal_note: null,
      next_air_date: '2026-04-15',
      next_season_number: 1,
      created_at: '2026-04-01T00:00:00.000Z',
    }];

    render(<Harness initialSeries={initialSeries} />);

    await act(async () => {
      await vi.runAllTimersAsync();
    });

    expect(screen.getByText('Disponible')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Marquer comme vu' })).toBeTruthy();
    expect(screen.queryByText(/Disponible aujourd'hui/)).toBeNull();
  });

  it('shows the following upcoming episode after marking today episode as watched', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-04-15T10:00:00+02:00'));

    fetchSeriesDetailsMock.mockResolvedValueOnce({
      id: 101,
      name: 'Test Show',
      overview: 'Overview',
      poster_path: null,
      first_air_date: '2024-01-01',
      seasons: [
        { season_number: 1, episode_count: 4, air_date: '2024-01-01' },
      ],
      last_episode_to_air: {
        season_number: 1,
        episode_number: 2,
        air_date: '2026-04-08',
        name: 'Episode 2',
      },
      next_episode_to_air: {
        season_number: 1,
        episode_number: 3,
        air_date: '2026-04-15',
        name: 'Episode 3',
      },
    });

    fetchSeasonDetailsMock.mockResolvedValueOnce({
      episodes: [
        { season_number: 1, episode_number: 1, air_date: '2026-04-01', name: 'Episode 1' },
        { season_number: 1, episode_number: 2, air_date: '2026-04-08', name: 'Episode 2' },
        { season_number: 1, episode_number: 3, air_date: '2026-04-15', name: 'Episode 3' },
        { season_number: 1, episode_number: 4, air_date: '2026-04-22', name: 'Episode 4' },
      ],
    });

    const initialSeries: Series[] = [{
      id: 'series-1',
      user_id: 'user-1',
      tmdb_id: 101,
      title: 'Test Show',
      creator: 'Creator',
      description: null,
      poster_url: null,
      first_air_date: '2024-01-01',
      seasons: 1,
      watched_seasons: [],
      watched_episodes: { '1': [1, 2] },
      genre: null,
      status: 'watching',
      rating: null,
      personal_note: null,
      next_air_date: '2026-04-15',
      next_season_number: 1,
      created_at: '2026-04-01T00:00:00.000Z',
    }];

    render(<Harness initialSeries={initialSeries} />);

    await act(async () => {
      await vi.runAllTimersAsync();
    });

    fireEvent.click(screen.getAllByRole('button', { name: 'Marquer comme vu' })[0]);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });

    expect(screen.getByText('Dans 7 jours')).toBeTruthy();
    expect(screen.getByText(/Episode 4/)).toBeTruthy();
  });
});
