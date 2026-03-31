# Mode Background Ambiance — Design Spec

**Date:** 2026-03-31
**Status:** Approved

---

## Overview

Add a subtle per-mode background ambiance to visually distinguish the three media modes (Books, Movies, Series). The effect follows the active mode globally — all pages within a mode share the same background treatment.

The effect consists of two layers:
1. A radial gradient glow from the top-center of the screen
2. A light noise/grain texture over the entire background

---

## Color Palette

| Mode | Color | Hex | Rationale |
|------|-------|-----|-----------|
| Books | Amber/gold | `#f59e0b` | Matches the existing brand accent; warm, cozy, default mode |
| Movies | Violet/indigo | `#6366f1` | Cinematic, premium, theatrical |
| Series | Teal/emerald | `#14b8a6` | Streaming, modern, episodic |

---

## Visual Effect

### Layer 1 — Radial glow

A fixed `div` positioned behind all content (`z-index: 0`, `pointer-events: none`) covering the full viewport. Contains a radial gradient from the mode color (very low opacity) to transparent, emanating from the top-center:

```
radial-gradient(ellipse 80% 40% at 50% -10%, <color> 0%, transparent 100%)
```

Opacity values:
- Light mode: `opacity-[0.07]` (~7%)
- Dark mode: `opacity-[0.12]` (~12%)

### Layer 2 — Grain texture

An SVG noise pattern applied as a `background-image` on the same fixed `div` (or a sibling), using `mix-blend-mode: overlay`, opacity ~15%.

The SVG is inlined as a `data:` URL — no external asset, no network request:

```
url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E")
```

---

## Architecture

### Where the change lives

Only `src/components/AppLayout.tsx` is modified.

The outer wrapper `div` gains a relative positioning context. A new fixed `div` (the ambiance layer) is inserted as the first child, before the sidebar/topbar/main. It reads `mode` from `useMediaMode()`.

```tsx
// AppLayout.tsx (new imports)
import { useMediaMode } from '../context/MediaModeContext';

// Inside the component
const { mode } = useMediaMode();

// New element, first child of the wrapper div
<div
  aria-hidden="true"
  className={`fixed inset-0 z-0 pointer-events-none transition-all duration-700 ${modeGlowClass(mode)}`}
/>
```

All existing content sits at `z-10` or higher (no existing z-index conflicts — current layout uses no z-index on main content).

### `modeGlowClass` helper

A small pure function in `AppLayout.tsx` (not exported):

```typescript
function modeGlowClass(mode: MediaMode): string {
  const base = 'opacity-[0.07] dark:opacity-[0.12]';
  const colors: Record<MediaMode, string> = {
    books:   'bg-mode-books',
    movies:  'bg-mode-movies',
    series:  'bg-mode-series',
  };
  return `${base} ${colors[mode]}`;
}
```

### CSS (in `src/index.css`)

Three utility classes defining the gradient + grain combination:

```css
.bg-mode-books {
  background-image:
    url("data:image/svg+xml,..."),  /* grain */
    radial-gradient(ellipse 80% 40% at 50% -10%, #f59e0b 0%, transparent 100%);
  background-blend-mode: overlay, normal;
}

.bg-mode-movies {
  background-image:
    url("data:image/svg+xml,..."),
    radial-gradient(ellipse 80% 40% at 50% -10%, #6366f1 0%, transparent 100%);
  background-blend-mode: overlay, normal;
}

.bg-mode-series {
  background-image:
    url("data:image/svg+xml,..."),
    radial-gradient(ellipse 80% 40% at 50% -10%, #14b8a6 0%, transparent 100%);
  background-blend-mode: overlay, normal;
}
```

### Transition

The fixed `div` uses `transition-all duration-700` so the glow fades smoothly when switching modes.

---

## Out of Scope

- Per-page variations within a mode
- Animated or moving glows
- Any change to card/button/text colors per mode
- Sidebar or bottom nav background changes
