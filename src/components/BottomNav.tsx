import { NavLink } from 'react-router-dom';
import { Home, Library, Compass } from 'lucide-react';

const TABS = [
  { to: '/', label: 'Home', icon: Home, end: true },
  { to: '/library', label: 'Library', icon: Library, end: false },
  { to: '/discover', label: 'Discover', icon: Compass, end: false },
];

export default function BottomNav() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-30 md:hidden bg-white/90 dark:bg-[#1a1f2e]/90 backdrop-blur-xl border-t border-black/[0.08] dark:border-white/[0.08]">
      <div className="flex items-center">
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
      {/* Safe area spacer for iOS */}
      <div className="h-safe-area-inset-bottom" />
    </nav>
  );
}
