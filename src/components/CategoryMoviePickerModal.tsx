import { Film } from 'lucide-react';
import type { Movie, MovieCategory } from '../types';
import { useTranslation } from 'react-i18next';
import CategoryItemPickerModal from './CategoryItemPickerModal';

interface Props {
  category: MovieCategory;
  movies: Movie[];
  onConfirm: (movieIds: string[]) => void;
  onClose: () => void;
}

export default function CategoryMoviePickerModal({ category, movies, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  return (
    <CategoryItemPickerModal
      existingIds={category.movie_ids}
      items={movies.map(m => ({ id: m.id, title: m.title, subtitle: m.director, imageUrl: m.poster_url }))}
      onConfirm={onConfirm}
      onClose={onClose}
      config={{
        header: t('movieLibrary.addMoviesTo', { name: category.title }),
        searchPlaceholder: t('movieLibrary.searchMovies'),
        noItemsFound: t('movieLibrary.noMoviesFound'),
        alreadyInCategory: t('movieLibrary.alreadyInCategory'),
        cancel: t('movieLibrary.cancel'),
        confirmLabel: n => n > 0 ? t('movieLibrary.addNMovies', { count: n }) : t('movieLibrary.confirmAdd'),
        fallbackIcon: <Film size={14} className="text-gray-400" />,
      }}
    />
  );
}
