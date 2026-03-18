import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { Movie } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface MoviesContextValue {
  movies: Movie[];
  loading: boolean;
  addMovie: (movie: Omit<Movie, 'id' | 'user_id' | 'created_at'>) => Promise<Movie | null>;
  updateMovie: (id: string, updates: Partial<Movie>) => Promise<void>;
  deleteMovie: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const MoviesContext = createContext<MoviesContextValue | null>(null);

export function MoviesProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchMovies = useCallback(async () => {
    if (!user) { setMovies([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('movies')
        .select('*')
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });
      if (error) throw error;
      setMovies(data ?? []);
    } catch (err) {
      console.error('Failed to fetch movies:', err);
      toast.error('Failed to load your watchlist');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchMovies(); }, [fetchMovies]);

  const addMovie = async (movieData: Omit<Movie, 'id' | 'user_id' | 'created_at'>) => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('movies')
        .insert({ ...movieData, user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      setMovies(prev => [data, ...prev]);
      toast.success('Movie added to your watchlist!');
      return data;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to add movie';
      toast.error(msg);
      return null;
    }
  };

  const updateMovie = async (id: string, updates: Partial<Movie>) => {
    try {
      const { data, error } = await supabase
        .from('movies')
        .update(updates)
        .eq('id', id)
        .select()
        .single();
      if (error) throw error;
      setMovies(prev => prev.map(m => m.id === id ? data : m));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update movie';
      toast.error(msg);
    }
  };

  const deleteMovie = async (id: string) => {
    try {
      const { error } = await supabase.from('movies').delete().eq('id', id);
      if (error) throw error;
      setMovies(prev => prev.filter(m => m.id !== id));
      toast.success('Movie removed from watchlist');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete movie';
      toast.error(msg);
    }
  };

  return (
    <MoviesContext.Provider value={{ movies, loading, addMovie, updateMovie, deleteMovie, refetch: fetchMovies }}>
      {children}
    </MoviesContext.Provider>
  );
}

export function useMovies() {
  const ctx = useContext(MoviesContext);
  if (!ctx) throw new Error('useMovies must be used within MoviesProvider');
  return ctx;
}
