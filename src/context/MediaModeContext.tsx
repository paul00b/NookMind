import React, { createContext, useContext, useState } from 'react';
import type { MediaMode } from '../types';

interface MediaModeContextValue {
  mode: MediaMode;
  setMode: (mode: MediaMode) => void;
}

const MediaModeContext = createContext<MediaModeContextValue | null>(null);

export function MediaModeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<MediaMode>(() => {
    const stored = localStorage.getItem('media-mode');
    return stored === 'movies' ? 'movies' : stored === 'series' ? 'series' : 'books';
  });

  const setMode = (m: MediaMode) => {
    setModeState(m);
    localStorage.setItem('media-mode', m);
  };

  return (
    <MediaModeContext.Provider value={{ mode, setMode }}>
      {children}
    </MediaModeContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useMediaMode() {
  const ctx = useContext(MediaModeContext);
  if (!ctx) throw new Error('useMediaMode must be used within MediaModeProvider');
  return ctx;
}
