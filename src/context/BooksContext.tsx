import type { ReactNode } from 'react';
import type { Book } from '../types';
import { createLibraryContext } from './createLibraryContext';

interface BooksContextValue {
  books: Book[];
  loading: boolean;
  addBook: (book: Omit<Book, 'id' | 'user_id' | 'created_at'>) => Promise<Book | null>;
  updateBook: (id: string, updates: Partial<Book>) => Promise<void>;
  deleteBook: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

const { Provider: BaseBooksProvider, useItems: useBooksBase } = createLibraryContext<Book>({
  contextName: 'useBooks',
  table: 'books',
  toastKeys: {
    fetchError: 'toast.books.fetchError',
    addError: 'toast.books.addError',
    updateError: 'toast.books.updateError',
    deleteError: 'toast.books.deleteError',
    addSuccess: 'toast.books.added',
    deleteSuccess: 'toast.books.deleted',
  },
});

export function BooksProvider({ children }: { children: ReactNode }) {
  return <BaseBooksProvider>{children}</BaseBooksProvider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useBooks(): BooksContextValue {
  const { items, loading, addItem, updateItem, deleteItem, refetch } = useBooksBase();

  return {
    books: items,
    loading,
    addBook: addItem,
    updateBook: updateItem,
    deleteBook: deleteItem,
    refetch,
  };
}
