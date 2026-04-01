import type { ReactNode } from 'react';
import type { BookCategory } from '../types';
import { createCollectionContext } from './createCollectionContext';

interface CategoryRow {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  book_category_items: { book_id: string }[];
}

interface CategoriesContextValue {
  categories: BookCategory[];
  loading: boolean;
  createCategory: (title: string) => Promise<BookCategory | null>;
  deleteCategory: (id: string) => Promise<void>;
  addBooksToCategory: (categoryId: string, bookIds: string[]) => Promise<void>;
  removeBookFromCategory: (categoryId: string, bookId: string) => Promise<void>;
}

const { Provider: BaseCategoriesProvider, useItems: useCategoriesBase } = createCollectionContext<BookCategory, CategoryRow>({
  contextName: 'useCategories',
  collectionTable: 'book_categories',
  joinTable: 'book_category_items',
  relationSelect: '*, book_category_items(book_id)',
  mappedIdKey: 'book_id',
  selectErrorMessage: 'Failed to fetch categories:',
  createErrorMessage: 'Failed to create category',
  deleteErrorMessage: 'Failed to delete category',
  addErrorMessage: 'Failed to add books to category',
  removeErrorMessage: 'Failed to remove book from category',
  toCollection: (row) => ({
    id: row.id,
    user_id: row.user_id,
    title: row.title,
    created_at: row.created_at,
    book_ids: row.book_category_items.map((item) => item.book_id),
  }),
  emptyCollection: (row) => ({
    id: row.id,
    user_id: row.user_id,
    title: row.title,
    created_at: row.created_at,
    book_ids: [],
  }),
  getMappedIds: (item) => item.book_ids,
  setMappedIds: (item, ids) => ({ ...item, book_ids: ids }),
});

export function CategoriesProvider({ children }: { children: ReactNode }) {
  return <BaseCategoriesProvider>{children}</BaseCategoriesProvider>;
}

export function useCategories(): CategoriesContextValue {
  const { items, loading, createItem, deleteItem, addMappedItems, removeMappedItem } = useCategoriesBase();

  return {
    categories: items,
    loading,
    createCategory: createItem,
    deleteCategory: deleteItem,
    addBooksToCategory: addMappedItems,
    removeBookFromCategory: removeMappedItem,
  };
}
