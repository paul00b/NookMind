export type BookStatus = 'read' | 'want_to_read' | 'reading';

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
  current_page: number | null;
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
  watched_date: string | null;
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
    cast?: { id: number; name: string; character?: string; profile_path?: string | null }[];
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
  watched_episodes: Record<string, number[]>;
  genre: string | null;
  status: SeriesStatus;
  rating: number | null;
  personal_note: string | null;
  next_air_date: string | null;
  next_season_number: number | null;
  created_at: string;
}

export interface TmdbEpisode {
  air_date: string | null;
  episode_number: number;
  season_number: number;
  name: string;
  overview?: string;
  runtime?: number | null;
  still_path?: string | null;
  vote_average?: number;
}

export interface TmdbSeasonDetails {
  episodes: TmdbEpisode[];
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
  next_episode_to_air?: TmdbEpisode | null;
  last_episode_to_air?: TmdbEpisode | null;
  seasons?: { season_number: number; episode_count: number; air_date: string | null }[];
  credits?: {
    cast?: { id: number; name: string; character?: string; profile_path?: string | null }[];
  };
}

export interface SeriesCategory {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  series_ids: string[];
}

export interface WatchProvider {
  provider_id: number;
  provider_name: string;
  logo_path: string;
}

export interface WatchProvidersResult {
  flatrate: WatchProvider[];
  link: string | null;
  deepLinks?: Record<string, string>;
}
