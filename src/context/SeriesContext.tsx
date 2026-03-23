import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { Series } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface SeriesContextValue {
  series: Series[];
  loading: boolean;
  addSeries: (series: Omit<Series, 'id' | 'user_id' | 'created_at'>) => Promise<Series | null>;
  updateSeries: (id: string, updates: Partial<Series>) => Promise<void>;
  deleteSeries: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const SeriesContext = createContext<SeriesContextValue | null>(null);

export function SeriesProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [series, setSeries] = useState<Series[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchSeries = useCallback(async () => {
    if (!user) { setSeries([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('series')
        .select('*')
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });
      if (error) throw error;
      setSeries(data ?? []);
    } catch (err) {
      console.error('Failed to fetch series:', err);
      toast.error('Failed to load your series');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchSeries(); }, [fetchSeries]);

  const addSeries = async (seriesData: Omit<Series, 'id' | 'user_id' | 'created_at'>) => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('series')
        .insert({ ...seriesData, user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      setSeries(prev => [data, ...prev]);
      toast.success('Series added!');
      return data;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to add series';
      toast.error(msg);
      return null;
    }
  };

  const updateSeries = async (id: string, updates: Partial<Series>) => {
    try {
      const { data, error } = await supabase
        .from('series')
        .update(updates)
        .eq('id', id)
        .select()
        .single();
      if (error) throw error;
      setSeries(prev => prev.map(s => s.id === id ? data : s));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update series';
      toast.error(msg);
    }
  };

  const deleteSeries = async (id: string) => {
    try {
      const { error } = await supabase.from('series').delete().eq('id', id);
      if (error) throw error;
      setSeries(prev => prev.filter(s => s.id !== id));
      toast.success('Series removed');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete series';
      toast.error(msg);
    }
  };

  return (
    <SeriesContext.Provider value={{ series, loading, addSeries, updateSeries, deleteSeries, refetch: fetchSeries }}>
      {children}
    </SeriesContext.Provider>
  );
}

export function useSeries() {
  const ctx = useContext(SeriesContext);
  if (!ctx) throw new Error('useSeries must be used within SeriesProvider');
  return ctx;
}
