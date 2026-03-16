import { useState } from 'react';
import type { Book } from '../types';
import { useBooks } from '../context/BooksContext';
import StarRating from './StarRating';
import { X, Pencil, Check, Trash2, BookOpen, ArrowLeftRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

interface Props {
  book: Book;
  onClose: () => void;
}

export default function BookDetailModal({ book, onClose }: Props) {
  const { updateBook, deleteBook } = useBooks();
  const { t } = useTranslation();
  const [editingNote, setEditingNote] = useState(false);
  const [note, setNote] = useState(book.personal_note || '');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [localBook, setLocalBook] = useState<Book>(book);

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

  const handleToggleStatus = async () => {
    const newStatus = localBook.status === 'read' ? 'want_to_read' : 'read';
    const updates: Partial<Book> = { status: newStatus };
    if (newStatus === 'want_to_read') updates.rating = null;
    setLocalBook(b => ({ ...b, ...updates }));
    await updateBook(book.id, updates);
    toast.success(newStatus === 'read' ? t('bookDetail.movedToRead') : t('bookDetail.movedToWantToRead'));
  };

  const handleDelete = async () => {
    await deleteBook(book.id);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-0 md:p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative z-10 w-full md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none md:max-h-[90vh] overflow-y-auto">
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
                localBook.status === 'read' ? 'bg-emerald-500' : 'bg-amber-500'
              }`}>
                {localBook.status === 'read' ? t('bookDetail.read') : t('bookDetail.wantToRead')}
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
                <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed line-clamp-4">
                  {localBook.description}
                </p>
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

            {/* Actions */}
            <div className="flex flex-wrap gap-2 pt-2">
              <button onClick={handleToggleStatus} className="btn-ghost text-sm flex items-center gap-1.5">
                <ArrowLeftRight size={14} />
                {localBook.status === 'read' ? t('bookDetail.moveToWantToRead') : t('bookDetail.moveToRead')}
              </button>
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
      </div>
    </div>
  );
}
