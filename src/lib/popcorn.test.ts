import { describe, expect, it } from 'vitest';
import {
  buildEpisodePopcornQuery,
  buildMoviePopcornQuery,
  buildPopcornLauncherUrl,
  buildSeriesPopcornQuery,
} from './popcorn';

describe('popcorn helpers', () => {
  it('builds a movie query with the release year when available', () => {
    expect(buildMoviePopcornQuery('Inception', '2010-07-16')).toBe('Inception 2010');
  });

  it('builds a plain series query', () => {
    expect(buildSeriesPopcornQuery('Breaking Bad')).toBe('Breaking Bad');
  });

  it('builds an episode query with padded season and episode numbers', () => {
    expect(buildEpisodePopcornQuery('Breaking Bad', 2, 3, '2009-03-29')).toBe('Breaking Bad S02E03 2009');
  });

  it('fills launcher template placeholders', () => {
    const url = buildPopcornLauncherUrl(
      'popcorntime://search?query={{query}}&kind={{kind}}&season={{season}}&episode={{episode}}',
      'Breaking Bad S02E03 2009',
      { kind: 'episode', title: 'Breaking Bad', season: 2, episode: 3, year: '2009' }
    );

    expect(url).toBe(
      'popcorntime://search?query=Breaking%20Bad%20S02E03%202009&kind=episode&season=2&episode=3'
    );
  });

  it('omits missing tokens cleanly in launcher templates', () => {
    const url = buildPopcornLauncherUrl(
      'popcorntime://search?query={{query}}&year={{year}}',
      'Breaking Bad',
      { kind: 'series', title: 'Breaking Bad' }
    );

    expect(url).toBe('popcorntime://search?query=Breaking%20Bad&year=');
  });
});
