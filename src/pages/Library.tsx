import { useState, useMemo } from 'react';
import { useBooks } from '../context/BooksContext';
import type { Book, BookStatus } from '../types';
import BookCard from '../components/BookCard';
import BookDetailModal from '../components/BookDetailModal';
import StarRating from '../components/StarRating';
import { BookOpen, ChevronDown, LayoutGrid, List } from 'lucide-react';
import { useTranslation } from 'react-i18next';

type SortKey = 'created_at' | 'title' | 'author' | 'rating';
type ViewMode = 'grid' | 'list';

function EmptyState({ status }: { status: BookStatus }) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center px-4">
      <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
        <BookOpen size={36} className="text-amber-500" />
      </div>
      <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
        {status === 'read' ? t('library.noBooksRead') : t('library.emptyReadingList')}
      </h3>
      <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs">
        {status === 'read' ? t('library.noBooksReadDesc') : t('library.emptyReadingListDesc')}
      </p>
    </div>
  );
}

function Select({ value, onChange, options }: { value: string; onChange: (v: string) => void; options: { value: string; label: string }[] }) {
  return (
    <div className="relative flex-shrink-0">
      <select
        value={value}
        onChange={e => onChange(e.target.value)}
        className="appearance-none input py-2 pr-8 text-sm cursor-pointer"
      >
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
    </div>
  );
}

function BookListRow({ book, onClick }: { book: Book; onClick: () => void }) {
  const { t } = useTranslation();
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors text-left group"
    >
      {/* Cover */}
      <div className="w-10 h-14 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
        {book.cover_url ? (
          <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" loading="lazy" />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <BookOpen size={16} className="text-gray-300 dark:text-gray-600" />
          </div>
        )}
      </div>

      {/* Title + author */}
      <div className="flex-1 min-w-0">
        <p className="font-serif font-semibold text-sm text-gray-900 dark:text-gray-100 truncate group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
          {book.title}
        </p>
        <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{book.author}</p>
      </div>

      {/* Genre */}
      {book.genre && (
        <span className="hidden sm:inline-flex flex-shrink-0 text-xs bg-amber-500/10 text-amber-700 dark:text-amber-400 px-2.5 py-1 rounded-full font-medium">
          {book.genre}
        </span>
      )}

      {/* Rating */}
      {book.status === 'read' && book.rating && (
        <div className="hidden sm:flex flex-shrink-0">
          <StarRating value={book.rating} readonly size={12} />
        </div>
      )}

      {/* Status badge */}
      <span className={`flex-shrink-0 text-xs font-medium px-2.5 py-1 rounded-full text-white ${
        book.status === 'read' ? 'bg-emerald-500' : 'bg-amber-500'
      }`}>
        {book.status === 'read' ? t('bookCard.read') : t('bookCard.wantToRead')}
      </span>
    </button>
  );
}

export default function Library() {
  const { t } = useTranslation();
  const { books, loading } = useBooks();
  const [activeTab, setActiveTab] = useState<BookStatus>('read');
  const [genreFilter, setGenreFilter] = useState('');
  const [authorFilter, setAuthorFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('created_at');
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');

  const filtered = useMemo(() => {
    let list = books.filter(b => b.status === activeTab);
    if (genreFilter) list = list.filter(b => b.genre === genreFilter);
    if (authorFilter) list = list.filter(b => b.author === authorFilter);

    list = [...list].sort((a, b) => {
      if (sortKey === 'created_at') return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
      if (sortKey === 'title') return a.title.localeCompare(b.title);
      if (sortKey === 'author') return a.author.localeCompare(b.author);
      if (sortKey === 'rating') return (b.rating ?? 0) - (a.rating ?? 0);
      return 0;
    });
    return list;
  }, [books, activeTab, genreFilter, authorFilter, sortKey]);

  const tabBooks = books.filter(b => b.status === activeTab);
  const genres = [...new Set(tabBooks.map(b => b.genre).filter(Boolean))] as string[];
  const authors = [...new Set(tabBooks.map(b => b.author).filter(Boolean))] as string[];

  const counts = {
    read: books.filter(b => b.status === 'read').length,
    want_to_read: books.filter(b => b.status === 'want_to_read').length,
  };

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-1">{t('library.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">{t('library.booksCount', { count: books.length })}</p>
        </div>

        {/* View toggle */}
        <div className="flex items-center bg-gray-100 dark:bg-gray-800 rounded-xl p-1 gap-0.5">
          <button
            onClick={() => setViewMode('grid')}
            className={`p-2 rounded-lg transition-all ${
              viewMode === 'grid'
                ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
                : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
            }`}
          >
            <LayoutGrid size={16} />
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-lg transition-all ${
              viewMode === 'list'
                ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
                : 'text-gray-400 hover:text-gray-600 dark:hover:text-gray-300'
            }`}
          >
            <List size={16} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-black/[0.06] dark:border-white/[0.06] pb-0">
        {([['read', t('library.read')], ['want_to_read', t('library.wantToRead')]] as const).map(([status, label]) => (
          <button
            key={status}
            onClick={() => { setActiveTab(status); setGenreFilter(''); setAuthorFilter(''); }}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-all -mb-px ${
              activeTab === status
                ? 'border-amber-500 text-amber-600 dark:text-amber-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
            }`}
          >
            {label}
            <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
              activeTab === status ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'
            }`}>
              {counts[status]}
            </span>
          </button>
        ))}
      </div>

      {/* Filters */}
      {tabBooks.length > 0 && (
        <div className="mb-6 flex items-center gap-2 overflow-x-auto pb-1" style={{ scrollbarWidth: 'none' }}>
          <Select
            value={genreFilter}
            onChange={setGenreFilter}
            options={[{ value: '', label: t('library.allGenres') }, ...genres.map(g => ({ value: g, label: g }))]}
          />
          <Select
            value={authorFilter}
            onChange={setAuthorFilter}
            options={[{ value: '', label: t('library.allAuthors') }, ...authors.map(a => ({ value: a, label: a }))]}
          />
          <div className="flex items-center flex-shrink-0">
            <Select
              value={sortKey}
              onChange={v => setSortKey(v as SortKey)}
              options={[
                { value: 'created_at', label: t('library.dateAdded') },
                { value: 'title', label: t('library.titleAZ') },
                { value: 'author', label: t('library.authorAZ') },
                ...(activeTab === 'read' ? [{ value: 'rating', label: t('library.ratingDesc') }] : []),
              ]}
            />
          </div>
        </div>
      )}

      {/* Content */}
      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="card overflow-hidden flex flex-col animate-pulse">
              <div className="w-full aspect-[2/3] bg-gray-200 dark:bg-gray-700 rounded-xl mb-3" />
              <div className="px-1 pb-1 space-y-2">
                <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-4/5" />
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-3/5" />
              </div>
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState status={activeTab} />
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
          {filtered.map(book => (
            <BookCard key={book.id} book={book} onClick={() => setSelectedBook(book)} />
          ))}
        </div>
      ) : (
        <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
          {filtered.map(book => (
            <BookListRow key={book.id} book={book} onClick={() => setSelectedBook(book)} />
          ))}
        </div>
      )}

      {/* Detail modal */}
      {selectedBook && (
        <BookDetailModal
          book={selectedBook}
          onClose={() => setSelectedBook(null)}
        />
      )}
    </div>
  );
}
