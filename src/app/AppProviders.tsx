import type { ReactNode } from 'react';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { MediaModeProvider } from '../context/MediaModeContext';
import { BooksProvider } from '../context/BooksContext';
import { CategoriesProvider } from '../context/CategoriesContext';
import { MoviesProvider } from '../context/MoviesContext';
import { MovieCategoriesProvider } from '../context/MovieCategoriesContext';
import { SeriesProvider } from '../context/SeriesContext';
import { SeriesCategoriesProvider } from '../context/SeriesCategoriesContext';

const appProviders = [
  ThemeProvider,
  AuthProvider,
  MediaModeProvider,
  BooksProvider,
  CategoriesProvider,
  MoviesProvider,
  MovieCategoriesProvider,
  SeriesProvider,
  SeriesCategoriesProvider,
] as const;

export default function AppProviders({ children }: { children: ReactNode }) {
  return appProviders.reduceRight((tree, Provider) => <Provider>{tree}</Provider>, children);
}
