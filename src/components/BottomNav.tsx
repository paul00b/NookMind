import { NavLink } from 'react-router-dom';
import { Home, Library, Compass } from 'lucide-react';

const TABS = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/library', label: 'Library', icon: Library, end: false },
  { to: '/discover', label: 'Discover', icon: Compass, end: false },
];

export default function BottomNav() {
  return (
    <nav className="fixed bottom-4 left-1/2 -translate-x-1/2 z-30 md:hidden">
      <div className="flex items-center gap-1 bg-white/80 dark:bg-[#1a1f2e]/80 backdrop-blur-xl border border-black/[0.08] dark:border-white/[0.08] rounded-full px-3 py-2 shadow-lg">
        {TABS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 px-4 py-1.5 rounded-full transition-all duration-200 min-w-[60px] ${
                isActive
                  ? 'bg-amber-500/15 text-amber-600 dark:text-amber-400'
                  : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <Icon size={20} className={isActive ? 'scale-110' : ''} />
                <span className="text-[10px] font-medium">{label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
