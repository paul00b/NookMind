import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import { addMappedIds, removeItemById, removeMappedId } from './helpers';

type CollectionWithId = { id: string; user_id: string; title: string; created_at: string };

interface CollectionContextValue<TCollection extends CollectionWithId> {
  items: TCollection[];
  loading: boolean;
  createItem: (title: string) => Promise<TCollection | null>;
  deleteItem: (id: string) => Promise<void>;
  addMappedItems: (collectionId: string, itemIds: string[]) => Promise<void>;
  removeMappedItem: (collectionId: string, itemId: string) => Promise<void>;
}

interface CreateCollectionContextOptions<
  TCollection extends CollectionWithId,
  TRow extends { id: string; user_id: string; title: string; created_at: string }
> {
  contextName: string;
  collectionTable: string;
  joinTable: string;
  relationSelect: string;
  mappedIdKey: string;
  selectErrorMessage: string;
  createErrorMessage: string;
  deleteErrorMessage: string;
  addErrorMessage: string;
  removeErrorMessage: string;
  toCollection: (row: TRow) => TCollection;
  emptyCollection: (row: TRow) => TCollection;
  getMappedIds: (item: TCollection) => string[];
  setMappedIds: (item: TCollection, ids: string[]) => TCollection;
}

export function createCollectionContext<
  TCollection extends CollectionWithId,
  TRow extends { id: string; user_id: string; title: string; created_at: string }
>({
  contextName,
  collectionTable,
  joinTable,
  relationSelect,
  mappedIdKey,
  selectErrorMessage,
  createErrorMessage,
  deleteErrorMessage,
  addErrorMessage,
  removeErrorMessage,
  toCollection,
  emptyCollection,
  getMappedIds,
  setMappedIds,
}: CreateCollectionContextOptions<TCollection, TRow>) {
  const Context = createContext<CollectionContextValue<TCollection> | null>(null);

  function Provider({ children }: { children: React.ReactNode }) {
    const { user } = useAuth();
    const [items, setItems] = useState<TCollection[]>([]);
    const [loading, setLoading] = useState(false);

    const fetchItems = useCallback(async () => {
      if (!user) {
        setItems([]);
        return;
      }

      setLoading(true);
      try {
        const { data, error } = await supabase
          .from(collectionTable)
          .select(relationSelect)
          .eq('user_id', user.id)
          .order('created_at', { ascending: true });

        if (error) {
          throw error;
        }

        setItems(((data ?? []) as unknown as TRow[]).map(toCollection));
      } catch (error) {
        console.error(selectErrorMessage, error);
      } finally {
        setLoading(false);
      }
    }, [user]);

    useEffect(() => {
      void fetchItems();
    }, [fetchItems]);

    const createItem = async (title: string): Promise<TCollection | null> => {
      if (!user) {
        return null;
      }

      try {
        const { data, error } = await supabase
          .from(collectionTable)
          .insert({ title: title.trim(), user_id: user.id })
          .select()
          .single();

        if (error) {
          throw error;
        }

        const nextItem = emptyCollection(data as TRow);
        setItems((current) => [...current, nextItem]);
        return nextItem;
      } catch {
        toast.error(createErrorMessage);
        return null;
      }
    };

    const deleteItem = async (id: string) => {
      try {
        const { error } = await supabase.from(collectionTable).delete().eq('id', id);

        if (error) {
          throw error;
        }

        setItems((current) => removeItemById(current, id));
      } catch {
        toast.error(deleteErrorMessage);
      }
    };

    const addMappedItems = async (collectionId: string, itemIds: string[]) => {
      if (!itemIds.length) {
        return;
      }

      try {
        const rows = itemIds.map((itemId) => ({ category_id: collectionId, [mappedIdKey]: itemId }));
        const { error } = await supabase
          .from(joinTable)
          .upsert(rows, { onConflict: `category_id,${mappedIdKey}` });

        if (error) {
          throw error;
        }

        setItems((current) =>
          addMappedIds(
            current,
            collectionId,
            (item) => item.id,
            setMappedIds,
            itemIds,
            getMappedIds,
          ),
        );
      } catch {
        toast.error(addErrorMessage);
      }
    };

    const removeMappedItem = async (collectionId: string, itemId: string) => {
      try {
        const { error } = await supabase
          .from(joinTable)
          .delete()
          .eq('category_id', collectionId)
          .eq(mappedIdKey, itemId);

        if (error) {
          throw error;
        }

        setItems((current) =>
          removeMappedId(
            current,
            collectionId,
            (item) => item.id,
            setMappedIds,
            itemId,
            getMappedIds,
          ),
        );
      } catch {
        toast.error(removeErrorMessage);
      }
    };

    return (
      <Context.Provider value={{ items, loading, createItem, deleteItem, addMappedItems, removeMappedItem }}>
        {children}
      </Context.Provider>
    );
  }

  function useItems() {
    const context = useContext(Context);

    if (!context) {
      throw new Error(`${contextName} must be used within its provider`);
    }

    return context;
  }

  return { Provider, useItems };
}
