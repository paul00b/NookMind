import { useState, useMemo, useRef, useEffect } from 'react';
import { useBooks } from '../context/BooksContext';
import { useCategories } from '../context/CategoriesContext';
import type { Book, BookStatus, BookCategory } from '../types';
import BookCard from '../components/BookCard';
import BookDetailModal from '../components/BookDetailModal';
import CategoryBookPickerModal from '../components/CategoryBookPickerModal';
import StarRating from '../components/StarRating';
import { BookOpen, ChevronDown, LayoutGrid, List, Plus, X, Check, Trash2, FolderOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import toast from 'react-hot-toast';

type SortKey = 'created_at' | 'title' | 'author' | 'rating';
type ViewMode = 'grid' | 'list';
type ActiveTab = BookStatus | string; // string = category id

function EmptyState({ isCategoryTab, onAddBooks }: { isCategoryTab?: boolean; onAddBooks?: () => void }) {
  const { t } = useTranslation();
  if (isCategoryTab) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center px-4">
        <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
          <FolderOpen size={36} className="text-amber-500" />
        </div>
        <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
          {t('library.categoryEmpty')}
        </h3>
        <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs mb-5">
          {t('library.categoryEmptyDesc')}
        </p>
        {onAddBooks && (
          <button onClick={onAddBooks} className="btn-primary text-sm flex items-center gap-2">
            <Plus size={15} /> {t('library.addBooks')}
          </button>
        )}
      </div>
    );
  }
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center px-4">
      <div className="w-20 h-20 bg-amber-500/10 rounded-full flex items-center justify-center mb-5">
        <BookOpen size={36} className="text-amber-500" />
      </div>
      <h3 className="font-serif text-xl font-semibold text-gray-800 dark:text-gray-200 mb-2">
        {t('library.noBooksRead')}
      </h3>
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

