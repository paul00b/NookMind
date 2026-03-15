import type { GoogleBookVolume } from '../types';

const BASE_URL = 'https://www.googleapis.com/books/v1/volumes';

export async function searchBooks(query: string): Promise<GoogleBookVolume[]> {
  if (!query.trim()) return [];

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 8000);

  try {
    const url = `${BASE_URL}?q=${encodeURIComponent(query)}&maxResults=8&printType=books`;
    const res = await fetch(url, { signal: controller.signal });
    if (!res.ok) throw new Error('Failed to fetch books');
    const data = await res.json();
    return (data.items as GoogleBookVolume[]) ?? [];
  } finally {
    clearTimeout(timeoutId);
  }
}

export function extractBookData(volume: GoogleBookVolume) {
  const info = volume.volumeInfo;
  const thumbnail = info.imageLinks?.thumbnail || info.imageLinks?.smallThumbnail || null;
  // Use HTTPS and higher-res thumbnail
  const cover_url = thumbnail ? thumbnail.replace('http://', 'https://').replace('zoom=1', 'zoom=2') : null;

  return {
    google_books_id: volume.id,
    title: info.title || 'Unknown Title',
    author: info.authors?.join(', ') || 'Unknown Author',
    description: info.description || null,
    cover_url,
    published_date: info.publishedDate || null,
    page_count: info.pageCount || null,
    genre: info.categories?.[0] || null,
  };
}
