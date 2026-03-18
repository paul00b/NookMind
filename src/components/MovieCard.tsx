import type { Movie } from '../types';
import StarRating from './StarRating';
import { Film } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface MovieCardProps {
  movie: Movie;
  onClick: () => void;
}

export default function MovieCard({ movie, onClick }: MovieCardProps) {
  const { t } = useTranslation();
  return (
    <button
      onClick={onClick}
      className="card text-left group cursor-pointer hover:scale-[1.02] transition-transform duration-200 overflow-hidden flex flex-col"
    >
      {/* Poster */}
      <div className="relative w-full aspect-[2/3] bg-gray-100 dark:bg-gray-800 rounded-xl overflow-hidden mb-3">
        {movie.poster_url ? (
          <img
            src={movie.poster_url}
            alt={movie.title}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Film size={40} className="text-gray-300 dark:text-gray-600" />
          </div>
        )}
        {/* Status badge */}
        <div className={`absolute top-2 right-2 text-xs font-medium px-2 py-0.5 rounded-full ${
          movie.status === 'watched'
            ? 'bg-emerald-500/90 text-white'
            : 'bg-amber-500/90 text-white'
        }`}>
          {movie.status === 'watched' ? t('movieCard.watched') : t('movieCard.wantToWatch')}
        </div>
      </div>

      {/* Info */}
      <div className="px-1 pb-1 flex-1 flex flex-col gap-1">
        <h3 className="font-serif font-semibold text-sm leading-tight text-gray-900 dark:text-gray-100 line-clamp-2 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
          {movie.title}
        </h3>
        <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{movie.director}</p>
        {movie.status === 'watched' && movie.rating && (
          <StarRating value={movie.rating} readonly size={13} />
        )}
      </div>
    </button>
  );
}
