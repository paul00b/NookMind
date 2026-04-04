import type { ReactNode } from 'react';
import type { Movie } from '../types';
import { createLibraryContext } from './createLibraryContext';

interface MoviesContextValue {
  movies: Movie[];
  loading: boolean;
  addMovie: (movie: Omit<Movie, 'id' | 'user_id' | 'created_at'>) => Promise<Movie | null>;
  updateMovie: (id: string, updates: Partial<Movie>) => Promise<void>;
  deleteMovie: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const { Provider: BaseMoviesProvider, useItems: useMoviesBase } = createLibraryContext<Movie>({
  contextName: 'useMovies',
  table: 'movies',
  selectMessage: 'Failed to fetch movies:',
  insertMessage: 'Failed to add movie',
  updateMessage: 'Failed to update movie',
  deleteMessage: 'Failed to delete movie',
  deleteSuccessMessage: 'Movie removed from watchlist',
  addSuccessMessage: 'Movie added to your watchlist!',
});

export function MoviesProvider({ children }: { children: ReactNode }) {
  return <BaseMoviesProvider>{children}</BaseMoviesProvider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useMovies(): MoviesContextValue {
  const { items, loading, addItem, updateItem, deleteItem, refetch } = useMoviesBase();

  return {
    movies: items,
    loading,
    addMovie: addItem,
    updateMovie: updateItem,
    deleteMovie: deleteItem,
    refetch,
  };
}