function BookListRow({ book, onClick, onRemove }: { book: Book; onClick: () => void; onRemove?: () => void }) {
  const { t } = useTranslation();
  return (
    <div className="relative group">
      <button
        onClick={onClick}
        className="w-full flex items-center gap-4 px-4 py-3 rounded-xl hover:bg-amber-500/5 dark:hover:bg-amber-500/10 transition-colors text-left"
      >
        <div className="w-10 h-14 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
          {book.cover_url ? (
            <img src={book.cover_url} alt={book.title} className="w-full h-full object-cover" loading="lazy" />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <BookOpen size={16} className="text-gray-300 dark:text-gray-600" />
            </div>
          )}
        </div>
        <div className="flex-1 min-w-0">
          <p className="font-serif font-semibold text-sm text-gray-900 dark:text-gray-100 truncate group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
            {book.title}
          </p>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate mt-0.5">{book.author}</p>
        </div>
        {book.genre && (
          <span className="hidden sm:inline-flex flex-shrink-0 text-xs bg-amber-500/10 text-amber-700 dark:text-amber-400 px-2.5 py-1 rounded-full font-medium">
            {book.genre}
          </span>
        )}
        {book.status === 'read' && book.rating && (
          <div className="hidden sm:flex flex-shrink-0">
            <StarRating value={book.rating} readonly size={12} />
          </div>
        )}
        <span className={`flex-shrink-0 text-xs font-medium px-2.5 py-1 rounded-full text-white ${
          book.status === 'read' ? 'bg-emerald-500' : 'bg-amber-500'
        }`}>
          {book.status === 'read' ? t('bookCard.read') : t('bookCard.wantToRead')}
        </span>
      </button>
      {onRemove && (
        <button
          onClick={e => { e.stopPropagation(); onRemove(); }}
          className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10"
          title={t('library.removeFromCategory')}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}

export default function Library() {
  const { t } = useTranslation();
  const { books, loading } = useBooks();
  const { categories, createCategory, deleteCategory, addBooksToCategory, removeBookFromCategory } = useCategories();

  const [activeTab, setActiveTab] = useState<ActiveTab>('want_to_read');
  const [genreFilter, setGenreFilter] = useState('');
  const [authorFilter, setAuthorFilter] = useState('');
  const [sortKey, setSortKey] = useState<SortKey>('created_at');
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');

  // Category creation
  const [creatingCategory, setCreatingCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const nameInputRef = useRef<HTMLInputElement>(null);

  // Book picker for a category
  const [pickerCategory, setPickerCategory] = useState<BookCategory | null>(null);

  // Delete confirm
  const [deletingCategoryId, setDeletingCategoryId] = useState<string | null>(null);

  useEffect(() => {
    if (creatingCategory) nameInputRef.current?.focus();
  }, [creatingCategory]);

  const activeCategory = useMemo(
    () => categories.find(c => c.id === activeTab) ?? null,
    [categories, activeTab]
  );

  const isStatusTab = activeTab === 'read' || activeTab === 'want_to_read' || activeTab === 'reading';

  const filtered = useMemo(() => {
    if (!isStatusTab) return [];
    let list = books.filter(b => b.status === (activeTab as BookStatus));
    if (genreFilter) list = list.filter(b => b.genre === genreFilter);
    if (authorFilter) list = list.filter(b => b.author === authorFilter);
    return [...list].sort((a, b) => {
      if (sortKey === 'created_at') return new Date(b.created_at).getTime() - new Date(a.created_at).getTime();
      if (sortKey === 'title') return a.title.localeCompare(b.title);
      if (sortKey === 'author') return a.author.localeCompare(b.author);
      if (sortKey === 'rating') return (b.rating ?? 0) - (a.rating ?? 0);
      return 0;
    });
  }, [books, activeTab, isStatusTab, genreFilter, authorFilter, sortKey]);

  const categoryBooks = useMemo(() => {
    if (!activeCategory) return [];
    return books.filter(b => activeCategory.book_ids.includes(b.id));
  }, [books, activeCategory]);

  const tabBooks = books.filter(b => isStatusTab && b.status === (activeTab as BookStatus));
  const genres = [...new Set(tabBooks.map(b => b.genre).filter(Boolean))] as string[];
  const authors = [...new Set(tabBooks.map(b => b.author).filter(Boolean))] as string[];

  const counts = {
    read: books.filter(b => b.status === 'read').length,
    want_to_read: books.filter(b => b.status === 'want_to_read').length,
    reading: books.filter(b => b.status === 'reading').length,
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    const cat = await createCategory(newCategoryName.trim());
    setNewCategoryName('');
    setCreatingCategory(false);
    if (cat) setActiveTab(cat.id);
  };

  const handleDeleteCategory = async (id: string) => {
    const cat = categories.find(c => c.id === id);
    if (activeTab === id) setActiveTab('want_to_read');
    await deleteCategory(id);
    setDeletingCategoryId(null);
    if (cat) toast.success(`"${cat.title}" supprimée`);
  };

  const handlePickerConfirm = async (newBookIds: string[]) => {
    if (!pickerCategory) return;
    await addBooksToCategory(pickerCategory.id, newBookIds);
    setPickerCategory(null);
  };

  const tabClass = (isActive: boolean) =>
    `pb-3 px-1 text-sm font-medium border-b-2 transition-all -mb-px flex-shrink-0 ${
      isActive
        ? 'border-amber-500 text-amber-600 dark:text-amber-400'
        : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
    }`;

  const countBadge = (count: number, isActive: boolean) => (
    <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
      isActive ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-500'
    }`}>
      {count}
    </span>
  );

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-1">{t('library.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">{t('library.booksCount', { count: books.length })}</p>
        </div>
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
      <div className="-mx-4 mb-6 overflow-x-auto overflow-y-hidden border-b border-black/[0.06] px-4 pb-0 [touch-action:pan-x] dark:border-white/[0.06] md:mx-0 md:px-0" style={{ scrollbarWidth: 'none' }}>
        <div className="flex min-w-max gap-2">
          {/* Built-in status tabs */}
          {([
            ['reading', t('library.reading')],
            ['want_to_read', t('library.wantToRead')],
            ['read', t('library.read')],
          ] as const).map(([status, label]) => (
            <button
              key={status}
              onClick={() => { setActiveTab(status); setGenreFilter(''); setAuthorFilter(''); }}
              className={tabClass(activeTab === status)}
            >
              {label}{countBadge(counts[status], activeTab === status)}
            </button>
          ))}

          {/* Custom category tabs */}
          {categories.map(cat => (
            <div key={cat.id} className="relative group flex-shrink-0 flex items-end">
              <button
                onClick={() => setActiveTab(cat.id)}
                className={tabClass(activeTab === cat.id) + ' pr-5'}
              >
                {cat.title}{countBadge(cat.book_ids.length, activeTab === cat.id)}
              </button>
              {/* Delete button on hover */}
              <button
                onClick={() => setDeletingCategoryId(cat.id)}
                className="absolute right-0 bottom-3 opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-gray-400 hover:text-red-500"
              >
                <X size={12} />
              </button>
            </div>
          ))}

          {/* New category */}
          {creatingCategory ? (
            <form onSubmit={handleCreateCategory} className="flex items-center gap-1 pb-3 flex-shrink-0">
              <input
                ref={nameInputRef}
                value={newCategoryName}
                onChange={e => setNewCategoryName(e.target.value)}
                placeholder={t('library.newCategoryPlaceholder')}
                className="input py-1 text-sm w-36"
                onBlur={() => { if (!newCategoryName.trim()) setCreatingCategory(false); }}
              />
              <button type="submit" className="p-1 text-amber-600 hover:text-amber-700">
                <Check size={16} />
              </button>
              <button type="button" onClick={() => { setCreatingCategory(false); setNewCategoryName(''); }} className="p-1 text-gray-400 hover:text-gray-600">
                <X size={16} />
              </button>
            </form>
          ) : (
            <button
              onClick={() => setCreatingCategory(true)}
              className="pb-3 px-1 text-sm font-medium border-b-2 border-transparent text-gray-400 hover:text-amber-500 dark:hover:text-amber-400 -mb-px flex-shrink-0 flex items-center gap-1 transition-colors"
            >
              <Plus size={14} />{t('library.newCategory')}
            </button>
          )}
        </div>
      </div>

      {/* Delete category confirm */}
      {deletingCategoryId && (() => {
        const cat = categories.find(c => c.id === deletingCategoryId)!;
        return (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl flex items-center gap-3 text-sm">
            <Trash2 size={15} className="text-red-500 flex-shrink-0" />
            <span className="flex-1 text-red-700 dark:text-red-400">{t('library.confirmDeleteCategory', { name: cat.title })}</span>
            <button onClick={() => handleDeleteCategory(deletingCategoryId)} className="font-medium text-red-600 hover:underline">{t('bookDetail.yesDelete')}</button>
            <button onClick={() => setDeletingCategoryId(null)} className="text-gray-500 hover:underline">{t('library.cancel')}</button>
          </div>
        );
      })()}

      {/* Category toolbar (custom category active) */}
      {activeCategory && (
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {activeCategory.book_ids.length} livre{activeCategory.book_ids.length !== 1 ? 's' : ''}
          </p>
          <button
            onClick={() => setPickerCategory(activeCategory)}
            className="btn-primary text-sm flex items-center gap-2 py-2"
          >
            <Plus size={15} /> {t('library.addBooks')}
          </button>
        </div>
      )}

      {/* Filters — only for status tabs */}
      {isStatusTab && tabBooks.length > 0 && (
        <div className="-mx-4 mb-6 overflow-x-auto overflow-y-hidden px-4 [touch-action:pan-x] md:mx-0 md:px-0" style={{ scrollbarWidth: 'none' }}>
          <div className="flex min-w-max items-center gap-2 pb-1">
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
      ) : activeCategory ? (
        /* Custom category view */
        categoryBooks.length === 0 ? (
          <EmptyState isCategoryTab onAddBooks={() => setPickerCategory(activeCategory)} />
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            {categoryBooks.map(book => (
              <div key={book.id} className="relative group/card">
                <BookCard book={book} onClick={() => setSelectedBook(book)} />
                <button
                  onClick={() => removeBookFromCategory(activeCategory.id, book.id)}
                  className="absolute top-2 left-2 opacity-0 group-hover/card:opacity-100 transition-opacity p-1 rounded-full bg-black/50 text-white hover:bg-red-500"
                  title={t('library.removeFromCategory')}
                >
                  <X size={11} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="card divide-y divide-black/[0.05] dark:divide-white/[0.05] overflow-hidden">
            {categoryBooks.map(book => (
              <BookListRow
                key={book.id}
                book={book}
                onClick={() => setSelectedBook(book)}
                onRemove={() => removeBookFromCategory(activeCategory.id, book.id)}
              />
            ))}
          </div>
        )
      ) : filtered.length === 0 ? (
        <EmptyState />
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

      {/* Book detail modal */}
      {selectedBook && (
        <BookDetailModal
          book={selectedBook}
          onClose={() => setSelectedBook(null)}
        />
      )}

      {/* Category book picker */}
      {pickerCategory && (
        <CategoryBookPickerModal
          category={pickerCategory}
          books={books}
          onConfirm={handlePickerConfirm}
          onClose={() => setPickerCategory(null)}
        />
      )}
    </div>
  );
}
