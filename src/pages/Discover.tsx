import { useEffect, useState, useCallback } from 'react';
import { BookOpen, RefreshCw, Plus } from 'lucide-react';
import { useBooks } from '../context/BooksContext';
import { fetchByGenre, fetchByAuthor, extractBookData } from '../lib/googleBooks';
import type { GoogleBookVolume } from '../types';
import AddBookModal from '../components/AddBookModal';

interface Section {
  title: string;
  reason: string;
  books: GoogleBookVolume[];
}

function BookSuggestionCard({ volume, onAdd }: { volume: GoogleBookVolume; onAdd: (v: GoogleBookVolume) => void }) {
  const info = volume.volumeInfo;
  const thumb = info.imageLinks?.thumbnail || info.imageLinks?.smallThumbnail;
  const cover = thumb ? thumb.replace('http://', 'https://') : null;

  return (
    <div className="flex-shrink-0 w-32 group">
      <div className="relative w-full aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2">
        {cover ? (
          <img src={cover} alt={info.title} className="w-full h-full object-cover" loading="lazy" />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <BookOpen size={24} className="text-gray-300 dark:text-gray-600" />
          </div>
        )}
        {/* Add button overlay */}
        <button
          onClick={() => onAdd(volume)}
          className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-200"
        >
          <div className="w-9 h-9 bg-amber-500 rounded-full flex items-center justify-center shadow-lg">
            <Plus size={18} className="text-white" />
          </div>
        </button>
      </div>
      <p className="text-xs font-medium text-gray-800 dark:text-gray-200 line-clamp-2 leading-tight mb-0.5">
        {info.title}
      </p>
      <p className="text-[10px] text-gray-500 dark:text-gray-400 truncate">
        {info.authors?.[0] || 'Unknown'}
      </p>
    </div>
  );
}

function SectionRow({ section, onAdd }: { section: Section; onAdd: (v: GoogleBookVolume) => void }) {
  return (
    <div className="mb-8">
      <div className="mb-3">
        <h2 className="font-serif text-lg font-semibold text-gray-900 dark:text-gray-100">{section.title}</h2>
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{section.reason}</p>
      </div>
      <div
        className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory"
        style={{ scrollbarWidth: 'none' }}
      >
        {section.books.map(vol => (
          <div key={vol.id} className="snap-start">
            <BookSuggestionCard volume={vol} onAdd={onAdd} />
          </div>
        ))}
      </div>
    </div>
  );
}

function SkeletonSection() {
  return (
    <div className="mb-8">
      <div className="h-5 bg-gray-200 dark:bg-gray-700 rounded-full w-40 mb-1 animate-pulse" />
      <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-56 mb-4 animate-pulse" />
      <div className="flex gap-3 overflow-hidden">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex-shrink-0 w-32 animate-pulse">
            <div className="w-full aspect-[2/3] bg-gray-200 dark:bg-gray-700 rounded-xl mb-2" />
            <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-4/5 mb-1" />
            <div className="h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full w-3/5" />
          </div>
        ))}
      </div>
    </div>
  );
}

function topItems(arr: (string | null)[], n: number): string[] {
  const counts = new Map<string, number>();
  for (const item of arr) {
    if (!item) continue;
    counts.set(item, (counts.get(item) ?? 0) + 1);
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, n)
    .map(([k]) => k);
}

export default function Discover() {
  const { books } = useBooks();
  const [sections, setSections] = useState<Section[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [prefill, setPrefill] = useState<ReturnType<typeof extractBookData> | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const existingTitles = new Set(books.map(b => b.title.toLowerCase()));
  const existingIds = new Set(books.map(b => b.google_books_id).filter(Boolean));

  const filterNew = useCallback((vols: GoogleBookVolume[]) =>
    vols.filter(v =>
      !existingIds.has(v.id) &&
      !existingTitles.has(v.volumeInfo.title?.toLowerCase() ?? '')
    ).slice(0, 8),
  [books]);

  const load = useCallback(async () => {
    if (books.length === 0) return;
    setLoading(true);
    setError('');
    setSections([]);

    const topGenres = topItems(books.map(b => b.genre), 2);
    const topAuthors = topItems(books.map(b => b.author), 2);

    try {
      const results: Section[] = [];

      // Genre sections
      for (const genre of topGenres) {
        const vols = await fetchByGenre(genre);
        const filtered = filterNew(vols);
        if (filtered.length > 0) {
          results.push({
            title: `More ${genre}`,
            reason: `Because you enjoy ${genre}`,
            books: filtered,
          });
        }
      }

      // Author sections
      for (const author of topAuthors) {
        const vols = await fetchByAuthor(author);
        const filtered = filterNew(vols);
        if (filtered.length > 0) {
          results.push({
            title: `More from ${author}`,
            reason: `Other books by an author in your library`,
            books: filtered,
          });
        }
      }

      setSections(results);
    } catch {
      setError('Failed to load recommendations. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [books, filterNew]);

  useEffect(() => { load(); }, [load]);

  const handleAdd = (vol: GoogleBookVolume) => {
    setPrefill(extractBookData(vol));
    setModalOpen(true);
  };

  const hasBooks = books.length > 0;

  return (
    <div className="p-4 md:p-8 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-1">Discover</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">Books picked from your reading taste</p>
        </div>
        {hasBooks && !loading && (
          <button onClick={load} className="btn-ghost flex items-center gap-1.5 text-sm mt-1">
            <RefreshCw size={14} /> Refresh
          </button>
        )}
      </div>

      {/* Empty library state */}
      {!hasBooks && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-16 h-16 bg-amber-500/10 rounded-full flex items-center justify-center mb-4">
            <BookOpen size={30} className="text-amber-500" />
          </div>
          <h2 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
            Your library is empty
          </h2>
          <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs">
            Add a few books to your library first — Discover will suggest new reads based on your taste.
          </p>
        </div>
      )}

      {/* Loading skeletons */}
      {loading && (
        <>
          <SkeletonSection />
          <SkeletonSection />
          <SkeletonSection />
        </>
      )}

      {/* Error */}
      {error && (
        <div className="card p-6 text-center">
          <p className="text-red-500 text-sm mb-3">{error}</p>
          <button onClick={load} className="btn-primary text-sm">Try again</button>
        </div>
      )}

      {/* Sections */}
      {!loading && sections.map(section => (
        <SectionRow key={section.title} section={section} onAdd={handleAdd} />
      ))}

      {/* No results but has books */}
      {!loading && !error && hasBooks && sections.length === 0 && (
        <div className="card p-8 text-center">
          <p className="text-gray-500 dark:text-gray-400 text-sm mb-3">
            No new suggestions found right now.
          </p>
          <button onClick={load} className="btn-ghost text-sm flex items-center gap-1.5 mx-auto">
            <RefreshCw size={14} /> Try again
          </button>
        </div>
      )}

      {/* Add book modal */}
      {modalOpen && (
        <AddBookModal
          prefill={prefill ?? undefined}
          onClose={() => { setModalOpen(false); setPrefill(null); }}
        />
      )}
    </div>
  );
}
