import { X, Sun, Moon, Monitor } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import type { ThemeMode } from '../types';
import Avatar from './Avatar';
import { useState } from 'react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

interface Props {
  onClose: () => void;
}

const APP_VERSION = '1.0.0';

export default function SettingsPanel({ onClose }: Props) {
  const { user, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const { t } = useTranslation();
  const [displayName, setDisplayName] = useState(
    user?.user_metadata?.full_name || user?.email?.split('@')[0] || 'Reader'
  );
  const [editingName, setEditingName] = useState(false);

  const email = user?.email || '';
  const name = displayName || email.split('@')[0] || 'Reader';

  const handleSaveName = () => {
    setEditingName(false);
    toast.success(t('settings.displayNameSaved'));
  };

  const themes: { value: ThemeMode; label: string; icon: React.ReactNode }[] = [
    { value: 'light', label: t('settings.light'), icon: <Sun size={14} /> },
    { value: 'dark', label: t('settings.dark'), icon: <Moon size={14} /> },
    { value: 'system', label: t('settings.system'), icon: <Monitor size={14} /> },
  ];

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      {/* Panel */}
      <div className="relative z-10 w-full max-w-sm h-full bg-[#f8f6f1] dark:bg-[#1a1f2e] border-l border-black/[0.08] dark:border-white/[0.08] overflow-y-auto animate-slide-in-right flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]">
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('settings.title')}</h2>
          <button onClick={onClose} className="btn-ghost p-2"><X size={18} /></button>
        </div>

        <div className="flex-1 p-6 space-y-8">
          {/* Profile */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.profile')}</h3>
            <div className="flex flex-col items-center gap-4 card p-5">
              <Avatar name={name} size="lg" />

              {editingName ? (
                <div className="w-full space-y-2">
                  <input
                    className="input text-center text-sm"
                    value={displayName}
                    onChange={e => setDisplayName(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleSaveName()}
                    autoFocus
                  />
                  <div className="flex gap-2">
                    <button onClick={handleSaveName} className="btn-primary text-sm py-1.5 flex-1">{t('settings.save')}</button>
                    <button onClick={() => setEditingName(false)} className="btn-ghost text-sm py-1.5 flex-1">{t('settings.cancel')}</button>
                  </div>
                </div>
              ) : (
                <div className="text-center">
                  <button
                    onClick={() => setEditingName(true)}
                    className="font-semibold text-gray-900 dark:text-gray-100 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
                  >
                    {name}
                  </button>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">{email}</p>
                </div>
              )}

              <button
                onClick={async () => { await signOut(); onClose(); }}
                className="w-full btn-ghost text-sm text-red-500 hover:bg-red-500/10 hover:text-red-600"
              >
                {t('settings.signOut')}
              </button>
            </div>
          </section>

          {/* Appearance */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.appearance')}</h3>
            <div className="card p-4">
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">{t('settings.theme')}</p>
              <div className="flex gap-2">
                {themes.map(t => (
                  <button
                    key={t.value}
                    onClick={() => setTheme(t.value)}
                    className={`flex-1 flex flex-col items-center gap-1.5 py-3 rounded-xl text-xs font-medium border transition-all ${
                      theme === t.value
                        ? 'bg-amber-500/10 border-amber-500 text-amber-600 dark:text-amber-400'
                        : 'bg-transparent border-black/[0.08] dark:border-white/[0.08] text-gray-600 dark:text-gray-400 hover:border-amber-500/40'
                    }`}
                  >
                    {t.icon}
                    {t.label}
                  </button>
                ))}
              </div>
            </div>
          </section>

          {/* About */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.about')}</h3>
            <div className="card p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600 dark:text-gray-400">{t('settings.version')}</span>
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{APP_VERSION}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600 dark:text-gray-400">{t('settings.app')}</span>
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">BookMind</span>
              </div>
              <p className="text-xs text-gray-400 dark:text-gray-500 pt-1 text-center italic">
                {t('settings.tagline')}
              </p>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
