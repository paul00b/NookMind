import { X, Sun, Moon, Monitor, RefreshCw, RotateCcw, Bell, BellOff, Tv, Film, Clapperboard, Send, Popcorn } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import type { ThemeMode } from '../types';
import Avatar from './Avatar';
import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useNotificationSubscription } from '../hooks/useNotificationSubscription';
import { getPopcornLauncherTemplate, setPopcornLauncherTemplate } from '../lib/popcorn';

interface Props {
  onClose: () => void;
}

const APP_VERSION = '1.0.0';

function Toggle({ checked, onChange }: { checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
        checked ? 'bg-teal-500' : 'bg-gray-300 dark:bg-gray-600'
      }`}
    >
      <span
        className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white shadow transition-transform ${
          checked ? 'translate-x-4' : 'translate-x-1'
        }`}
      />
    </button>
  );
}

export default function SettingsPanel({ onClose }: Props) {
  const { user, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const {
    supported: notifSupported,
    permission,
    subscribed,
    loading: notifLoading,
    preferences,
    subscribe,
    unsubscribe,
    updatePreferences,
    sendTestNotification,
  } = useNotificationSubscription();

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previousOverflow; };
  }, []);

  const [refreshing, setRefreshing] = useState(false);
  const [testNotifState, setTestNotifState] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [testNotifMessage, setTestNotifMessage] = useState('');
  const [popcornTemplate, setPopcornTemplateState] = useState(() => getPopcornLauncherTemplate());

  const handleClearCache = () => {
    setRefreshing(true);
    sessionStorage.clear();
    setTimeout(() => {
      setRefreshing(false);
      toast.success(t('settings.cacheCleared'));
    }, 800);
  };

  const handleReplayOnboarding = () => {
    localStorage.removeItem('nookmind_onboarding_completed');
    onClose();
    navigate('/onboarding');
  };

  const handleToggleNotifications = async () => {
    if (subscribed) {
      await unsubscribe();
      toast.success(t('settings.notifDisabled'));
    } else {
      const ok = await subscribe();
      if (ok) toast.success(t('settings.notifEnabled'));
      else if (permission === 'denied') toast.error(t('settings.notifDenied'));
    }
  };

  const handleTestNotifications = async () => {
    setTestNotifState('loading');
    setTestNotifMessage('Envoi du test en cours...');

    const result = await sendTestNotification();
    setTestNotifState(result.ok ? 'success' : 'error');
    setTestNotifMessage(result.message);
  };

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

  const handleSavePopcornTemplate = () => {
    setPopcornLauncherTemplate(popcornTemplate);
    toast.success(t('settings.popcornSaved'));
  };

  const handleClearPopcornTemplate = () => {
    setPopcornTemplateState('');
    setPopcornLauncherTemplate('');
    toast.success(t('settings.popcornCleared'));
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
        <div className="flex items-center justify-between px-6 pb-4 border-b border-black/[0.06] dark:border-white/[0.06]" style={{ paddingTop: 'calc(1.5rem + env(safe-area-inset-top))' }}>
          <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">{t('settings.title')}</h2>
          <button onClick={onClose} className="btn-ghost p-2"><X size={18} /></button>
        </div>

        <div className="flex-1 p-6 space-y-8">
          {/* Profile */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.profile')}</h3>
            <div className="flex flex-col items-center gap-4 card p-5">
              <Avatar name={name} size="lg" imageUrl={user?.user_metadata?.avatar_url} />

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

          {/* Notifications */}
          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.notifications')}</h3>
            <div className="card p-4 space-y-4">
              {!notifSupported ? (
                <p className="text-sm text-gray-500 dark:text-gray-400 text-center py-2">
                  {t('settings.notifNotSupported')}
                </p>
              ) : permission === 'denied' ? (
                <div className="flex items-start gap-3">
                  <BellOff size={16} className="text-red-400 mt-0.5 shrink-0" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">{t('settings.notifDenied')}</p>
                </div>
              ) : (
                <>
                  {/* Enable / Disable toggle */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <Bell size={15} className={subscribed ? 'text-teal-500' : 'text-gray-400'} />
                      <span className="text-sm font-medium text-gray-800 dark:text-gray-200">
                        {subscribed ? t('settings.notifActive') : t('settings.enableNotifications')}
                      </span>
                    </div>
                    <button
                      onClick={handleToggleNotifications}
                      disabled={notifLoading}
                      className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all disabled:opacity-60 ${
                        subscribed
                          ? 'bg-teal-500/10 text-teal-600 dark:text-teal-400 hover:bg-teal-500/20'
                          : 'bg-teal-500 text-white hover:bg-teal-600 shadow-sm'
                      }`}
                    >
                      {notifLoading ? '…' : subscribed ? t('settings.notifDeactivate') : t('settings.notifActivate')}
                    </button>
                  </div>

                  {/* Per-type toggles (only when subscribed) */}
                  {subscribed && (
                    <div className="space-y-3 pt-1 border-t border-black/[0.06] dark:border-white/[0.06]">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <Tv size={14} className="text-teal-500" />
                          <span className="text-sm text-gray-700 dark:text-gray-300">{t('settings.notifEpisodes')}</span>
                        </div>
                        <Toggle
                          checked={preferences.notify_episodes}
                          onChange={(v) => void updatePreferences({ notify_episodes: v })}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <Clapperboard size={14} className="text-teal-500" />
                          <span className="text-sm text-gray-700 dark:text-gray-300">{t('settings.notifSeasons')}</span>
                        </div>
                        <Toggle
                          checked={preferences.notify_seasons}
                          onChange={(v) => void updatePreferences({ notify_seasons: v })}
                        />
                      </div>
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2.5">
                          <Film size={14} className="text-teal-500" />
                          <span className="text-sm text-gray-700 dark:text-gray-300">{t('settings.notifMovies')}</span>
                        </div>
                        <Toggle
                          checked={preferences.notify_movies}
                          onChange={(v) => void updatePreferences({ notify_movies: v })}
                        />
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>

            {subscribed && (
              <div className="mt-3 space-y-2">
                <button
                  onClick={() => { void handleTestNotifications(); }}
                  disabled={notifLoading || testNotifState === 'loading'}
                  className={`w-full flex items-center justify-center gap-2 rounded-xl px-3 py-3 text-sm font-semibold transition-all disabled:opacity-60 ${
                    testNotifState === 'success'
                      ? 'bg-teal-500/10 text-teal-700 dark:text-teal-300 border border-teal-500/30'
                      : testNotifState === 'error'
                        ? 'bg-red-500/10 text-red-700 dark:text-red-300 border border-red-500/30'
                        : 'bg-white dark:bg-[#0f1117] text-gray-800 dark:text-gray-200 border border-black/[0.08] dark:border-white/[0.08] hover:border-teal-500/40 hover:text-teal-600 dark:hover:text-teal-400'
                  }`}
                >
                  <Send size={14} />
                  {testNotifState === 'loading'
                    ? 'Test en cours...'
                    : testNotifState === 'success'
                      ? 'Test envoye'
                      : testNotifState === 'error'
                        ? 'Retester les notifications'
                        : 'Tester les notifications'}
                </button>

                {testNotifState !== 'idle' && (
                  <p
                    className={`text-xs ${
                      testNotifState === 'success'
                        ? 'text-teal-700 dark:text-teal-300'
                        : testNotifState === 'error'
                          ? 'text-red-700 dark:text-red-300'
                          : 'text-gray-500 dark:text-gray-400'
                    }`}
                  >
                    {testNotifMessage}
                  </p>
                )}
              </div>
            )}
          </section>

          <section>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-400 dark:text-gray-500 mb-4">{t('settings.integrations')}</h3>
            <div className="card p-4 space-y-3">
              <div className="flex items-start gap-2.5">
                <Popcorn size={16} className="text-amber-500 mt-0.5 shrink-0" />
                <div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{t('settings.popcornTitle')}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{t('settings.popcornHelp')}</p>
                </div>
              </div>

              <input
                className="input text-sm"
                value={popcornTemplate}
                onChange={e => setPopcornTemplateState(e.target.value)}
                placeholder={t('settings.popcornPlaceholder')}
              />

              <p className="text-[11px] text-gray-400 dark:text-gray-500">
                {t('settings.popcornTokens')}
              </p>

              <div className="flex gap-2">
                <button onClick={handleSavePopcornTemplate} className="btn-primary text-sm py-2 flex-1">
                  {t('settings.save')}
                </button>
                <button onClick={handleClearPopcornTemplate} className="btn-ghost text-sm py-2 flex-1">
                  {t('settings.popcornReset')}
                </button>
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
                <span className="text-sm font-medium text-gray-900 dark:text-gray-100">NookMind</span>
              </div>
              <p className="text-xs text-gray-400 dark:text-gray-500 pt-1 text-center italic">
                {t('settings.tagline')}
              </p>
              <div className="border-t border-black/[0.06] dark:border-white/[0.06] pt-3 mt-1">
                <button
                  onClick={handleClearCache}
                  disabled={refreshing}
                  className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-black/[0.04] dark:hover:bg-white/[0.04] transition-colors disabled:opacity-60"
                >
                  <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
                  {refreshing ? t('settings.cacheRefreshing') : t('settings.clearCache')}
                </button>
              </div>
              <div className="border-t border-black/[0.06] dark:border-white/[0.06] pt-3 mt-1">
                <button
                  onClick={handleReplayOnboarding}
                  className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-black/[0.04] dark:hover:bg-white/[0.04] transition-colors"
                >
                  <RotateCcw size={14} />
                  {t('settings.replayOnboarding')}
                </button>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
