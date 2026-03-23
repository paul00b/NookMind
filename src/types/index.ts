export type BookStatus = 'read' | 'want_to_read';

export interface Book {
  id: string;
  user_id: string;
  google_books_id: string | null;
  title: string;
  author: string;
  description: string | null;
  cover_url: string | null;
  published_date: string | null;
  page_count: number | null;
  genre: string | null;
  status: BookStatus;
  rating: number | null;
  personal_note: string | null;
  created_at: string;
}

export interface GoogleBookVolume {
  id: string;
  volumeInfo: {
    title: string;
    authors?: string[];
    description?: string;
    imageLinks?: {
      thumbnail?: string;
      smallThumbnail?: string;
    };
    publishedDate?: string;
    pageCount?: number;
    categories?: string[];
    industryIdentifiers?: Array<{ type: string; identifier: string }>;
  };
}

export type ThemeMode = 'light' | 'dark' | 'system';

export type MediaMode = 'books' | 'movies' | 'series';

export type MovieStatus = 'watched' | 'want_to_watch';

export interface Movie {
  id: string;
  user_id: string;
  tmdb_id: number | null;
  title: string;
  director: string;
  description: string | null;
  poster_url: string | null;
  release_date: string | null;
  runtime: number | null;
  genre: string | null;
  status: MovieStatus;
  rating: number | null;
  personal_note: string | null;
  created_at: string;
}

export interface TmdbMovie {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  release_date: string;
  runtime?: number;
  genres?: { id: number; name: string }[];
  credits?: {
    crew: { job: string; name: string }[];
  };
}

export interface MovieCategory {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  movie_ids: string[];
}

export interface BookCategory {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  book_ids: string[];
}

export type SeriesStatus = 'watched' | 'want_to_watch' | 'watching';

export interface Series {
  id: string;
  user_id: string;
  tmdb_id: number | null;
  title: string;
  creator: string;
  description: string | null;
  poster_url: string | null;
  first_air_date: string | null;
  seasons: number | null;
  watched_seasons: number[];
  genre: string | null;
  status: SeriesStatus;
  rating: number | null;
  personal_note: string | null;
  created_at: string;
}

export interface TmdbSeries {
  id: number;
  name: string;
  overview: string;
  poster_path: string | null;
  first_air_date: string;
  number_of_seasons?: number;
  genres?: { id: number; name: string }[];
  created_by?: { name: string }[];
}

export interface SeriesCategory {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  series_ids: string[];
}
