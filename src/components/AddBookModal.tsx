import { useState } from 'react';
import { X, BookOpen } from 'lucide-react';
import { useBooks } from '../context/BooksContext';
import type { Book, BookStatus } from '../types';
import StarRating from './StarRating';

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
};

interface Props {
  prefill?: Partial<BookFormData>;
  onClose: () => void;
}

export default function AddBookModal({ prefill, onClose }: Props) {
  const { addBook } = useBooks();
  const [form, setForm] = useState<BookFormData>({ ...EMPTY, ...prefill });
  const [saving, setSaving] = useState(false);

  const set = <K extends keyof BookFormData>(k: K, v: BookFormData[K]) =>
    setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.title.trim() || !form.author.trim()) return;
    setSaving(true);
    const result = await addBook(form);
    setSaving(false);
    if (result) onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-0 md:p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      {/* Modal */}
      <div className="relative z-10 w-full md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[92vh] overflow-y-auto">
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">Add to Library</h2>
          <button onClick={onClose} className="btn-ghost p-2"><X size={18} /></button>
        </div>

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
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Title *</label>
              <input
                className="input"
                value={form.title}
                onChange={e => set('title', e.target.value)}
                placeholder="Book title"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Author *</label>
              <input
                className="input"
                value={form.author}
                onChange={e => set('author', e.target.value)}
                placeholder="Author name"
                required
              />
            </div>
          </div>

          {/* Genre & Year */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Genre</label>
              <input className="input" value={form.genre || ''} onChange={e => set('genre', e.target.value || null)} placeholder="e.g. Fiction" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Published</label>
              <input className="input" value={form.published_date || ''} onChange={e => set('published_date', e.target.value || null)} placeholder="Year" />
            </div>
          </div>

          {/* Page count & Cover URL */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Pages</label>
              <input className="input" type="number" min="1" value={form.page_count || ''} onChange={e => set('page_count', e.target.value ? parseInt(e.target.value) : null)} placeholder="Page count" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Cover URL</label>
              <input className="input" value={form.cover_url || ''} onChange={e => set('cover_url', e.target.value || null)} placeholder="Image URL" />
            </div>
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Status</label>
            <div className="flex gap-2">
              {(['want_to_read', 'read'] as BookStatus[]).map(s => (
                <button
                  key={s}
                  type="button"
                  onClick={() => { set('status', s); if (s === 'want_to_read') set('rating', null); }}
                  className={`flex-1 py-2.5 rounded-full text-sm font-medium border transition-all ${
                    form.status === s
                      ? 'bg-amber-500 text-white border-amber-500'
                      : 'bg-transparent text-gray-600 dark:text-gray-400 border-black/10 dark:border-white/10 hover:border-amber-500/50'
                  }`}
                >
                  {s === 'read' ? 'Already read' : 'Want to read'}
                </button>
              ))}
            </div>
          </div>

          {/* Rating (only if read) */}
          {form.status === 'read' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Rating</label>
              <StarRating value={form.rating} onChange={v => set('rating', v)} size={24} />
            </div>
          )}

          {/* Note */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Personal note</label>
            <textarea
              className="input resize-none h-20 text-sm"
              value={form.personal_note || ''}
              onChange={e => set('personal_note', e.target.value || null)}
              placeholder="Your thoughts..."
            />
          </div>

          {/* Description */}
          {form.description && (
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
              <textarea
                className="input resize-none h-20 text-sm"
                value={form.description}
                onChange={e => set('description', e.target.value || null)}
              />
            </div>
          )}

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Cancel</button>
            <button type="submit" disabled={saving || !form.title.trim()} className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
              {saving ? (
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <BookOpen size={16} />
              )}
              {saving ? 'Saving...' : 'Add to Library'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
