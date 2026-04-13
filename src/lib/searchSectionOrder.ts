import { useEffect, useState } from 'react';
import type { MediaMode } from '../types';

export const DEFAULT_SEARCH_SECTION_ORDER = {
  books: ['want_to_read', 'last_read'],
  movies: ['trending', 'want_to_watch', 'last_watched'],
  series: ['trending', 'watching', 'waiting', 'want_to_watch', 'last_watched'],
} as const;

export type SearchSectionId =
  | (typeof DEFAULT_SEARCH_SECTION_ORDER.books)[number]
  | (typeof DEFAULT_SEARCH_SECTION_ORDER.movies)[number]
  | (typeof DEFAULT_SEARCH_SECTION_ORDER.series)[number];

export interface SearchSectionPreference {
  id: string;
  visible: boolean;
}

const SEARCH_SECTION_EVENT = 'nookmind:search-sections-changed';

function getStorageKey(mode: MediaMode) {
  return `nookmind_search_section_prefs_${mode}`;
}

function sanitizePreferences(value: unknown, fallback: readonly string[]): SearchSectionPreference[] {
  const valid = new Set(fallback);

  if (!Array.isArray(value)) {
    return fallback.map(id => ({ id, visible: true }));
  }

  const normalized = value.flatMap(item => {
    if (typeof item === 'string') {
      return valid.has(item) ? [{ id: item, visible: true }] : [];
    }

    if (
      item &&
      typeof item === 'object' &&
      'id' in item &&
      typeof item.id === 'string' &&
      valid.has(item.id)
    ) {
      return [{
        id: item.id,
        visible: item.visible !== false,
      }];
    }

    return [];
  });

  const deduped: SearchSectionPreference[] = [];
  for (const item of normalized) {
    if (!deduped.some(existing => existing.id === item.id)) deduped.push(item);
  }

  const missing = fallback
    .filter(id => !deduped.some(item => item.id === id))
    .map(id => ({ id, visible: true }));

  return [...deduped, ...missing];
}

export function getStoredSearchSectionPreferences(mode: MediaMode): SearchSectionPreference[] {
  if (typeof window === 'undefined') {
    return sanitizePreferences(null, DEFAULT_SEARCH_SECTION_ORDER[mode]);
  }

  try {
    const raw = localStorage.getItem(getStorageKey(mode));
    return sanitizePreferences(raw ? JSON.parse(raw) : null, DEFAULT_SEARCH_SECTION_ORDER[mode]);
  } catch {
    return sanitizePreferences(null, DEFAULT_SEARCH_SECTION_ORDER[mode]);
  }
}

export function setStoredSearchSectionPreferences(mode: MediaMode, prefs: SearchSectionPreference[]) {
  if (typeof window === 'undefined') return;
  const sanitized = sanitizePreferences(prefs, DEFAULT_SEARCH_SECTION_ORDER[mode]);
  localStorage.setItem(getStorageKey(mode), JSON.stringify(sanitized));
  window.dispatchEvent(new CustomEvent(SEARCH_SECTION_EVENT, { detail: { mode, prefs: sanitized } }));
}

function moveItem<T>(items: T[], fromIndex: number, toIndex: number): T[] {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 || toIndex >= items.length) return items;
  const next = [...items];
  const [item] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, item);
  return next;
}

export function useSearchSectionOrder(mode: MediaMode) {
  const [sections, setSections] = useState<SearchSectionPreference[]>(() => getStoredSearchSectionPreferences(mode));

  useEffect(() => {
    setSections(getStoredSearchSectionPreferences(mode));
  }, [mode]);

  useEffect(() => {
    const syncSections = () => {
      setSections(getStoredSearchSectionPreferences(mode));
    };

    const handleStorage = (event: StorageEvent) => {
      if (event.key && event.key !== getStorageKey(mode)) return;
      syncSections();
    };

    const handleCustomEvent = (event: Event) => {
      const detail = (event as CustomEvent<{ mode?: MediaMode }>).detail;
      if (detail?.mode && detail.mode !== mode) return;
      syncSections();
    };

    window.addEventListener('storage', handleStorage);
    window.addEventListener(SEARCH_SECTION_EVENT, handleCustomEvent as EventListener);

    return () => {
      window.removeEventListener('storage', handleStorage);
      window.removeEventListener(SEARCH_SECTION_EVENT, handleCustomEvent as EventListener);
    };
  }, [mode]);

  const updateSections = (next: SearchSectionPreference[]) => {
    const sanitized = sanitizePreferences(next, DEFAULT_SEARCH_SECTION_ORDER[mode]);
    setSections(sanitized);
    setStoredSearchSectionPreferences(mode, sanitized);
  };

  const moveSection = (sectionId: string, toIndex: number) => {
    const fromIndex = sections.findIndex(section => section.id === sectionId);
    if (fromIndex === -1) return;
    updateSections(moveItem(sections, fromIndex, toIndex));
  };

  const toggleSectionVisibility = (sectionId: string) => {
    updateSections(sections.map(section =>
      section.id === sectionId ? { ...section, visible: !section.visible } : section
    ));
  };

  const resetSections = () => {
    updateSections(sanitizePreferences(null, DEFAULT_SEARCH_SECTION_ORDER[mode]));
  };

  return {
    sections,
    moveSection,
    toggleSectionVisibility,
    resetSections,
  };
}

export function orderSearchSections<T extends { id: string }>(
  sections: T[],
  prefs: readonly SearchSectionPreference[]
): T[] {
  const rank = new Map(prefs.map((item, index) => [item.id, index]));

  return [...sections]
    .sort((a, b) => (rank.get(a.id) ?? Number.MAX_SAFE_INTEGER) - (rank.get(b.id) ?? Number.MAX_SAFE_INTEGER));
}
