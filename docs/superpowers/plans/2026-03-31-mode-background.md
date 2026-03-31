# Mode Background Ambiance — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a subtle per-mode radial glow + grain background to distinguish Books, Movies, and Series modes visually.

**Architecture:** A single fixed `div` (aria-hidden, pointer-events-none) is inserted as the first child of `AppLayout`. It reads the active mode from `useMediaMode()` and applies one of three CSS classes. Each class stacks a grain SVG and a mode-coloured radial gradient as `background-image` layers. The div's opacity is kept very low via Tailwind utility classes.

**Tech Stack:** React 19, Tailwind CSS, plain CSS in `src/index.css`.

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `src/index.css` | Add `.mode-bg-books`, `.mode-bg-movies`, `.mode-bg-series` classes |
| Modify | `src/components/AppLayout.tsx` | Import `useMediaMode`, add ambiance `div`, add `modeGlowClass` helper |

---

## Task 1 — CSS: add mode background classes

**Files:**
- Modify: `src/index.css`

- [ ] **Step 1: Add the three classes inside `@layer components`**

Open `src/index.css`. Inside the existing `@layer components { ... }` block, append the following **after** the last existing class (e.g. after `.nav-link` or whatever ends the block — add before the closing `}`):

```css
  /* ── Mode background ambiance ── */

  /* Shared grain data URL used in all three classes */
  /* grain = feTurbulence fractalNoise SVG, inlined as data: URL */

  .mode-bg-books {
    background-image:
      url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E"),
      radial-gradient(ellipse 80% 40% at 50% -10%, #f59e0b, transparent);
    background-size: 256px 256px, 100% 100%;
    background-repeat: repeat, no-repeat;
  }

  .mode-bg-movies {
    background-image:
      url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E"),
      radial-gradient(ellipse 80% 40% at 50% -10%, #6366f1, transparent);
    background-size: 256px 256px, 100% 100%;
    background-repeat: repeat, no-repeat;
  }

  .mode-bg-series {
    background-image:
      url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E"),
      radial-gradient(ellipse 80% 40% at 50% -10%, #14b8a6, transparent);
    background-size: 256px 256px, 100% 100%;
    background-repeat: repeat, no-repeat;
  }
```

- [ ] **Step 2: Verify the build still passes**

```bash
cd "/Users/Paul/Desktop/Projets Dev/NookMind"
npm run build 2>&1 | tail -8
```

Expected: `✓ built in` with no errors.

- [ ] **Step 3: Commit**

```bash
git add src/index.css
git commit -m "feat: add mode background CSS classes (glow + grain)"
```

---

## Task 2 — AppLayout: add the ambiance div

**Files:**
- Modify: `src/components/AppLayout.tsx`

- [ ] **Step 1: Add the `useMediaMode` import and `modeGlowClass` helper**

Current top of the file:
```tsx
import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';
import InstallPromptSheet from './InstallPromptSheet';
```

Replace with:
```tsx
import { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import MobileTopBar from './MobileTopBar';
import SettingsPanel from './SettingsPanel';
import InstallPromptSheet from './InstallPromptSheet';
import { useMediaMode } from '../context/MediaModeContext';
import type { MediaMode } from '../types';

function modeGlowClass(mode: MediaMode): string {
  const classes: Record<MediaMode, string> = {
    books:  'mode-bg-books',
    movies: 'mode-bg-movies',
    series: 'mode-bg-series',
  };
  return classes[mode];
}
```

- [ ] **Step 2: Read the mode and insert the ambiance div**

Current component opening:
```tsx
export default function AppLayout() {
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [installSheetOpen, setInstallSheetOpen] = useState(false);
```

Replace with:
```tsx
export default function AppLayout() {
  const { mode } = useMediaMode();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [installSheetOpen, setInstallSheetOpen] = useState(false);
```

Then find the outer wrapper div:
```tsx
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117]">
      {/* Desktop sidebar */}
```

Replace with:
```tsx
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117]">
      {/* Mode ambiance background */}
      <div
        aria-hidden="true"
        className={`fixed inset-0 z-0 pointer-events-none transition-opacity duration-700 opacity-[0.08] dark:opacity-[0.14] ${modeGlowClass(mode)}`}
      />
      {/* Desktop sidebar */}
```

- [ ] **Step 3: Verify the build still passes**

```bash
cd "/Users/Paul/Desktop/Projets Dev/NookMind"
npm run build 2>&1 | tail -8
```

Expected: `✓ built in` with no errors.

- [ ] **Step 4: Visual check in browser**

```bash
npm run dev
```

Open the app. Switch between Books / Movies / Series modes. You should see a very subtle coloured glow at the top of the screen transition between amber (books), violet (movies), and teal (series). The effect should be visible but not distracting — if it looks too strong, lower the opacity values in the Tailwind class (`opacity-[0.06]` / `dark:opacity-[0.10]`).

- [ ] **Step 5: Commit**

```bash
git add src/components/AppLayout.tsx
git commit -m "feat: apply mode background ambiance in AppLayout"
```
