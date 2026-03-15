import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';

export default function AppLayout() {
  const [settingsOpen, setSettingsOpen] = useState(false);

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117]">
      {/* Desktop sidebar */}
      <div className="hidden md:block">
        <Sidebar onOpenSettings={() => setSettingsOpen(true)} />
      </div>

      {/* Mobile top bar */}
      <MobileTopBar onOpenSettings={() => setSettingsOpen(true)} />

      {/* Main content */}
      <main className="md:ml-60 min-h-screen pb-20 md:pb-0">
        <Outlet />
      </main>

      {/* Settings panel */}
      {settingsOpen && <SettingsPanel onClose={() => setSettingsOpen(false)} />}
    </div>
  );
}
