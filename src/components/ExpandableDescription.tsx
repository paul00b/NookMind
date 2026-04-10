import { useState, useRef, useEffect } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

interface Props {
  description: string;
  label?: string;
  seeMoreText: string;
  seeLessText: string;
  clampClass?: string;
}

export default function ExpandableDescription({
  description,
  label,
  seeMoreText,
  seeLessText,
  clampClass = 'line-clamp-4',
}: Props) {
  const [expanded, setExpanded] = useState(false);
  const [truncated, setTruncated] = useState(false);
  const ref = useRef<HTMLParagraphElement>(null);

  useEffect(() => {
    if (ref.current) setTruncated(ref.current.scrollHeight > ref.current.clientHeight);
  }, [description]);

  return (
    <div>
      {label && <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">{label}</p>}
      <button
        type="button"
        onClick={() => (truncated || expanded) && setExpanded(e => !e)}
        className="w-full text-left group"
      >
        <p ref={ref} className={`text-sm text-gray-700 dark:text-gray-300 leading-relaxed ${expanded ? '' : clampClass}`}>
          {description}
        </p>
        {(truncated || expanded) && (
          <span className="inline-flex items-center gap-1 text-xs text-amber-600 dark:text-amber-400 mt-1 group-hover:underline">
            {expanded
              ? <><ChevronUp size={12} />{seeLessText}</>
              : <><ChevronDown size={12} />{seeMoreText}</>
            }
          </span>
        )}
      </button>
    </div>
  );
}
