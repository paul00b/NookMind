import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { BookCategory } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface CategoriesContextValue {
  categories: BookCategory[];
  loading: boolean;
  createCategory: (title: string) => Promise<BookCategory | null>;
  deleteCategory: (id: string) => Promise<void>;
  addBooksToCategory: (categoryId: string, bookIds: string[]) => Promise<void>;
  removeBookFromCategory: (categoryId: string, bookId: string) => Promise<void>;
}

const CategoriesContext = createContext<CategoriesContextValue | null>(null);

export function CategoriesProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [categories, setCategories] = useState<BookCategory[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCategories = useCallback(async () => {
    if (!user) { setCategories([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('book_categories')
        .select('*, book_category_items(book_id)')
        .eq('user_id', user.id)
        .order('created_at', { ascending: true });
      if (error) throw error;
      setCategories(
        (data ?? []).map(c => ({
          id: c.id,
          user_id: c.user_id,
          title: c.title,
          created_at: c.created_at,
          book_ids: (c.book_category_items as { book_id: string }[]).map(i => i.book_id),
        }))
      );
    } catch (err) {
      console.error('Failed to fetch categories:', err);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const createCategory = async (title: string): Promise<BookCategory | null> => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('book_categories')
        .insert({ title: title.trim(), user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      const cat: BookCategory = { ...data, book_ids: [] };
      setCategories(prev => [...prev, cat]);
      return cat;
    } catch {
      toast.error('Failed to create category');
      return null;
    }
  };

  const deleteCategory = async (id: string) => {
    try {
      const { error } = await supabase.from('book_categories').delete().eq('id', id);
      if (error) throw error;
      setCategories(prev => prev.filter(c => c.id !== id));
    } catch {
      toast.error('Failed to delete category');
    }
  };

  const addBooksToCategory = async (categoryId: string, bookIds: string[]) => {
    if (!bookIds.length) return;
    try {
      const rows = bookIds.map(book_id => ({ category_id: categoryId, book_id }));
      const { error } = await supabase
        .from('book_category_items')
        .upsert(rows, { onConflict: 'category_id,book_id' });
      if (error) throw error;
      setCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, book_ids: [...new Set([...c.book_ids, ...bookIds])] }
          : c
      ));
    } catch {
      toast.error('Failed to add books to category');
    }
  };

  const removeBookFromCategory = async (categoryId: string, bookId: string) => {
    try {
      const { error } = await supabase
        .from('book_category_items')
        .delete()
        .eq('category_id', categoryId)
        .eq('book_id', bookId);
      if (error) throw error;
      setCategories(prev => prev.map(c =>
        c.id === categoryId
          ? { ...c, book_ids: c.book_ids.filter(id => id !== bookId) }
          : c
      ));
    } catch {
      toast.error('Failed to remove book from category');
    }
  };

  return (
    <CategoriesContext.Provider value={{ categories, loading, createCategory, deleteCategory, addBooksToCategory, removeBookFromCategory }}>
      {children}
    </CategoriesContext.Provider>
  );
}

export function useCategories() {
  const ctx = useContext(CategoriesContext);
  if (!ctx) throw new Error('useCategories must be used within CategoriesProvider');
  return ctx;
}
