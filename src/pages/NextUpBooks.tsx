import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BookOpen, Check } from 'lucide-react';
import { useBooks } from '../context/BooksContext';
import type { Book } from '../types';
import toast from 'react-hot-toast';

export default function NextUpBooks() {
  const { books, updateBook } = useBooks();
  const { t } = useTranslation();

  const reading = books.filter(b => b.status === 'reading');
  const nextToRead = books.filter(b => b.status === 'want_to_read')[0] ?? null;

  if (reading.length === 0 && !nextToRead) {
    return (
      <div className="p-4 md:p-8 max-w-2xl mx-auto flex flex-col items-center justify-center min-h-64 text-center">
        <BookOpen size={40} className="text-gray-300 dark:text-gray-600 mb-4" />
        <p className="text-gray-500 dark:text-gray-400 text-sm">{t('nextUp.noReading')}</p>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-8 max-w-2xl mx-auto">
      <h1 className="font-serif text-2xl font-bold text-amber-600 dark:text-amber-400 mb-6">
        {t('discover.title')}
      </h1>
      <div className="space-y-3">
        {reading.map(book => (
          <ReadingCard key={book.id} book={book} updateBook={updateBook} t={t} />
        ))}
        {reading.length === 0 && nextToRead && (
          <NextToReadCard book={nextToRead} updateBook={updateBook} t={t} />
        )}
      </div>
    </div>
  );
}

function ReadingCard({
  book, updateBook, t,
}: {
  book: Book;
  updateBook: (id: string, updates: Partial<Book>) => Promise<void>;
  t: (key: string, opts?: Record<string, unknown>) => string;
}) {
  const [editingPage, setEditingPage] = useState(false);
  const [pageInput, setPageInput] = useState(String(book.current_page ?? ''));
  const progress = book.current_page != null && book.page_count
    ? Math.min(100, Math.round((book.current_page / book.page_count) * 100))
    : null;

  const savePage = async () => {
    const page = pageInput ? parseInt(pageInput) : null;
    await updateBook(book.id, { current_page: page });
    setEditingPage(false);
  };

  const markRead = async () => {
    await updateBook(book.id, { status: 'read', current_page: null });
    toast.success(t('nextUp.markAsRead'));
  };

  return (
    <div className="card p-4 flex gap-4 items-start">
      <div className="w-12 aspect-[2/3] rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
        {book.cover_url
          ? <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" />
          : <div className="w-full h-full flex items-center justify-center"><BookOpen size={20} className="text-gray-400" /></div>
        }
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">{book.title}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400 truncate">{book.author}</p>

        {/* Progress bar */}
        {progress != null && (
          <div className="mt-2 h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
            <div className="h-full bg-amber-500 rounded-full transition-all" style={{ width: `${progress}%` }} />
          </div>
        )}

        {/* Page counter */}
        <div className="mt-1.5 flex items-center gap-2">
          {editingPage ? (
            <>
              <input
                type="number"
                min="1"
                className="input w-20 text-sm py-1"
                value={pageInput}
                onChange={e => setPageInput(e.target.value)}
                autoFocus
                onBlur={savePage}
                onKeyDown={e => { if (e.key === 'Enter') savePage(); if (e.key === 'Escape') setEditingPage(false); }}
              />
              {book.page_count && <span className="text-xs text-gray-400">/ {book.page_count}</span>}
            </>
          ) : (
            <button
              onClick={() => setEditingPage(true)}
              className="text-xs text-gray-500 dark:text-gray-400 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
            >
              {book.current_page != null
                ? book.page_count
                  ? t('nextUp.pageOf', { current: book.current_page, total: book.page_count })
                  : t('nextUp.pageOnly', { current: book.current_page })
                : t('nextUp.updatePage')}
            </button>
          )}
        </div>

        {/* Mark as read */}
        <button
          onClick={markRead}
          className="mt-2 inline-flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400 hover:underline"
        >
          <Check size={12} /> {t('nextUp.markAsRead')}
        </button>
      </div>
    </div>
  );
}

function NextToReadCard({
  book, updateBook, t,
}: {
  book: Book;
  updateBook: (id: string, updates: Partial<Book>) => Promise<void>;
  t: (key: string, opts?: Record<string, unknown>) => string;
}) {
  const startReading = () => updateBook(book.id, { status: 'reading' });

  return (
    <div className="card p-4 flex gap-4 items-start border-dashed opacity-80">
      <div className="w-12 aspect-[2/3] rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
        {book.cover_url
          ? <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" />
          : <div className="w-full h-full flex items-center justify-center"><BookOpen size={20} className="text-gray-400" /></div>
        }
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-gray-400 mb-0.5">{t('nextUp.nextToRead')}</p>
        <p className="font-semibold text-gray-900 dark:text-gray-100 truncate">{book.title}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400 truncate">{book.author}</p>
        <button
          onClick={startReading}
          className="mt-2 btn-primary text-xs py-1.5 px-3"
        >
          {t('nextUp.startReading')}
        </button>
      </div>
    </div>
  );
}
