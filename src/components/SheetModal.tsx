import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent, type ReactNode } from 'react';

interface SheetModalProps {
  children: ReactNode;
  onClose: () => void;
  panelClassName: string;
  overlayClassName?: string;
  rootClassName?: string;
  showHandle?: boolean;
}

const MOBILE_MAX_WIDTH = 767;
const CLOSE_THRESHOLD_PX = 96;

export default function SheetModal({
  children,
  onClose,
  panelClassName,
  overlayClassName = 'bg-black/50 backdrop-blur-sm animate-fade-in',
  rootClassName = 'z-50',
  showHandle = true,
}: SheetModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const dragStateRef = useRef<{ pointerId: number; startY: number } | null>(null);
  const [dragOffset, setDragOffset] = useState(0);
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  useEffect(() => {
    if (!isDragging) return;

    const handlePointerMove = (event: PointerEvent) => {
      const dragState = dragStateRef.current;
      if (!dragState || event.pointerId !== dragState.pointerId) return;
      const nextOffset = Math.max(0, event.clientY - dragState.startY);
      setDragOffset(nextOffset);
    };

    const finishDrag = (event: PointerEvent) => {
      const dragState = dragStateRef.current;
      if (!dragState || event.pointerId !== dragState.pointerId) return;
      dragStateRef.current = null;
      setIsDragging(false);
      if (dragOffset >= CLOSE_THRESHOLD_PX) {
        onClose();
        return;
      }
      setDragOffset(0);
    };

    window.addEventListener('pointermove', handlePointerMove, { passive: true });
    window.addEventListener('pointerup', finishDrag);
    window.addEventListener('pointercancel', finishDrag);

    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', finishDrag);
      window.removeEventListener('pointercancel', finishDrag);
    };
  }, [dragOffset, isDragging, onClose]);

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    const isMobile = typeof window !== 'undefined' && window.innerWidth <= MOBILE_MAX_WIDTH;
    if (!isMobile) return;
    dragStateRef.current = { pointerId: event.pointerId, startY: event.clientY };
    setIsDragging(true);
    setDragOffset(0);
  };

  return (
    <div className={`fixed inset-0 flex items-end md:items-center justify-center p-0 md:p-4 ${rootClassName}`}>
      <button
        type="button"
        className={`absolute inset-0 ${overlayClassName}`}
        onClick={onClose}
        aria-label="Close"
      />

      <div
        ref={panelRef}
        className={`relative z-10 w-full ${panelClassName}`}
        style={{
          transform: dragOffset > 0 ? `translateY(${dragOffset}px)` : undefined,
          transition: isDragging ? 'none' : 'transform 220ms cubic-bezier(0.22, 1, 0.36, 1)',
        }}
      >
        {showHandle && (
          <div className="md:hidden px-6 pt-3 pb-1">
            <div
              className="mx-auto flex h-8 w-24 items-center justify-center touch-none"
              onPointerDown={handlePointerDown}
            />
            <div className="pointer-events-none mx-auto -mt-4 h-1.5 w-12 rounded-full bg-gray-300 dark:bg-gray-600" />
          </div>
        )}
        {children}
      </div>
    </div>
  );
}
