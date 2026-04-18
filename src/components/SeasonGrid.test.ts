import { describe, expect, it } from 'vitest';
import { deriveSeriesStatus } from '../lib/seriesUtils';

describe('deriveSeriesStatus', () => {
  it('keeps a series in watching when episodes are watched but no season is complete', () => {
    expect(deriveSeriesStatus([], 3, false, { '1': [1] })).toBe('watching');
  });

  it('marks a series as want_to_watch when nothing is watched yet', () => {
    expect(deriveSeriesStatus([], 3, false, {})).toBe('want_to_watch');
  });

  it('marks a partially watched single-season series as watching', () => {
    expect(deriveSeriesStatus([], 1, false, { '1': [1, 2, 3, 4] })).toBe('watching');
  });
});
