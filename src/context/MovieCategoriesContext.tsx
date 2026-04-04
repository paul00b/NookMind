import type { ReactNode } from 'react';
import type { MovieCategory } from '../types';
import { createCollectionContext } from './createCollectionContext';

interface MovieCategoryRow {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  movie_category_items: { movie_id: string }[];
}

interface MovieCategoriesContextValue {
  movieCategories: MovieCategory[];
  loading: boolean;
  createMovieCategory: (title: string) => Promise<MovieCategory | null>;
  deleteMovieCategory: (id: string) => Promise<void>;
  addMoviesToCategory: (categoryId: string, movieIds: string[]) => Promise<void>;
  removeMovieFromCategory: (categoryId: string, movieId: string) => Promise<void>;
}

const { Provider: BaseMovieCategoriesProvider, useItems: useMovieCategoriesBase } = createCollectionContext<MovieCategory, MovieCategoryRow>({
  contextName: 'useMovieCategories',
  collectionTable: 'movie_categories',
  joinTable: 'movie_category_items',
  relationSelect: '*, movie_category_items(movie_id)',
  mappedIdKey: 'movie_id',
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
    movie_ids: row.movie_category_items.map((item) => item.movie_id),
  }),
  emptyCollection: (row) => ({
    id: row.id,
    user_id: row.user_id,
    title: row.title,
    created_at: row.created_at,
    movie_ids: [],
  }),
  getMappedIds: (item) => item.movie_ids,
  setMappedIds: (item, ids) => ({ ...item, movie_ids: ids }),
});

export function MovieCategoriesProvider({ children }: { children: ReactNode }) {
  return <BaseMovieCategoriesProvider>{children}</BaseMovieCategoriesProvider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useMovieCategories(): MovieCategoriesContextValue {
  const { items, loading, createItem, deleteItem, addMappedItems, removeMappedItem } = useMovieCategoriesBase();

  return {
    movieCategories: items,
    loading,
    createMovieCategory: createItem,
    deleteMovieCategory: deleteItem,
    addMoviesToCategory: addMappedItems,
    removeMovieFromCategory: removeMappedItem,
  };
}
