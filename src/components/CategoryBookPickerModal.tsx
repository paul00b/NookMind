import { useState, useMemo } from 'react';
import { X, Search, Check, BookOpen } from 'lucide-react';
import type { Book, BookCategory } from '../types';
import { useTranslation } from 'react-i18next';

interface Props {
  category: BookCategory;
  books: Book[];
  onConfirm: (bookIds: string[]) => void;
  onClose: () => void;
}

export default function CategoryBookPickerModal({ category, books, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set(category.book_ids));

  const filtered = useMemo(() => {
    const q = query.toLowerCase();
    return books.filter(b =>
      !q || b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q)
    );
  }, [books, query]);

  const toggle = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleConfirm = () => {
    const newIds = [...selected].filter(id => !category.book_ids.includes(id));
    onConfirm(newIds);
  };

  const newCount = [...selected].filter(id => !category.book_ids.includes(id)).length;

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center p-0 md:p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      <div className="relative z-10 w-full md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-black/[0.06] dark:border-white/[0.06] flex-shrink-0">
          <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
            {t('library.addBooksTo', { name: category.title })}
          </h2>
          <button onClick={onClose} className="btn-ghost p-2">
            <X size={18} />
          </button>
        </div>

        {/* Search */}
        <div className="px-5 py-3 flex-shrink-0">
          <div className="relative">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder={t('library.searchBooks')}
              className="input pl-9 text-sm py-2"
              autoFocus
            />
          </div>
        </div>

        {/* Book list */}
        <div className="flex-1 overflow-y-auto px-3 pb-3">
          {filtered.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-10">{t('library.noBooksFound')}</p>
          ) : (
            filtered.map(book => {
              const isSelected = selected.has(book.id);
              const alreadyIn = category.book_ids.includes(book.id);
              return (
                <button
                  key={book.id}
                  onClick={() => toggle(book.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors text-left ${
                    isSelected
                      ? 'bg-amber-500/10 dark:bg-amber-500/15'
                      : 'hover:bg-gray-50 dark:hover:bg-white/5'
                  }`}
                >
                  {/* Cover */}
                  <div className="w-9 h-12 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
                    {book.cover_url ? (
                      <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <BookOpen size={14} className="text-gray-400" />
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{book.title}</p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{book.author}</p>
                  </div>

                  {/* Already-in badge or checkbox */}
                  {alreadyIn ? (
                    <span className="flex-shrink-0 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                      {t('library.alreadyInCategory')}
                    </span>
                  ) : (
                    <div className={`w-5 h-5 rounded-md border-2 flex-shrink-0 flex items-center justify-center transition-colors ${
                      isSelected
                        ? 'bg-amber-500 border-amber-500'
                        : 'border-gray-300 dark:border-gray-600'
                    }`}>
                      {isSelected && <Check size={12} className="text-white" strokeWidth={3} />}
                    </div>
                  )}
                </button>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="p-5 border-t border-black/[0.06] dark:border-white/[0.06] flex gap-3 flex-shrink-0">
          <button onClick={onClose} className="btn-ghost flex-1 text-sm">
            {t('library.cancel')}
          </button>
          <button
            onClick={handleConfirm}
            disabled={newCount === 0}
            className="btn-primary flex-1 text-sm disabled:opacity-40"
          >
            {newCount > 0
              ? t('library.addNBooks', { count: newCount })
              : t('library.confirmAdd')}
          </button>
        </div>
      </div>
    </div>
  );
}
