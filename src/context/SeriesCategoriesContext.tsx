import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { SeriesCategory } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface SeriesCategoriesContextValue {
  seriesCategories: SeriesCategory[];
  loading: boolean;
  createSeriesCategory: (title: string) => Promise<SeriesCategory | null>;
  deleteSeriesCategory: (id: string) => Promise<void>;
  addSeriesToCategory: (categoryId: string, seriesIds: string[]) => Promise<void>;
  removeSeriesFromCategory: (categoryId: string, seriesId: string) => Promise<void>;
}

const SeriesCategoriesContext = createContext<SeriesCategoriesContextValue | null>(null);

export function SeriesCategoriesProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [seriesCategories, setSeriesCategories] = useState<SeriesCategory[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCategories = useCallback(async () => {
    if (!user) { setSeriesCategories([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('series_categories')
        .select('*, series_category_items(series_id)')
        .eq('user_id', user.id)
        .order('created_at', { ascending: true });
      if (error) throw error;
      setSeriesCategories(
        (data ?? []).map(c => ({
          id: c.id,
          user_id: c.user_id,
          title: c.title,
          created_at: c.created_at,
          series_ids: (c.series_category_items as { series_id: string }[]).map(i => i.series_id),
        }))
      );
    } catch (err) {
      console.error('Failed to fetch series categories:', err);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const createSeriesCategory = async (title: string): Promise<SeriesCategory | null> => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('series_categories')
        .insert({ title: title.trim(), user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      const cat: SeriesCategory = { ...data, series_ids: [] };
      setSeriesCategories(prev => [...prev, cat]);
      return cat;
    } catch {
      toast.error('Failed to create collection');
      return null;
    }
  };

  const deleteSeriesCategory = async (id: string) => {
    try {
      const { error } = await supabase.from('series_categories').delete().eq('id', id);
      if (error) throw error;
      setSeriesCategories(prev => prev.filter(c => c.id !== id));
    } catch {
      toast.error('Failed to delete collection');
    }
  };

  const addSeriesToCategory = async (categoryId: string, seriesIds: string[]) => {
    if (!seriesIds.length) return;
    try {
      const rows = seriesIds.map(series_id => ({ category_id: categoryId, series_id }));
      const { error } = await supabase
        .from('series_category_items')
        .upsert(rows, { onConflict: 'category_id,series_id' });
      if (error) throw error;
      setSeriesCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, series_ids: [...new Set([...c.series_ids, ...seriesIds])] }
          : c
      ));
    } catch {
      toast.error('Failed to add series to collection');
    }
  };

  const removeSeriesFromCategory = async (categoryId: string, seriesId: string) => {
    try {
      const { error } = await supabase
        .from('series_category_items')
        .delete()
        .eq('category_id', categoryId)
        .eq('series_id', seriesId);
      if (error) throw error;
      setSeriesCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, series_ids: c.series_ids.filter(id => id !== seriesId) }
          : c
      ));
    } catch {
      toast.error('Failed to remove series from collection');
    }
  };

  return (
    <SeriesCategoriesContext.Provider value={{ seriesCategories, loading, createSeriesCategory, deleteSeriesCategory, addSeriesToCategory, removeSeriesFromCategory }}>
      {children}
    </SeriesCategoriesContext.Provider>
  );
}

export function useSeriesCategories() {
  const ctx = useContext(SeriesCategoriesContext);
  if (!ctx) throw new Error('useSeriesCategories must be used within SeriesCategoriesProvider');
  return ctx;
}
