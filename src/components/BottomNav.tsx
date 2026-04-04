import { useLocation, useNavigate } from 'react-router-dom';
import { Search, Library, Compass, BookOpen, Film, Tv } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useMediaMode } from '../context/MediaModeContext';

export default function BottomNav() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { mode, setMode } = useMediaMode();
  if (pathname === '/login') return null;

  const TABS = [
    { to: '/', label: t('nav.home'), icon: Search, end: true },
    { to: '/library', label: t('nav.library'), icon: Library, end: false },
    { to: '/discover', label: t('nav.discover'), icon: Compass, end: false },
  ];

  return (
    <nav data-mobile-bottom-nav className="fixed left-4 right-4 z-30 md:hidden flex flex-col gap-2" style={{ bottom: 'calc(16px + env(safe-area-inset-bottom))' }}>
      {/* Books / Movies toggle pill */}
      <div className="flex items-center justify-center">
        <div className="flex items-center bg-white/85 dark:bg-[#1a1f2e]/85 backdrop-blur-xl border border-black/[0.08] dark:border-white/[0.08] rounded-full shadow-md p-1 gap-0.5">
          <button
            onPointerDown={(e) => { e.preventDefault(); setMode('books'); }}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all touch-manipulation ${
              mode === 'books'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <BookOpen size={12} />
            {t('nav.books')}
          </button>
          <button
            onPointerDown={(e) => { e.preventDefault(); setMode('movies'); }}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all touch-manipulation ${
              mode === 'movies'
                ? 'bg-indigo-500 text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <Film size={12} />
            {t('nav.movies')}
          </button>
          <button
            onPointerDown={(e) => { e.preventDefault(); setMode('series'); }}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all touch-manipulation ${
              mode === 'series'
                ? 'bg-teal-500 text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <Tv size={12} />
            {t('nav.series')}
          </button>
        </div>
      </div>

      {/* Main nav tabs */}
      <div className="flex items-center bg-white/85 dark:bg-[#1a1f2e]/85 backdrop-blur-xl border border-black/[0.08] dark:border-white/[0.08] rounded-full shadow-lg overflow-hidden">
        {TABS.map(({ to, label, icon: Icon, end }) => {
          const isActive = end ? pathname === to : pathname.startsWith(to);
          return (
            <div
              key={to}
              role="link"
              onPointerDown={(e) => { e.preventDefault(); navigate(to); }}
              className={`flex-1 flex flex-col items-center gap-1 py-3 transition-all duration-200 touch-manipulation cursor-pointer ${
                isActive
                  ? 'text-amber-600 dark:text-amber-400'
                  : 'text-gray-500 dark:text-gray-400'
              }`}
            >
              <Icon size={22} className={isActive ? 'scale-110 transition-transform' : 'transition-transform'} />
              <span className="text-[10px] font-medium">{label}</span>
            </div>
          );
        })}
      </div>
    </nav>
  );
}
