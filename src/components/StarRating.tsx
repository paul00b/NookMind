import { useRef, useState, useId } from 'react';

interface StarRatingProps {
  value: number | null;
  onChange?: (v: number) => void;
  readonly?: boolean;
  size?: number;
}

type FillType = 'empty' | 'half' | 'full';

function getFill(displayValue: number, star: number): FillType {
  if (displayValue >= star) return 'full';
  if (displayValue >= star - 0.5) return 'half';
  return 'empty';
}

function StarIcon({ fill, size, clipId }: { fill: FillType; size: number; clipId: string }) {
  const path = 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z';

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" style={{ flexShrink: 0 }}>
      {fill === 'half' && (
        <defs>
          <clipPath id={clipId}>
            <rect x="0" y="0" width="12" height="24" />
          </clipPath>
        </defs>
      )}
      {/* Background star (always drawn) */}
      <path
        d={path}
        fill="none"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={fill === 'empty' ? 'stroke-gray-300 dark:stroke-gray-600' : 'stroke-amber-500'}
      />
      {/* Amber fill (full or half-clipped) */}
      {fill !== 'empty' && (
        <path
          d={path}
          strokeWidth="0"
          className="fill-amber-500"
          clipPath={fill === 'half' ? `url(#${clipId})` : undefined}
        />
      )}
    </svg>
  );
}

export default function StarRating({ value, onChange, readonly = false, size = 18 }: StarRatingProps) {
  const uid = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const [hoverValue, setHoverValue] = useState<number | null>(null);
  const isDragging = useRef(false);

  const displayValue = hoverValue ?? value ?? 0;

  const getValueFromPointer = (clientX: number): number => {
    const rect = containerRef.current!.getBoundingClientRect();
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width - 1));
    const starWidth = rect.width / 5;
    const starIndex = Math.floor(x / starWidth);
    const posInStar = (x - starIndex * starWidth) / starWidth;
    return Math.min(Math.max(starIndex + (posInStar < 0.5 ? 0.5 : 1), 0.5), 5);
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    isDragging.current = true;
    e.currentTarget.setPointerCapture(e.pointerId);
    setHoverValue(getValueFromPointer(e.clientX));
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDragging.current) {
      setHoverValue(getValueFromPointer(e.clientX));
    } else {
      setHoverValue(getValueFromPointer(e.clientX));
    }
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging.current) return;
    isDragging.current = false;
    onChange?.(getValueFromPointer(e.clientX));
    setHoverValue(null);
  };

  const handlePointerLeave = () => {
    if (!isDragging.current) setHoverValue(null);
  };

  if (readonly) {
    return (
      <div className="inline-flex gap-0.5">
        {[1, 2, 3, 4, 5].map(star => (
          <StarIcon key={star} fill={getFill(displayValue, star)} size={size} clipId={`${uid}-${star}`} />
        ))}
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className="inline-flex gap-0.5 cursor-pointer select-none touch-none"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerLeave={handlePointerLeave}
    >
      {[1, 2, 3, 4, 5].map(star => (
        <StarIcon key={star} fill={getFill(displayValue, star)} size={size} clipId={`${uid}-${star}`} />
      ))}
    </div>
  );
}
