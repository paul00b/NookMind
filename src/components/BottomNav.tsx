import { NavLink, useLocation } from 'react-router-dom';
import { Home, Library, Compass } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function BottomNav() {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  if (pathname === '/login') return null;

  const TABS = [
    { to: '/', label: t('nav.home'), icon: Home, end: true },
    { to: '/library', label: t('nav.library'), icon: Library, end: false },
    { to: '/discover', label: t('nav.discover'), icon: Compass, end: false },
  ];

  return (
    <nav className="fixed bottom-4 left-4 right-4 z-30 md:hidden">
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
