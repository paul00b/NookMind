import type { ReactNode } from 'react';
import type { Series } from '../types';
import { createLibraryContext } from './createLibraryContext';

interface SeriesContextValue {
  series: Series[];
  loading: boolean;
  addSeries: (series: Omit<Series, 'id' | 'user_id' | 'created_at'>) => Promise<Series | null>;
  updateSeries: (id: string, updates: Partial<Series>) => Promise<void>;
  deleteSeries: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const { Provider: BaseSeriesProvider, useItems: useSeriesBase } = createLibraryContext<Series>({
  contextName: 'useSeries',
  table: 'series',
  selectMessage: 'Failed to fetch series:',
  insertMessage: 'Failed to add series',
  updateMessage: 'Failed to update series',
  deleteMessage: 'Failed to delete series',
  deleteSuccessMessage: 'Series removed',
  addSuccessMessage: 'Series added!',
});

export function SeriesProvider({ children }: { children: ReactNode }) {
  return <BaseSeriesProvider>{children}</BaseSeriesProvider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSeries(): SeriesContextValue {
  const { items, loading, addItem, updateItem, deleteItem, refetch } = useSeriesBase();

  return {
    series: items,
    loading,
    addSeries: addItem,
    updateSeries: updateItem,
    deleteSeries: deleteItem,
    refetch,
  };
}
