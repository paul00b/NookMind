import { createContext, useCallback, useContext, useEffect, useRef, useState, type ButtonHTMLAttributes, type PointerEvent as ReactPointerEvent, type ReactNode } from 'react';

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
const CLOSE_ANIMATION_MS = 220;

const SheetCloseContext = createContext<(() => void) | null>(null);

export function useSheetClose() {
  const requestClose = useContext(SheetCloseContext);
  if (!requestClose) {
    throw new Error('useSheetClose must be used within SheetModal');
  }
  return requestClose;
}

export function SheetCloseButton({
  children,
  onClick,
  type = 'button',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement>) {
  const requestClose = useSheetClose();

  return (
    <button
      {...props}
      type={type}
      onClick={event => {
        onClick?.(event);
        if (!event.defaultPrevented) requestClose();
      }}
    >
      {children}
    </button>
  );
}

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
  const closeTimeoutRef = useRef<number | null>(null);
  const [dragOffset, setDragOffset] = useState(0);
  const [isDragging, setIsDragging] = useState(false);

  const isMobileViewport = useCallback(
    () => typeof window !== 'undefined' && window.innerWidth <= MOBILE_MAX_WIDTH,
    []
  );

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
      if (closeTimeoutRef.current != null) {
        window.clearTimeout(closeTimeoutRef.current);
      }
    };
  }, []);

  const requestClose = useCallback(() => {
    if (closeTimeoutRef.current != null) return;

    dragStateRef.current = null;
    setIsDragging(false);

    if (!isMobileViewport()) {
      onClose();
      return;
    }

    const panelHeight = panelRef.current?.getBoundingClientRect().height ?? window.innerHeight;
    setDragOffset(Math.max(panelHeight + 32, CLOSE_THRESHOLD_PX));

    closeTimeoutRef.current = window.setTimeout(() => {
      onClose();
    }, CLOSE_ANIMATION_MS);
  }, [isMobileViewport, onClose]);

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
        requestClose();
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
  }, [dragOffset, isDragging, requestClose]);

  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    const isMobile = typeof window !== 'undefined' && window.innerWidth <= MOBILE_MAX_WIDTH;
    if (!isMobile) return;
    dragStateRef.current = { pointerId: event.pointerId, startY: event.clientY };
    setIsDragging(true);
    setDragOffset(0);
  };

  return (
    <SheetCloseContext.Provider value={requestClose}>
      <div className={`fixed inset-0 flex items-end md:items-center justify-center p-0 md:p-4 ${rootClassName}`}>
        <button
          type="button"
          className={`absolute inset-0 ${overlayClassName}`}
          onClick={requestClose}
          aria-label="Close"
        />

        <div
          ref={panelRef}
          className={`relative z-10 w-full ${panelClassName}`}
          style={{
            transform: dragOffset > 0 ? `translateY(${dragOffset}px)` : undefined,
            transition: isDragging ? 'none' : `transform ${CLOSE_ANIMATION_MS}ms cubic-bezier(0.22, 1, 0.36, 1)`,
          }}
        >
          {showHandle && (
            <div className="md:hidden px-6 pt-3 pb-1">
              <div
                className="mx-auto flex h-8 w-40 max-w-full items-center justify-center touch-none"
                onPointerDown={handlePointerDown}
              />
              <div className="pointer-events-none mx-auto -mt-4 h-1.5 w-12 rounded-full bg-gray-300 dark:bg-gray-600" />
            </div>
          )}
          {children}
        </div>
      </div>
    </SheetCloseContext.Provider>
  );
}
