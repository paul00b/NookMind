import { NavLink } from 'react-router-dom';
import { Home, Library, Compass, Settings, BookOpen, Film, Tv } from 'lucide-react';
import Avatar from './Avatar';
import { useAuth } from '../context/AuthContext';
import { useMediaMode } from '../context/MediaModeContext';
import { useTranslation } from 'react-i18next';

interface Props {
  onOpenSettings: () => void;
}

export default function Sidebar({ onOpenSettings }: Props) {
  const { user } = useAuth();
  const { mode, setMode } = useMediaMode();
  const { t } = useTranslation();

  const NAV = [
    { to: '/', label: t('nav.home'), icon: <Home size={18} />, end: true },
    { to: '/library', label: t('nav.library'), icon: <Library size={18} />, end: false },
    { to: '/discover', label: t('nav.discover'), icon: <Compass size={18} />, end: false },
  ];
  const name = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'Reader';

  return (
    <aside className="fixed left-0 top-0 h-full w-60 flex flex-col border-r border-black/[0.08] dark:border-white/[0.08] bg-[#f8f6f1] dark:bg-[#0f1117] z-30 px-4 py-6">
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-3 mb-6">
        <img src="/logo.svg" className="w-8 h-8" alt="" />
        <span className="font-serif font-bold text-xl text-gray-900 dark:text-gray-100">NookMind</span>
      </div>

      {/* Books / Movies toggle */}
      <div className="flex items-center bg-gray-100 dark:bg-gray-800/60 rounded-xl p-1 gap-0.5 mb-6">
        <button
          onClick={() => setMode('books')}
          className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-semibold transition-all ${
            mode === 'books'
              ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
        >
          <BookOpen size={13} />
          {t('nav.books')}
        </button>
        <button
          onClick={() => setMode('movies')}
          className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-semibold transition-all ${
            mode === 'movies'
              ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
        >
          <Film size={13} />
          {t('nav.movies')}
        </button>
        <button
          onClick={() => setMode('series')}
          className={`flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-semibold transition-all ${
            mode === 'series'
              ? 'bg-white dark:bg-[#1a1f2e] text-amber-600 dark:text-amber-400 shadow-sm'
              : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
          }`}
        >
          <Tv size={13} />
          {t('nav.series')}
        </button>
      </div>

      {/* Nav */}
      <nav className="flex-1 space-y-1">
        {NAV.map(({ to, label, icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            {icon}
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Bottom: user + settings */}
      <div className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-black/[0.04] dark:hover:bg-white/[0.04] transition-colors group cursor-pointer" onClick={onOpenSettings}>
        <Avatar name={name} size="sm" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{name}</p>
          <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user?.email}</p>
        </div>
        <Settings size={15} className="text-gray-400 group-hover:text-amber-500 transition-colors flex-shrink-0" />
      </div>
    </aside>
  );
}
