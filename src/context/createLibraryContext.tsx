import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { supabase } from '../lib/supabase';
import { useAuth } from './AuthContext';
import { prependItem, removeItemById, replaceItemById } from './helpers';

type EntityWithId = { id: string; user_id: string; created_at: string };

interface CreateLibraryContextOptions {
  contextName: string;
  table: string;
  toastKeys: {
    fetchError: string;
    addError: string;
    updateError: string;
    deleteError: string;
    addSuccess: string;
    deleteSuccess: string;
  };
}

interface LibraryContextValue<TEntity extends EntityWithId> {
  items: TEntity[];
  loading: boolean;
  addItem: (item: Omit<TEntity, 'id' | 'user_id' | 'created_at'>) => Promise<TEntity | null>;
  updateItem: (id: string, updates: Partial<TEntity>) => Promise<void>;
  deleteItem: (id: string) => Promise<void>;
  refetch: () => Promise<void>;
}

export function createLibraryContext<TEntity extends EntityWithId>({
  contextName,
  table,
  toastKeys,
}: CreateLibraryContextOptions) {
  const Context = createContext<LibraryContextValue<TEntity> | null>(null);

  function Provider({ children }: { children: React.ReactNode }) {
    const { user } = useAuth();
    const { t } = useTranslation();
    const [items, setItems] = useState<TEntity[]>([]);
    const [loading, setLoading] = useState(false);

    const fetchItems = useCallback(async () => {
      if (!user) {
        setItems([]);
        return;
      }

      setLoading(true);
      try {
        const { data, error } = await supabase
          .from(table)
          .select('*')
          .eq('user_id', user.id)
          .order('created_at', { ascending: false });

        if (error) {
          throw error;
        }

        setItems((data ?? []) as TEntity[]);
      } catch (error) {
        console.error(toastKeys.fetchError, error);
        toast.error(t(toastKeys.fetchError));
      } finally {
        setLoading(false);
      }
    }, [user]);

    useEffect(() => {
      void fetchItems();
    }, [fetchItems]);

    const addItem = async (itemData: Omit<TEntity, 'id' | 'user_id' | 'created_at'>) => {
      if (!user) {
        return null;
      }

      try {
        const { data, error } = await supabase
          .from(table)
          .insert({ ...itemData, user_id: user.id })
          .select()
          .single();

        if (error) {
          throw error;
        }

        const nextItem = data as TEntity;
        setItems((current) => prependItem(current, nextItem));
        toast.success(t(toastKeys.addSuccess));
        return nextItem;
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : t(toastKeys.addError);
        toast.error(message);
        return null;
      }
    };

    const updateItem = async (id: string, updates: Partial<TEntity>) => {
      try {
        const { data, error } = await supabase
          .from(table)
          .update(updates)
          .eq('id', id)
          .select()
          .single();

        if (error) {
          throw error;
        }

        setItems((current) => replaceItemById(current, data as TEntity));
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : t(toastKeys.updateError);
        toast.error(message);
      }
    };

    const deleteItem = async (id: string) => {
      try {
        const { error } = await supabase.from(table).delete().eq('id', id);

        if (error) {
          throw error;
        }

        setItems((current) => removeItemById(current, id));
        toast.success(t(toastKeys.deleteSuccess));
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : t(toastKeys.deleteError);
        toast.error(message);
      }
    };

    return (
      <Context.Provider value={{ items, loading, addItem, updateItem, deleteItem, refetch: fetchItems }}>
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
