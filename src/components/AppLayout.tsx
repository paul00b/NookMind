import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';
import InstallPromptSheet from './InstallPromptSheet';

const STORAGE_KEY = 'bm-install-prompted';
const isStandalone = window.matchMedia('(display-mode: standalone)').matches ||
  (window.navigator as unknown as { standalone?: boolean }).standalone === true;
const isMobile = window.innerWidth < 768;

export default function AppLayout() {
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [installSheetOpen, setInstallSheetOpen] = useState(false);

  useEffect(() => {
    if (isStandalone || !isMobile || localStorage.getItem(STORAGE_KEY)) return;
    const timer = setTimeout(() => setInstallSheetOpen(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  const handleDismiss = () => {
    setInstallSheetOpen(false);
    localStorage.setItem(STORAGE_KEY, '1');
  };

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117]">
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
      {installSheetOpen && <InstallPromptSheet onDismiss={handleDismiss} />}
    </div>
  );
}
