import { useState, useEffect, useRef } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';
import InstallPromptSheet from './InstallPromptSheet';
import NotificationPromptSheet from './NotificationPromptSheet';
import { useMediaMode } from '../context/MediaModeContext';
import { useNotificationSubscription } from '../hooks/useNotificationSubscription';
import { isNative } from '../lib/platform';
import type { MediaMode } from '../types';

const MODES: MediaMode[] = ['series', 'movies', 'books'];

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
  const { mode, setMode } = useMediaMode();
  const mainRef = useRef<HTMLDivElement>(null);
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);
  const touchBlocked = useRef(false);

  function isInsideHScroll(el: EventTarget | null): boolean {
    let node = el as HTMLElement | null;
    while (node && node !== mainRef.current) {
      const style = window.getComputedStyle(node);
      const overflow = style.overflowX;
      if ((overflow === 'auto' || overflow === 'scroll') && node.scrollWidth > node.clientWidth) {
        return true;
      }
      node = node.parentElement;
    }
    return false;
  }

  const handleTouchStart = (e: React.TouchEvent) => {
    touchBlocked.current = isInsideHScroll(e.target);
    if (touchBlocked.current) return;
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (touchBlocked.current || touchStartX.current === null || touchStartY.current === null || !mainRef.current) return;
    const dx = e.touches[0].clientX - touchStartX.current;
    const dy = e.touches[0].clientY - touchStartY.current;
    if (Math.abs(dx) > Math.abs(dy)) {
      mainRef.current.style.transform = `translateX(${dx * 0.25}px)`;
      mainRef.current.style.transition = 'none';
    }
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchBlocked.current) { touchBlocked.current = false; return; }
    if (touchStartX.current === null || touchStartY.current === null) return;
    const dx = e.changedTouches[0].clientX - touchStartX.current;
    const dy = e.changedTouches[0].clientY - touchStartY.current;
    touchStartX.current = null;
    touchStartY.current = null;

    if (mainRef.current) {
      const el = mainRef.current;
      el.style.transition = 'transform 0.25s ease-out';
      el.style.transform = 'translateX(0px)';
      // Remove transform entirely after transition so it doesn't create a stacking context
      // (which would break z-index of fixed children like sheets vs BottomNav)
      const onEnd = () => { el.style.transform = ''; el.style.transition = ''; el.removeEventListener('transitionend', onEnd); };
      el.addEventListener('transitionend', onEnd);
    }

    if (Math.abs(dx) < 60 || Math.abs(dx) < Math.abs(dy) * 1.5) return;

    const idx = MODES.indexOf(mode);
    const next = MODES[(idx + (dx > 0 ? -1 : 1) + MODES.length) % MODES.length];
    setMode(next);
  };
  const { supported: notifSupported, subscribed, permission } = useNotificationSubscription();

  // Install prompt — mobile only, once
  useEffect(() => {
    if (isNative()) return;                                    // never on native
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
      <main
        className="md:ml-60 min-h-screen pb-36 md:pb-0"
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        <div ref={mainRef}>
          <Outlet />
        </div>
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
