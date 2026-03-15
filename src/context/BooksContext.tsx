import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import type { Book } from '../types';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import toast from 'react-hot-toast';

interface BooksContextValue {
  books: Book[];
  loading: boolean;
  addBook: (book: Omit<Book, 'id' | 'user_id' | 'created_at'>) => Promise<Book | null>;
  updateBook: (id: string, updates: Partial<Book>) => Promise<void>;
  deleteBook: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const BooksContext = createContext<BooksContextValue | null>(null);

export function BooksProvider({ children }: { children: React.ReactNode }) {
  const { user } = useAuth();
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchBooks = useCallback(async () => {
    if (!user) { setBooks([]); return; }
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('books')
        .select('*')
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });
      if (error) throw error;
      setBooks(data ?? []);
    } catch (err) {
      console.error('Failed to fetch books:', err);
      toast.error('Failed to load your library');
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { fetchBooks(); }, [fetchBooks]);

  const addBook = async (bookData: Omit<Book, 'id' | 'user_id' | 'created_at'>) => {
    if (!user) return null;
    try {
      const { data, error } = await supabase
        .from('books')
        .insert({ ...bookData, user_id: user.id })
        .select()
        .single();
      if (error) throw error;
      setBooks(prev => [data, ...prev]);
      toast.success('Book added to your library!');
      return data;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to add book';
      toast.error(msg);
      return null;
    }
  };

  const updateBook = async (id: string, updates: Partial<Book>) => {
    try {
      const { data, error } = await supabase
        .from('books')
        .update(updates)
        .eq('id', id)
        .select()
        .single();
      if (error) throw error;
      setBooks(prev => prev.map(b => b.id === id ? data : b));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update book';
      toast.error(msg);
    }
  };

  const deleteBook = async (id: string) => {
    try {
      const { error } = await supabase.from('books').delete().eq('id', id);
      if (error) throw error;
      setBooks(prev => prev.filter(b => b.id !== id));
      toast.success('Book removed from library');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete book';
      toast.error(msg);
    }
  };

  return (
    <BooksContext.Provider value={{ books, loading, addBook, updateBook, deleteBook, refetch: fetchBooks }}>
      {children}
    </BooksContext.Provider>
  );
}

export function useBooks() {
  const ctx = useContext(BooksContext);
  if (!ctx) throw new Error('useBooks must be used within BooksProvider');
  return ctx;
}
