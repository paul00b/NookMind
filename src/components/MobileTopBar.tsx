import Avatar from './Avatar';
import { useAuth } from '../context/AuthContext';

interface Props {
  onOpenSettings: () => void;
}

export default function MobileTopBar({ onOpenSettings }: Props) {
  const { user } = useAuth();
  const name = user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'Reader';

  return (
    <header className="md:hidden flex items-center justify-between px-4 py-3 border-b border-black/[0.06] dark:border-white/[0.06] bg-[#f8f6f1]/90 dark:bg-[#0f1117]/90 backdrop-blur-sm sticky top-0 z-20">
      <div className="flex items-center gap-2">
        <div className="w-7 h-7 bg-amber-500 rounded-lg flex items-center justify-center text-white font-bold text-xs">B</div>
        <span className="font-serif font-bold text-lg text-gray-900 dark:text-gray-100">BookMind</span>
      </div>
      <button onClick={onOpenSettings} className="rounded-full transition-opacity hover:opacity-80">
        <Avatar name={name} size="sm" />
      </button>
    </header>
  );
}
