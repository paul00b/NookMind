import type { ReactNode } from 'react';
import type { SeriesCategory } from '../types';
import { createCollectionContext } from './createCollectionContext';

interface SeriesCategoryRow {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  series_category_items: { series_id: string }[];
}

interface SeriesCategoriesContextValue {
  seriesCategories: SeriesCategory[];
  loading: boolean;
  createSeriesCategory: (title: string) => Promise<SeriesCategory | null>;
  deleteSeriesCategory: (id: string) => Promise<void>;
  addSeriesToCategory: (categoryId: string, seriesIds: string[]) => Promise<void>;
  removeSeriesFromCategory: (categoryId: string, seriesId: string) => Promise<void>;
}

const { Provider: BaseSeriesCategoriesProvider, useItems: useSeriesCategoriesBase } = createCollectionContext<SeriesCategory, SeriesCategoryRow>({
  contextName: 'useSeriesCategories',
  collectionTable: 'series_categories',
  joinTable: 'series_category_items',
  relationSelect: '*, series_category_items(series_id)',
  mappedIdKey: 'series_id',
  toastKeys: {
    fetchError: 'toast.collections.fetchError',
    createError: 'toast.collections.createError',
    deleteError: 'toast.collections.deleteError',
    addError: 'toast.collections.addError',
    removeError: 'toast.collections.removeError',
  },
  toCollection: (row) => ({
    id: row.id,
    user_id: row.user_id,
    title: row.title,
    created_at: row.created_at,
    series_ids: row.series_category_items.map((item) => item.series_id),
  }),
  emptyCollection: (row) => ({
    id: row.id,
    user_id: row.user_id,
    title: row.title,
    created_at: row.created_at,
    series_ids: [],
  }),
  getMappedIds: (item) => item.series_ids,
  setMappedIds: (item, ids) => ({ ...item, series_ids: ids }),
});

export function SeriesCategoriesProvider({ children }: { children: ReactNode }) {
  return <BaseSeriesCategoriesProvider>{children}</BaseSeriesCategoriesProvider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSeriesCategories(): SeriesCategoriesContextValue {
  const { items, loading, createItem, deleteItem, addMappedItems, removeMappedItem } = useSeriesCategoriesBase();

  return {
    seriesCategories: items,
    loading,
    createSeriesCategory: createItem,
    deleteSeriesCategory: deleteItem,
    addSeriesToCategory: addMappedItems,
    removeSeriesFromCategory: removeMappedItem,
  };
}
