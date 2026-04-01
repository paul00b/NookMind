import { describe, expect, it } from 'vitest';
import {
  addMappedIds,
  mergeUniqueIds,
  prependItem,
  removeItemById,
  removeMappedId,
  replaceItemById,
} from './helpers';

interface TestItem {
  id: string;
  relatedIds: string[];
  label: string;
}

describe('context helpers', () => {
  it('prepends a new item without mutating the current array', () => {
    const current = [{ id: '1', relatedIds: [], label: 'first' }];
    const next = { id: '2', relatedIds: [], label: 'second' };

    expect(prependItem(current, next)).toEqual([next, current[0]]);
    expect(current).toEqual([{ id: '1', relatedIds: [], label: 'first' }]);
  });

  it('replaces an item by id', () => {
    const current = [
      { id: '1', relatedIds: [], label: 'first' },
      { id: '2', relatedIds: [], label: 'second' },
    ];
    const replacement = { id: '2', relatedIds: ['3'], label: 'updated' };

    expect(replaceItemById(current, replacement)).toEqual([
      current[0],
      replacement,
    ]);
  });

  it('removes an item by id', () => {
    const current = [
      { id: '1', relatedIds: [], label: 'first' },
      { id: '2', relatedIds: [], label: 'second' },
    ];

    expect(removeItemById(current, '1')).toEqual([current[1]]);
  });

  it('merges ids without duplicates', () => {
    expect(mergeUniqueIds(['1', '2'], ['2', '3'])).toEqual(['1', '2', '3']);
  });

  it('adds mapped ids only on the targeted item', () => {
    const current: TestItem[] = [
      { id: 'a', relatedIds: ['1'], label: 'alpha' },
      { id: 'b', relatedIds: ['2'], label: 'beta' },
    ];

    expect(
      addMappedIds(
        current,
        'a',
        (item) => item.id,
        (item, ids) => ({ ...item, relatedIds: ids }),
        ['1', '3'],
        (item) => item.relatedIds,
      ),
    ).toEqual([
      { id: 'a', relatedIds: ['1', '3'], label: 'alpha' },
      current[1],
    ]);
  });

  it('removes a mapped id only on the targeted item', () => {
    const current: TestItem[] = [
      { id: 'a', relatedIds: ['1', '3'], label: 'alpha' },
      { id: 'b', relatedIds: ['2'], label: 'beta' },
    ];

    expect(
      removeMappedId(
        current,
        'a',
        (item) => item.id,
        (item, ids) => ({ ...item, relatedIds: ids }),
        '3',
        (item) => item.relatedIds,
      ),
    ).toEqual([
      { id: 'a', relatedIds: ['1'], label: 'alpha' },
      current[1],
    ]);
  });
});
