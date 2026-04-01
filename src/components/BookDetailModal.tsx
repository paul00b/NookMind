import { useState, useRef, useEffect } from 'react';
import type { Book, BookStatus } from '../types';
import { useBooks } from '../context/BooksContext';
import { useCategories } from '../context/CategoriesContext';
import StarRating from './StarRating';
import SheetModal from './SheetModal';
import { X, Pencil, Check, Trash2, BookOpen, ChevronDown, ChevronUp, FolderPlus, FolderMinus } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

interface Props {
  book: Book;
  onClose: () => void;
}

export default function BookDetailModal({ book, onClose }: Props) {
  const { updateBook, deleteBook } = useBooks();
  const { categories, addBooksToCategory, removeBookFromCategory } = useCategories();
  const { t } = useTranslation();
  const [editingNote, setEditingNote] = useState(false);
  const [note, setNote] = useState(book.personal_note || '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [localBook, setLocalBook] = useState<Book>(book);
  const [editingPage, setEditingPage] = useState(false);
  const [pageInput, setPageInput] = useState(String(book.current_page ?? ''));
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);
  useEffect(() => {
    if (descRef.current) setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [localBook.description]);

  const handleRatingChange = async (rating: number) => {
    setLocalBook(b => ({ ...b, rating }));
    await updateBook(book.id, { rating });
    toast.success(t('bookDetail.ratingUpdated'));
  };

  const handleSaveNote = async () => {
    setLocalBook(b => ({ ...b, personal_note: note }));
    await updateBook(book.id, { personal_note: note });
    setEditingNote(false);
    toast.success(t('bookDetail.noteSaved'));
  };

  const handleStatusChange = async (newStatus: BookStatus) => {
    const updates: Partial<Book> = { status: newStatus };
    if (newStatus === 'want_to_read') { updates.rating = null; updates.current_page = null; }
    if (newStatus !== 'reading') updates.current_page = null;
    setLocalBook(b => ({ ...b, ...updates }));
    await updateBook(book.id, updates);
    toast.success(
      newStatus === 'read'
        ? t('bookDetail.movedToRead')
        : newStatus === 'reading'
        ? t('bookDetail.movedToReading')
        : t('bookDetail.movedToWantToRead')
    );
  };

  const handleSavePage = async () => {
    const page = pageInput ? parseInt(pageInput) : null;
    setLocalBook(b => ({ ...b, current_page: page }));
    await updateBook(book.id, { current_page: page });
    setEditingPage(false);
  };

  const handleDelete = async () => {
    await deleteBook(book.id);
    onClose();
  };

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none md:max-h-[90vh] overflow-y-auto"
    >
        {/* Close */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 btn-ghost p-2 z-10"
        >
          <X size={20} />
        </button>

        <div className="flex flex-col md:flex-row gap-6 p-6">
          {/* Cover */}
          <div className="flex-shrink-0 mx-auto md:mx-0">
            <div className="w-32 md:w-40 aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
              {localBook.cover_url ? (
                <img src={localBook.cover_url} alt={localBook.title} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center">
                  <BookOpen size={40} className="text-gray-400" />
                </div>
              )}
            </div>
          </div>

          {/* Details */}
          <div className="flex-1 min-w-0 space-y-4">
            {/* Title & author */}
            <div>
              <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 leading-tight mb-1">
                {localBook.title}
              </h2>
              <p className="text-gray-600 dark:text-gray-400 font-medium">{localBook.author}</p>
            </div>

            {/* Meta */}
            <div className="flex flex-wrap gap-2 text-sm">
              {localBook.genre && (
                <span className="bg-amber-500/10 text-amber-700 dark:text-amber-400 px-3 py-1 rounded-full font-medium">
                  {localBook.genre}
                </span>
              )}
              <span className={`px-3 py-1 rounded-full font-medium text-white ${
                localBook.status === 'read' ? 'bg-emerald-500' : localBook.status === 'reading' ? 'bg-blue-500' : 'bg-amber-500'
              }`}>
                {localBook.status === 'read' ? t('bookDetail.read') : localBook.status === 'reading' ? t('library.reading') : t('bookDetail.wantToRead')}
              </span>
              {localBook.published_date && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                  {localBook.published_date.slice(0, 4)}
                </span>
              )}
              {localBook.page_count && (
                <span className="bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 px-3 py-1 rounded-full">
                  {t('bookDetail.pages', { count: localBook.page_count })}
                </span>
              )}
            </div>

            {/* Status selector */}
            <div className="flex gap-2 flex-wrap">
              {(['want_to_read', 'reading', 'read'] as const).map(s => (
                <button
                  key={s}
                  type="button"
                  onClick={() => localBook.status !== s && handleStatusChange(s)}
                  className={`text-sm px-3 py-1.5 rounded-lg font-medium border transition-all ${
                    localBook.status === s
                      ? 'bg-amber-500 text-white border-amber-500'
                      : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-amber-400'
                  }`}
                >
                  {t(`addBook.status_${s}`)}
                </button>
              ))}
            </div>

            {/* Current page */}
            {localBook.status === 'reading' && (
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-sm text-gray-500 dark:text-gray-400">{t('bookDetail.currentPage')}</p>
                  {!editingPage && (
                    <button onClick={() => setEditingPage(true)} className="text-gray-400 hover:text-amber-500 transition-colors">
                      <Pencil size={13} />
                    </button>
                  )}
                </div>
                {editingPage ? (
                  <div className="flex gap-2 items-center">
                    <input
                      type="number"
                      min="1"
                      className="input w-28 text-sm"
                      value={pageInput}
                      onChange={e => setPageInput(e.target.value)}
                      autoFocus
                    />
                    {localBook.page_count && (
                      <span className="text-sm text-gray-400">/ {localBook.page_count}</span>
                    )}
                    <button onClick={handleSavePage} className="btn-primary text-sm py-1.5 px-3 flex items-center gap-1">
                      <Check size={14} /> {t('bookDetail.save')}
                    </button>
                    <button onClick={() => setEditingPage(false)} className="btn-ghost text-sm py-1.5">
                      {t('bookDetail.cancel')}
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-gray-700 dark:text-gray-300">
                    {localBook.current_page != null
                      ? localBook.page_count
                        ? `Page ${localBook.current_page} / ${localBook.page_count}`
                        : `Page ${localBook.current_page}`
                      : <span className="italic text-gray-400">{t('bookDetail.noPageYet')}</span>
                    }
                  </p>
                )}
                {localBook.current_page != null && localBook.page_count != null && (
                  <div className="mt-2 h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-amber-500 rounded-full transition-all"
                      style={{ width: `${Math.min(100, Math.round((localBook.current_page / localBook.page_count) * 100))}%` }}
                    />
                  </div>
                )}
              </div>
            )}

            {/* Rating */}
            {localBook.status === 'read' && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('bookDetail.yourRating')}</p>
                <StarRating value={localBook.rating} onChange={handleRatingChange} size={22} />
              </div>
            )}

            {/* Description */}
            {localBook.description && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{t('bookDetail.description')}</p>
                <button
                  onClick={() => (descTruncated || descExpanded) && setDescExpanded(e => !e)}
                  className="w-full text-left group"
                >
                  <p ref={descRef} className={`text-sm text-gray-700 dark:text-gray-300 leading-relaxed ${descExpanded ? '' : 'line-clamp-4'}`}>
                    {localBook.description}
                  </p>
                  {(descTruncated || descExpanded) && (
                  <span className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 mt-1 group-hover:underline">
                    {descExpanded
                      ? <><ChevronUp size={12} />{t('bookDetail.seeLess', 'See less')}</>
                      : <><ChevronDown size={12} />{t('bookDetail.seeMore', 'See more')}</>
                    }
                  </span>
                  )}
                </button>
              </div>
            )}

            {/* Personal note */}
            <div>
              <div className="flex items-center gap-2 mb-1">
                <p className="text-sm text-gray-500 dark:text-gray-400">{t('bookDetail.personalNote')}</p>
                {!editingNote && (
                  <button onClick={() => setEditingNote(true)} className="text-gray-400 hover:text-amber-500 transition-colors">
                    <Pencil size={13} />
                  </button>
                )}
              </div>
              {editingNote ? (
                <div className="space-y-2">
                  <textarea
                    value={note}
                    onChange={e => setNote(e.target.value)}
                    className="input text-sm h-24 resize-none"
                    placeholder={t('bookDetail.notePlaceholder')}
                    autoFocus
                  />
                  <div className="flex gap-2">
                    <button onClick={handleSaveNote} className="btn-primary text-sm py-1.5 flex items-center gap-1">
                      <Check size={14} /> {t('bookDetail.save')}
                    </button>
                    <button onClick={() => { setNote(book.personal_note || ''); setEditingNote(false); }} className="btn-ghost text-sm py-1.5">
                      {t('bookDetail.cancel')}
                    </button>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
                  {localBook.personal_note || <span className="text-gray-400 italic">{t('bookDetail.noNotes')}</span>}
                </p>
              )}
            </div>

            {/* Collections */}
            {categories.length > 0 && (
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">{t('bookDetail.collections')}</p>
                <div className="flex flex-wrap gap-2">
                  {categories.map(cat => {
                    const isIn = cat.book_ids.includes(localBook.id);
                    return (
                      <button
                        key={cat.id}
                        onClick={() => isIn
                          ? removeBookFromCategory(cat.id, localBook.id)
                          : addBooksToCategory(cat.id, [localBook.id])
                        }
                        className={`inline-flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border transition-colors ${
                          isIn
                            ? 'bg-amber-500/15 border-amber-400/50 text-amber-700 dark:text-amber-400 hover:bg-red-500/10 hover:border-red-400/50 hover:text-red-600 dark:hover:text-red-400'
                            : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-amber-500/10 hover:border-amber-400/50 hover:text-amber-700 dark:hover:text-amber-400'
                        }`}
                      >
                        {isIn ? <FolderMinus size={12} /> : <FolderPlus size={12} />}
                        {cat.title}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-wrap gap-2 pt-2">
              {!confirmDelete ? (
                <button onClick={() => setConfirmDelete(true)} className="btn-ghost text-sm text-red-500 hover:bg-red-500/10 flex items-center gap-1.5">
                  <Trash2 size={14} /> {t('bookDetail.delete')}
                </button>
              ) : (
                <div className="flex items-center gap-2 text-sm">
                  <span className="text-gray-600 dark:text-gray-400">{t('bookDetail.areYouSure')}</span>
                  <button onClick={handleDelete} className="text-red-500 font-medium hover:underline">{t('bookDetail.yesDelete')}</button>
                  <button onClick={() => setConfirmDelete(false)} className="text-gray-500 hover:underline">{t('bookDetail.cancel')}</button>
                </div>
              )}
            </div>
          </div>
        </div>
    </SheetModal>
  );
}
