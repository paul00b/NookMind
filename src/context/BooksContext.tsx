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
  selectMessage: 'Failed to fetch books:',
  insertMessage: 'Failed to add book',
  updateMessage: 'Failed to update book',
  deleteMessage: 'Failed to delete book',
  deleteSuccessMessage: 'Book removed from library',
  addSuccessMessage: 'Book added to your library!',
});

export function BooksProvider({ children }: { children: ReactNode }) {
  return <BaseBooksProvider>{children}</BaseBooksProvider>;
}

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
