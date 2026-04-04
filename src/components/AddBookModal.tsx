import { useState, useMemo } from 'react';
import { X, BookOpen, AlertTriangle } from 'lucide-react';
import { useBooks } from '../context/BooksContext';
import type { Book } from '../types';
import StarRating from './StarRating';
import SheetModal, { SheetCloseButton } from './SheetModal';
import { useTranslation } from 'react-i18next';

const normalize = (s: string) => s.toLowerCase().trim().replace(/\s+/g, ' ');

type BookFormData = Omit<Book, 'id' | 'user_id' | 'created_at'>;

const EMPTY: BookFormData = {
  google_books_id: null,
  title: '',
  author: '',
  description: null,
  cover_url: null,
  published_date: null,
  page_count: null,
  genre: null,
  status: 'want_to_read',
  rating: null,
  personal_note: null,
  current_page: null,
};

interface Props {
  prefill?: Partial<BookFormData>;
  onClose: () => void;
}

export default function AddBookModal({ prefill, onClose }: Props) {
  const { addBook, books } = useBooks();
  const { t } = useTranslation();
  const [form, setForm] = useState<BookFormData>({ ...EMPTY, ...prefill });
  const [saving, setSaving] = useState(false);

  const isDuplicate = useMemo(() => {
    if (!form.title.trim() || !form.author.trim()) return false;
    return books.some(
      b => normalize(b.title) === normalize(form.title) &&
           normalize(b.author) === normalize(form.author)
    );
  }, [books, form.title, form.author]);

  const set = <K extends keyof BookFormData>(k: K, v: BookFormData[K]) =>
    setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim()) return;
    setSaving(true);
    const result = await addBook(form);
    setSaving(false);
    if (result) onClose();
  };

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh]"
      scrollable
      header={
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('addBook.title')}</h2>
          <SheetCloseButton className="btn-ghost p-2"><X size={18} /></SheetCloseButton>
        </div>
      }
    >
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* Cover preview */}
          {form.cover_url && (
            <div className="flex justify-center mb-2">
              <div className="w-20 aspect-[2/3] rounded-xl overflow-hidden shadow-md">
                <img src={form.cover_url} alt="Cover" className="w-full h-full object-cover" />
              </div>
            </div>
          )}

          {/* Title & Author */}
          <div className="grid grid-cols-1 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.titleLabel')}</label>
              <input
                className="input"
                value={form.title}
                onChange={e => set('title', e.target.value)}
                placeholder={t('addBook.titlePlaceholder')}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.authorLabel')}</label>
              <input
                className="input"
                value={form.author}
                onChange={e => set('author', e.target.value)}
                placeholder={t('addBook.authorPlaceholder')}
              />
            </div>
          </div>

          {/* Genre & Year */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.genreLabel')}</label>
              <input className="input" value={form.genre || ''} onChange={e => set('genre', e.target.value || null)} placeholder={t('addBook.genrePlaceholder')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.publishedLabel')}</label>
              <input className="input" value={form.published_date || ''} onChange={e => set('published_date', e.target.value || null)} placeholder={t('addBook.publishedPlaceholder')} />
            </div>
          </div>

          {/* Page count & Cover URL */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.pagesLabel')}</label>
              <input className="input" type="number" min="1" value={form.page_count || ''} onChange={e => set('page_count', e.target.value ? parseInt(e.target.value) : null)} placeholder={t('addBook.pagesPlaceholder')} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.coverUrlLabel')}</label>
              <input className="input" value={form.cover_url || ''} onChange={e => set('cover_url', e.target.value || null)} placeholder={t('addBook.coverUrlPlaceholder')} />
            </div>
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              {t('addBook.statusLabel')}
            </label>
            <div className="flex gap-2">
              {(['want_to_read', 'reading', 'read'] as const).map(s => (
                <button
                  key={s}
                  type="button"
                  onClick={() => { set('status', s); if (s !== 'read') set('rating', null); if (s !== 'reading') set('current_page', null); }}
                  className={`flex-1 py-1.5 px-2 rounded-lg text-xs font-medium border transition-all ${
                    form.status === s
                      ? 'bg-amber-500 text-white border-amber-500'
                      : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:border-amber-400'
                  }`}
                >
                  {t(`addBook.status_${s}`)}
                </button>
              ))}
            </div>
          </div>

          {/* Current page (only if reading) */}
          {form.status === 'reading' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('addBook.currentPageLabel')}
              </label>
              <input
                className="input"
                type="number"
                min="1"
                value={form.current_page ?? ''}
                onChange={e => set('current_page', e.target.value ? parseInt(e.target.value) : null)}
                placeholder={t('addBook.currentPagePlaceholder')}
              />
            </div>
          )}

          {/* Rating (only if read) */}
          {form.status === 'read' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">{t('addBook.ratingLabel')}</label>
              <StarRating value={form.rating} onChange={v => set('rating', v)} size={28} />
            </div>
          )}

          {/* Note */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.noteLabel')}</label>
            <textarea
              className="input resize-none h-20 text-sm"
              value={form.personal_note || ''}
              onChange={e => set('personal_note', e.target.value || null)}
              placeholder={t('addBook.notePlaceholder')}
            />
          </div>

          {/* Description */}
          {form.description && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('addBook.descriptionLabel')}</label>
              <textarea
                className="input resize-none h-20 text-sm"
                value={form.description}
                onChange={e => set('description', e.target.value || null)}
              />
            </div>
          )}

          {/* Duplicate warning */}
          {isDuplicate && (
            <div className="flex items-start gap-2.5 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded-xl px-4 py-3">
              <AlertTriangle size={15} className="text-amber-500 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-amber-700 dark:text-amber-300">{t('addBook.alreadyInLibrary')}</p>
            </div>
          )}

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">{t('addBook.cancel')}</button>
            <button type="submit" disabled={saving || !form.title.trim()} className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
              {saving ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <BookOpen size={16} />
              )}
              {saving ? t('addBook.saving') : isDuplicate ? t('addBook.addAnyway') : t('addBook.addToLibrary')}
            </button>
          </div>
        </form>
    </SheetModal>
  );
}
