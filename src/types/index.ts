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

export interface BookCategory {
  id: string;
  user_id: string;
  title: string;
  created_at: string;
  book_ids: string[];
}
