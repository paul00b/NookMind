import { useState, useMemo, type ReactNode } from 'react';
import { X, Search, Check } from 'lucide-react';
import SheetModal, { SheetCloseButton } from './SheetModal';

export interface PickerItem {
  id: string;
  title: string;
  subtitle: string;
  imageUrl?: string | null;
}

interface Config {
  header: string;
  searchPlaceholder: string;
  noItemsFound: string;
  alreadyInCategory: string;
  cancel: string;
  confirmLabel: (newCount: number) => string;
  fallbackIcon: ReactNode;
}

interface Props {
  existingIds: string[];
  items: PickerItem[];
  config: Config;
  onConfirm: (newIds: string[]) => void;
  onClose: () => void;
}

export default function CategoryItemPickerModal({ existingIds, items, config, onConfirm, onClose }: Props) {
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set(existingIds));

  const filtered = useMemo(() => {
    const q = query.toLowerCase();
    return items.filter(item =>
      !q || item.title.toLowerCase().includes(q) || item.subtitle.toLowerCase().includes(q)
    );
  }, [items, query]);

  const toggle = (id: string) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const newCount = [...selected].filter(id => !existingIds.includes(id)).length;

  const handleConfirm = () => {
    onConfirm([...selected].filter(id => !existingIds.includes(id)));
  };

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-lg card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[85vh] flex flex-col"
    >
      <div className="flex items-center justify-between p-5 border-b border-black/[0.06] dark:border-white/[0.06] flex-shrink-0">
        <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">{config.header}</h2>
        <SheetCloseButton className="btn-ghost p-2"><X size={18} /></SheetCloseButton>
      </div>

      <div className="px-5 py-3 flex-shrink-0">
        <div className="relative">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder={config.searchPlaceholder}
            className="input pl-9 text-sm py-2"
            autoFocus
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-3 pb-3">
        {filtered.length === 0 ? (
          <p className="text-center text-sm text-gray-400 py-10">{config.noItemsFound}</p>
        ) : (
          filtered.map(item => {
            const isSelected = selected.has(item.id);
            const alreadyIn = existingIds.includes(item.id);
            return (
              <button
                key={item.id}
                onClick={() => toggle(item.id)}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors text-left ${
                  isSelected ? 'bg-amber-500/10 dark:bg-amber-500/15' : 'hover:bg-gray-50 dark:hover:bg-white/5'
                }`}
              >
                <div className="w-9 h-12 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800">
                  {item.imageUrl ? (
                    <img src={item.imageUrl} alt={item.title} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      {config.fallbackIcon}
                    </div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{item.title}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{item.subtitle}</p>
                </div>
                {alreadyIn ? (
                  <span className="flex-shrink-0 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                    {config.alreadyInCategory}
                  </span>
                ) : (
                  <div className={`w-5 h-5 rounded-md border-2 flex-shrink-0 flex items-center justify-center transition-colors ${
                    isSelected ? 'bg-amber-500 border-amber-500' : 'border-gray-300 dark:border-gray-600'
                  }`}>
                    {isSelected && <Check size={12} className="text-white" strokeWidth={3} />}
                  </div>
                )}
              </button>
            );
          })
        )}
      </div>

      <div className="p-5 border-t border-black/[0.06] dark:border-white/[0.06] flex gap-3 flex-shrink-0">
        <button onClick={onClose} className="btn-ghost flex-1 text-sm">{config.cancel}</button>
        <button
          onClick={handleConfirm}
          disabled={newCount === 0}
          className="btn-primary flex-1 text-sm disabled:opacity-40"
        >
          {config.confirmLabel(newCount)}
        </button>
      </div>
    </SheetModal>
  );
}
