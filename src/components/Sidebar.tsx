import { NavLink } from 'react-router-dom';
import { Home, Library, Compass, Settings } from 'lucide-react';
import Avatar from './Avatar';
import { useAuth } from '../context/AuthContext';

interface Props {
  onOpenSettings: () => void;
}

const NAV = [
  { to: '/', label: 'Home', icon: <Home size={18} />, end: true },
  { to: '/library', label: 'My Library', icon: <Library size={18} />, end: false },
  { to: '/discover', label: 'Discover', icon: <Compass size={18} />, end: false },
];

export default function Sidebar({ onOpenSettings }: Props) {
  const { user } = useAuth();
  const name = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'Reader';

  return (
    <aside className="fixed left-0 top-0 h-full w-60 flex flex-col border-r border-black/[0.08] dark:border-white/[0.08] bg-[#f8f6f1] dark:bg-[#0f1117] z-30 px-4 py-6">
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-3 mb-8">
        <div className="w-8 h-8 bg-amber-500 rounded-xl flex items-center justify-center text-white font-bold text-sm">B</div>
        <span className="font-serif font-bold text-xl text-gray-900 dark:text-gray-100">BookMind</span>
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
