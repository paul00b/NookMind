import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { MovieCategory } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface MovieCategoriesContextValue {
  movieCategories: MovieCategory[];
  loading: boolean;
  createMovieCategory: (title: string) => Promise<MovieCategory | null>;
  deleteMovieCategory: (id: string) => Promise<void>;
  addMoviesToCategory: (categoryId: string, movieIds: string[]) => Promise<void>;
  removeMovieFromCategory: (categoryId: string, movieId: string) => Promise<void>;
}

const MovieCategoriesContext = createContext<MovieCategoriesContextValue | null>(null);

export function MovieCategoriesProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [movieCategories, setMovieCategories] = useState<MovieCategory[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCategories = useCallback(async () => {
    if (!user) { setMovieCategories([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('movie_categories')
        .select('*, movie_category_items(movie_id)')
        .eq('user_id', user.id)
        .order('created_at', { ascending: true });
      if (error) throw error;
      setMovieCategories(
        (data ?? []).map(c => ({
          id: c.id,
          user_id: c.user_id,
          title: c.title,
          created_at: c.created_at,
          movie_ids: (c.movie_category_items as { movie_id: string }[]).map(i => i.movie_id),
        }))
      );
    } catch (err) {
      console.error('Failed to fetch movie categories:', err);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const createMovieCategory = async (title: string): Promise<MovieCategory | null> => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('movie_categories')
        .insert({ title: title.trim(), user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      const cat: MovieCategory = { ...data, movie_ids: [] };
      setMovieCategories(prev => [...prev, cat]);
      return cat;
    } catch {
      toast.error('Failed to create collection');
      return null;
    }
  };

  const deleteMovieCategory = async (id: string) => {
    try {
      const { error } = await supabase.from('movie_categories').delete().eq('id', id);
      if (error) throw error;
      setMovieCategories(prev => prev.filter(c => c.id !== id));
    } catch {
      toast.error('Failed to delete collection');
    }
  };

  const addMoviesToCategory = async (categoryId: string, movieIds: string[]) => {
    if (!movieIds.length) return;
    try {
      const rows = movieIds.map(movie_id => ({ category_id: categoryId, movie_id }));
      const { error } = await supabase
        .from('movie_category_items')
        .upsert(rows, { onConflict: 'category_id,movie_id' });
      if (error) throw error;
      setMovieCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, movie_ids: [...new Set([...c.movie_ids, ...movieIds])] }
          : c
      ));
    } catch {
      toast.error('Failed to add movies to collection');
    }
  };

  const removeMovieFromCategory = async (categoryId: string, movieId: string) => {
    try {
      const { error } = await supabase
        .from('movie_category_items')
        .delete()
        .eq('category_id', categoryId)
        .eq('movie_id', movieId);
      if (error) throw error;
      setMovieCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, movie_ids: c.movie_ids.filter(id => id !== movieId) }
          : c
      ));
    } catch {
      toast.error('Failed to remove movie from collection');
    }
  };

  return (
    <MovieCategoriesContext.Provider value={{ movieCategories, loading, createMovieCategory, deleteMovieCategory, addMoviesToCategory, removeMovieFromCategory }}>
      {children}
    </MovieCategoriesContext.Provider>
  );
}

export function useMovieCategories() {
  const ctx = useContext(MovieCategoriesContext);
  if (!ctx) throw new Error('useMovieCategories must be used within MovieCategoriesProvider');
  return ctx;
}
