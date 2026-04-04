import { BookOpen } from 'lucide-react';
import type { Book, BookCategory } from '../types';
import { useTranslation } from 'react-i18next';
import CategoryItemPickerModal from './CategoryItemPickerModal';

interface Props {
  category: BookCategory;
  books: Book[];
  onConfirm: (bookIds: string[]) => void;
  onClose: () => void;
}

export default function CategoryBookPickerModal({ category, books, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  return (
    <CategoryItemPickerModal
      existingIds={category.book_ids}
      items={books.map(b => ({ id: b.id, title: b.title, subtitle: b.author, imageUrl: b.cover_url }))}
      onConfirm={onConfirm}
      onClose={onClose}
      config={{
        header: t('library.addBooksTo', { name: category.title }),
        searchPlaceholder: t('library.searchBooks'),
        noItemsFound: t('library.noBooksFound'),
        alreadyInCategory: t('library.alreadyInCategory'),
        cancel: t('library.cancel'),
        confirmLabel: n => n > 0 ? t('library.addNBooks', { count: n }) : t('library.confirmAdd'),
        fallbackIcon: <BookOpen size={14} className="text-gray-400" />,
      }}
    />
  );
}
