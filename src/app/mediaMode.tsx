import type { ReactNode } from 'react';
import type { MediaMode } from '../types';

export interface MediaModeOptions<T> {
  books: T;
  movies: T;
  series: T;
}

export function pickByMediaMode<T>(mode: MediaMode, options: MediaModeOptions<T>) {
  return options[mode];
}

export function renderByMediaMode(mode: MediaMode, options: MediaModeOptions<ReactNode>) {
  return pickByMediaMode(mode, options);
}
