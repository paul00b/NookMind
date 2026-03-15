import type { Book } from '../types';
import StarRating from './StarRating';
import { BookOpen } from 'lucide-react';

interface BookCardProps {
  book: Book;
  onClick: () => void;
}

export default function BookCard({ book, onClick }: BookCardProps) {
  return (
    <button
      onClick={onClick}
      className="card text-left group cursor-pointer hover:scale-[1.02] transition-transform duration-200 overflow-hidden flex flex-col"
    >
      {/* Cover */}
      <div className="relative w-full aspect-[2/3] bg-gray-100 dark:bg-gray-800 rounded-xl overflow-hidden mb-3">
        {book.cover_url ? (
          <img
            src={book.cover_url}
            alt={book.title}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <BookOpen size={40} className="text-gray-300 dark:text-gray-600" />
          </div>
        )}
        {/* Status badge */}
        <div className={`absolute top-2 right-2 text-xs font-medium px-2 py-0.5 rounded-full ${
          book.status === 'read'
            ? 'bg-emerald-500/90 text-white'
            : 'bg-amber-500/90 text-white'
        }`}>
          {book.status === 'read' ? 'Read' : 'Want to read'}
        </div>
      </div>

      {/* Info */}
      <div className="px-1 pb-1 flex-1 flex flex-col gap-1">
        <h3 className="font-serif font-semibold text-sm leading-tight text-gray-900 dark:text-gray-100 line-clamp-2 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
          {book.title}
        </h3>
        <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{book.author}</p>
        {book.status === 'read' && book.rating && (
          <StarRating value={book.rating} readonly size={13} />
        )}
      </div>
    </button>
  );
}
