import { describe, expect, it } from 'vitest';
import { deriveSeriesStatus } from './SeasonGrid';

describe('deriveSeriesStatus', () => {
  it('keeps a series in watching when episodes are watched but no season is complete', () => {
    expect(deriveSeriesStatus([], 3, false, { '1': [1] })).toBe('watching');
  });

  it('marks a series as want_to_watch when nothing is watched yet', () => {
    expect(deriveSeriesStatus([], 3, false, {})).toBe('want_to_watch');
  });
});
