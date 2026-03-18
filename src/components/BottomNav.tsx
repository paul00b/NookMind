import { NavLink, useLocation } from 'react-router-dom';
import { Home, Library, Compass, BookOpen, Film } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useMediaMode } from '../context/MediaModeContext';

export default function BottomNav() {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  const { mode, setMode } = useMediaMode();
  if (pathname === '/login') return null;

  const TABS = [
    { to: '/', label: t('nav.home'), icon: Home, end: true },
    { to: '/library', label: t('nav.library'), icon: Library, end: false },
    { to: '/discover', label: t('nav.discover'), icon: Compass, end: false },
  ];

  return (
    <nav className="fixed bottom-4 left-4 right-4 z-30 md:hidden flex flex-col gap-2">
      {/* Books / Movies toggle pill */}
      <div className="flex items-center justify-center">
        <div className="flex items-center bg-white/85 dark:bg-[#1a1f2e]/85 backdrop-blur-xl border border-black/[0.08] dark:border-white/[0.08] rounded-full shadow-md p-1 gap-0.5">
          <button
            onClick={() => setMode('books')}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all ${
              mode === 'books'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <BookOpen size={12} />
            {t('nav.books')}
          </button>
          <button
            onClick={() => setMode('movies')}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all ${
              mode === 'movies'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            <Film size={12} />
            {t('nav.movies')}
          </button>
        </div>
      </div>

      {/* Main nav tabs */}
      <div className="flex items-center bg-white/85 dark:bg-[#1a1f2e]/85 backdrop-blur-xl border border-black/[0.08] dark:border-white/[0.08] rounded-full shadow-lg overflow-hidden">
        {TABS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex-1 flex flex-col items-center gap-1 py-3 transition-all duration-200 ${
                isActive
                  ? 'text-amber-600 dark:text-amber-400'
                  : 'text-gray-500 dark:text-gray-400'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon size={22} className={isActive ? 'scale-110 transition-transform' : 'transition-transform'} />
                <span className="text-[10px] font-medium">{label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
