import { Star } from 'lucide-react';

interface StarRatingProps {
  value: number | null;
  onChange?: (v: number) => void;
  readonly?: boolean;
  size?: number;
}

export default function StarRating({ value, onChange, readonly = false, size = 18 }: StarRatingProps) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map(star => (
        <button
          key={star}
          type="button"
          disabled={readonly}
          onClick={() => onChange?.(star)}
          className={`transition-transform ${readonly ? 'cursor-default' : 'cursor-pointer hover:scale-110 active:scale-95'}`}
        >
          <Star
            size={size}
            className={
              (value ?? 0) >= star
                ? 'fill-amber-500 text-amber-500'
                : 'fill-transparent text-gray-300 dark:text-gray-600'
            }
          />
        </button>
      ))}
    </div>
  );
}
