import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';
import InstallPromptSheet from './InstallPromptSheet';
import NotificationPromptSheet from './NotificationPromptSheet';
import { useMediaMode } from '../context/MediaModeContext';
import { useNotificationSubscription } from '../hooks/useNotificationSubscription';
import type { MediaMode } from '../types';

function modeGlowClass(mode: MediaMode): string {
  const classes: Record<MediaMode, string> = {
    books:  'mode-bg-books',
    movies: 'mode-bg-movies',
    series: 'mode-bg-series',
  };
  return classes[mode];
}

const INSTALL_STORAGE_KEY = 'bm-install-prompted';
const NOTIF_STORAGE_KEY   = 'bm-notif-prompted';

const isStandalone = window.matchMedia('(display-mode: standalone)').matches ||
  (window.navigator as unknown as { standalone?: boolean }).standalone === true;
const isMobile = window.innerWidth < 768;

export default function AppLayout() {
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [installSheetOpen, setInstallSheetOpen] = useState(false);
  const [notifSheetOpen, setNotifSheetOpen] = useState(false);
  const { mode } = useMediaMode();
  const { supported: notifSupported, subscribed, permission } = useNotificationSubscription();

  // Install prompt — mobile only, once
  useEffect(() => {
    if (isStandalone || !isMobile || localStorage.getItem(INSTALL_STORAGE_KEY)) return;
    const timer = setTimeout(() => setInstallSheetOpen(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  // Notification prompt — show after install prompt delay, if not already subscribed/denied
  useEffect(() => {
    if (!notifSupported || subscribed || permission === 'denied') return;
    if (localStorage.getItem(NOTIF_STORAGE_KEY)) return;
    // Delay a bit more than install prompt so they don't overlap
    const timer = setTimeout(() => setNotifSheetOpen(true), 5000);
    return () => clearTimeout(timer);
  }, [notifSupported, subscribed, permission]);

  const handleInstallDismiss = () => {
    setInstallSheetOpen(false);
    localStorage.setItem(INSTALL_STORAGE_KEY, '1');
  };

  const handleNotifDismiss = () => {
    setNotifSheetOpen(false);
    localStorage.setItem(NOTIF_STORAGE_KEY, '1');
  };

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117]">
      {/* Mode ambiance background */}
      <div
        aria-hidden="true"
        className={`fixed inset-0 z-0 pointer-events-none transition-opacity duration-700 opacity-25 dark:opacity-30 ${modeGlowClass(mode)}`}
      />
      {/* Desktop sidebar */}
      <div className="hidden md:block">
        <Sidebar onOpenSettings={() => setSettingsOpen(true)} />
      </div>

      {/* Mobile top bar */}
      <MobileTopBar onOpenSettings={() => setSettingsOpen(true)} />

      {/* Main content */}
      <main className="md:ml-60 min-h-screen pb-36 md:pb-0">
        <Outlet />
      </main>

      {/* Settings panel */}
      {settingsOpen && <SettingsPanel onClose={() => setSettingsOpen(false)} />}

      {/* Install prompt */}
      {installSheetOpen && <InstallPromptSheet onDismiss={handleInstallDismiss} />}

      {/* Notification prompt */}
      {notifSheetOpen && <NotificationPromptSheet onDismiss={handleNotifDismiss} />}
    </div>
  );
}
