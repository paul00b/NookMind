import { useLayoutEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';

interface SearchSectionStackItem {
  id: string;
  visible: boolean;
  node: ReactNode;
}

interface Props {
  items: SearchSectionStackItem[];
}

export default function SearchSectionStack({ items }: Props) {
  const nodeRefs = useRef(new Map<string, HTMLDivElement>());
  const contentRefs = useRef(new Map<string, HTMLDivElement>());
  const previousPositionsRef = useRef(new Map<string, number>());
  const [contentHeights, setContentHeights] = useState<Record<string, number>>({});

  useLayoutEffect(() => {
    const nextHeights: Record<string, number> = {};
    items.forEach(item => {
      const node = contentRefs.current.get(item.id);
      if (!node) return;
      nextHeights[item.id] = node.scrollHeight;
    });
    setContentHeights(prev => {
      const prevKeys = Object.keys(prev);
      const nextKeys = Object.keys(nextHeights);
      if (prevKeys.length === nextKeys.length && nextKeys.every(key => prev[key] === nextHeights[key])) {
        return prev;
      }
      return nextHeights;
    });
  }, [items]);

  useLayoutEffect(() => {
    const nextPositions = new Map<string, number>();

    items.forEach(item => {
      const node = nodeRefs.current.get(item.id);
      if (!node) return;

      const nextTop = node.getBoundingClientRect().top;
      nextPositions.set(item.id, nextTop);

      const previousTop = previousPositionsRef.current.get(item.id);
      if (previousTop == null) return;

      const deltaY = previousTop - nextTop;
      if (Math.abs(deltaY) < 1) return;

      node.animate(
        [
          { transform: `translateY(${deltaY}px)` },
          { transform: 'translateY(0px)' },
        ],
        {
          duration: 260,
          easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
        }
      );
    });

    previousPositionsRef.current = nextPositions;
  }, [items]);

  return (
    <div className="w-full max-w-xl">
      {items.map(item => (
        <div
          key={item.id}
          ref={node => {
            if (node) nodeRefs.current.set(item.id, node);
            else nodeRefs.current.delete(item.id);
          }}
          style={{ height: item.visible ? (contentHeights[item.id] ?? 0) : 0, willChange: 'height, transform, opacity' }}
          className={`origin-top overflow-hidden transform-gpu transition-[height,opacity,transform,margin] duration-300 ease-[cubic-bezier(0.22,1,0.36,1)] ${
            item.visible
              ? 'opacity-100 translate-y-0 scale-100 mt-10'
              : 'opacity-0 -translate-y-1 scale-[0.99] mt-0 pointer-events-none'
          }`}
          aria-hidden={!item.visible}
        >
          <div
            ref={node => {
              if (node) contentRefs.current.set(item.id, node);
              else contentRefs.current.delete(item.id);
            }}
            className={item.visible ? 'transition-opacity duration-200 opacity-100' : 'transition-opacity duration-150 opacity-0'}
          >
            {item.node}
          </div>
        </div>
      ))}
    </div>
  );
}
