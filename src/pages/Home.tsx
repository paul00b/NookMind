import { useState, useEffect, useRef, useCallback } from 'react';
import { Search, Plus, X, BookOpen, CheckCircle2 } from 'lucide-react';
import { searchBooks, extractBookData } from '../lib/googleBooks';
import type { GoogleBookVolume, Book } from '../types';
import AddBookModal from '../components/AddBookModal';
import BookDetailModal from '../components/BookDetailModal';
import StarRating from '../components/StarRating';
import { useBooks } from '../context/BooksContext';
import { useTranslation } from 'react-i18next';

function useDebounce<T>(value: T, delay: number): T {
  const [dv, setDv] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDv(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return dv;
}

function WantToReadSlider({ onSelect }: { onSelect: (book: Book) => void }) {
  const { books } = useBooks();
  const { t } = useTranslation();

  const wantToRead = books
    .filter(b => b.status === 'want_to_read')
    .slice(0, 10);

  if (wantToRead.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('home.wantToRead')}
      </h2>
      <div
        className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth"
        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
      >
        {wantToRead.map(book => (
          <div
            key={book.id}
            onClick={() => onSelect(book)}
            className="flex-shrink-0 snap-start group cursor-pointer"
          >
            <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
              {book.cover_url ? (
                <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <BookOpen size={22} className="text-gray-300 dark:text-gray-600" />
                </div>
              )}
            </div>
            <div className="w-20 md:w-28 text-left">
              <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                {book.title}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function LastReadSlider({ onSelect }: { onSelect: (book: Book) => void }) {
  const { books } = useBooks();
  const { t } = useTranslation();
  const sliderRef = useRef<HTMLDivElement>(null);

  const lastRead = books
    .filter(b => b.status === 'read')
    .slice(0, 10);

  if (lastRead.length === 0) return null;

  return (
    <div className="w-full max-w-xl mt-10">
      <h2 className="text-sm font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3 px-1">
        {t('home.lastRead')}
      </h2>
      <div
        ref={sliderRef}
        className="flex gap-3 overflow-x-auto pb-2 snap-x snap-mandatory scroll-smooth"
        style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
      >
        {lastRead.map(book => (
          <div
            key={book.id}
            onClick={() => onSelect(book)}
            className="flex-shrink-0 snap-start group cursor-pointer"
          >
            {/* Cover */}
            <div className="w-20 md:w-28 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 mb-2 group-hover:scale-[1.03] transition-transform duration-200">
              {book.cover_url ? (
                <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" loading="lazy" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <BookOpen size={22} className="text-gray-300 dark:text-gray-600" />
                </div>
              )}
            </div>
            {/* Info */}
            <div className="w-20 md:w-28 text-left">
              <p className="text-xs font-medium text-gray-800 dark:text-gray-200 truncate leading-tight group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
                {book.title}
              </p>
              {book.rating && (
                <div className="mt-0.5">
                  <StarRating value={book.rating} readonly size={10} />
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

export default function Home() {
  const { t } = useTranslation();
  const { books } = useBooks();
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 600);
  const [results, setResults] = useState<GoogleBookVolume[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReturnType<typeof extractBookData> | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const doSearch = useCallback(async (q: string) => {
    if (q.trim().length < 3) { setResults([]); setDropdownOpen(false); return; }
    setSearching(true);
    setSearchError('');
    try {
      const res = await searchBooks(q);
      setResults(res);
      setDropdownOpen(true);
    } catch (err) {
      setSearchError(t('home.searchTimeout'));
      setDropdownOpen(false);
    } finally {
      setSearching(false);
    }
  }, []);

  useEffect(() => { doSearch(debouncedQuery); }, [debouncedQuery, doSearch]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSelectBook = (vol: GoogleBookVolume) => {
    const data = extractBookData(vol);
    setPrefill(data);
    setModalOpen(true);
    setDropdownOpen(false);
    setQuery('');
  };

  const handleAddManually = () => {
    setPrefill(null);
    setModalOpen(true);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4 py-12">
      {/* Heading */}
      <div className="text-center mb-10">
        <h1 className="font-serif text-4xl md:text-5xl font-bold text-gray-900 dark:text-gray-100 mb-3">
          {t('home.title')}
        </h1>
        <p className="text-gray-500 dark:text-gray-400 text-lg max-w-md mx-auto">
          {t('home.subtitle')}
        </p>
      </div>

      {/* Search */}
      <div className="w-full max-w-xl relative" ref={dropdownRef}>
        <div className="relative">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onFocus={() => results.length > 0 && setDropdownOpen(true)}
            placeholder={t('home.searchPlaceholder')}
            className="input rounded-full pl-11 pr-10 py-3.5 text-base shadow-sm"
            autoComplete="off"
          />
          {query && (
            <button
              onClick={() => { setQuery(''); setResults([]); setDropdownOpen(false); inputRef.current?.focus(); }}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            >
              <X size={16} />
            </button>
          )}
        </div>

        {/* Dropdown */}
        {(dropdownOpen || searching) && query && (
          <div className="absolute top-full mt-2 left-0 right-0 card shadow-xl z-20 overflow-y-auto animate-slide-up max-h-[60vh]">
            {searching ? (
              <div className="p-3 space-y-2">
                {[1, 2, 3].map(i => (
                  <div key={i} className="flex gap-3 animate-pulse p-2">
                    <div className="w-10 h-14 bg-gray-200 dark:bg-gray-700 rounded-lg flex-shrink-0" />
                    <div className="flex-1 space-y-2 py-1">
                      <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-3/4" />
                      <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-1/2" />
                    </div>
                  </div>
                ))}
              </div>
            ) : results.length === 0 ? (
              <div className="p-6 text-center text-gray-500 dark:text-gray-400 text-sm">
                {t('home.noBooksFound', { query })}
              </div>
            ) : (
              <ul>
                {results.map((vol, idx) => {
                  const info = vol.volumeInfo;
                  const thumb = info.imageLinks?.smallThumbnail;
                  const firstAuthor = info.authors?.[0] ?? '';
                  const alreadyAdded = books.some(
                    b => normalize(b.title) === normalize(info.title) &&
                         normalize(b.author) === normalize(firstAuthor)
                  );
                  return (
                    <li key={vol.id}>
                      {idx > 0 && <div className="border-t border-black/[0.06] dark:border-white/[0.06] mx-3" />}
                      <button
                        onClick={() => handleSelectBook(vol)}
                        className="w-full flex items-center gap-3 px-4 py-3 hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors text-left"
                      >
                        <div className="w-10 h-14 rounded-lg overflow-hidden flex-shrink-0 bg-gray-100 dark:bg-gray-800">
                          {thumb ? (
                            <img src={thumb.replace('http://', 'https://')} alt="" className="w-full h-full object-cover" />
                          ) : (
                            <div className="w-full h-full bg-gray-200 dark:bg-gray-700" />
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-sm text-gray-900 dark:text-gray-100 truncate">{info.title}</p>
                          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">
                            {info.authors?.join(', ') || t('home.unknownAuthor')}
                            {info.publishedDate && ` · ${info.publishedDate.slice(0, 4)}`}
                          </p>
                        </div>
                        {alreadyAdded && (
                          <span className="flex-shrink-0 flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded-full">
                            <CheckCircle2 size={12} />
                            {t('addBook.inLibrary')}
                          </span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* Error */}
      {searchError && (
        <p className="mt-3 text-sm text-red-500">{searchError}</p>
      )}

      {/* Add manually */}
      <div className="mt-6 flex items-center gap-3">
        <div className="w-16 h-px bg-gray-200 dark:bg-gray-700" />
        <span className="text-sm text-gray-400 dark:text-gray-500">{t('home.or')}</span>
        <div className="w-16 h-px bg-gray-200 dark:bg-gray-700" />
      </div>
      <button onClick={handleAddManually} className="mt-4 btn-ghost flex items-center gap-2 text-sm">
        <Plus size={16} /> {t('home.addManually')}
      </button>

      {/* Want to read slider */}
      <WantToReadSlider onSelect={setSelectedBook} />

      {/* Last read slider */}
      <LastReadSlider onSelect={setSelectedBook} />

      {/* Add book modal */}
      {modalOpen && (
        <AddBookModal
          prefill={prefill ?? undefined}
          onClose={() => { setModalOpen(false); setPrefill(null); }}
        />
      )}

      {/* Book detail modal */}
      {selectedBook && (
        <BookDetailModal
          book={selectedBook}
          onClose={() => setSelectedBook(null)}
        />
      )}

    </div>
  );
}
