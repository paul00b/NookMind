# Onboarding Slides Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 3-slide swipeable onboarding carousel before the login screen for first-time users, with skip/progress/get-started, localStorage persistence, Settings replay, and EN/FR i18n.

**Architecture:** New `/onboarding` route with a scroll-snap carousel. Entry logic in `App.tsx` redirects first-time visitors. A `OnboardingSlide` component renders each slide with gradient backgrounds and floating icons. Settings panel gets a replay button.

**Tech Stack:** React 19, TypeScript, Tailwind CSS, CSS scroll-snap, IntersectionObserver, i18next, React Router DOM v7, Lucide React icons

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `src/pages/Onboarding.tsx` | Carousel container: scroll-snap, slide tracking, skip/get-started nav, progress dots |
| Create | `src/components/OnboardingSlide.tsx` | Single slide: gradient bg, noise texture, floating icons, title/body text |
| Modify | `src/App.tsx` | Add `/onboarding` route, redirect logic for first-time visitors |
| Modify | `src/components/SettingsPanel.tsx` | Add "Replay onboarding" button in About section |
| Modify | `src/i18n/locales/en.ts` | Add `onboarding.*` and `settings.replayOnboarding` keys |
| Modify | `src/i18n/locales/fr.ts` | Add `onboarding.*` and `settings.replayOnboarding` keys |

---

### Task 1: Add i18n Keys

**Files:**
- Modify: `src/i18n/locales/en.ts:166-197` (settings section + new onboarding section)
- Modify: `src/i18n/locales/fr.ts:166-197` (same)

- [ ] **Step 1: Add English onboarding keys**

In `src/i18n/locales/en.ts`, add a new `onboarding` block after the `settings` block, and add `replayOnboarding` inside the `settings` block:

```ts
// Inside the settings block, after notifNotSupported line:
    replayOnboarding: 'Replay onboarding',

// New block after settings closing brace:
  onboarding: {
    slide1Title: 'Track movies, series & books',
    slide1Body: 'Your watchlist, your reading list, all in one place. Free and ads-free, forever.',
    slide2Title: 'Never miss a thing',
    slide2Body: 'IMDB ratings per episode, new episode alerts, cast info, and streaming providers at a glance.',
    slide3Title: 'Discover & collect',
    slide3Body: 'Trending now, upcoming releases. Build collections and track your watching stats.',
    skip: 'Skip',
    getStarted: 'Get Started',
  },
```

- [ ] **Step 2: Add French onboarding keys**

In `src/i18n/locales/fr.ts`, same structure:

```ts
// Inside the settings block, after notifNotSupported line:
    replayOnboarding: 'Revoir la présentation',

// New block after settings closing brace:
  onboarding: {
    slide1Title: 'Suivez vos films, séries et livres',
    slide1Body: 'Votre liste de visionnage, votre liste de lecture, tout au même endroit. Gratuit et sans pub, pour toujours.',
    slide2Title: 'Ne manquez plus rien',
    slide2Body: 'Notes IMDB par épisode, alertes de nouveaux épisodes, casting et plateformes de streaming en un coup d\'œil.',
    slide3Title: 'Découvrez et collectionnez',
    slide3Body: 'Tendances du moment, prochaines sorties. Créez des collections et suivez vos statistiques.',
    skip: 'Passer',
    getStarted: 'Commencer',
  },
```

- [ ] **Step 3: Commit**

```bash
git add src/i18n/locales/en.ts src/i18n/locales/fr.ts
git commit -m "feat(onboarding): add EN/FR i18n keys"
```

---

### Task 2: Create OnboardingSlide Component

**Files:**
- Create: `src/components/OnboardingSlide.tsx`

- [ ] **Step 1: Create the OnboardingSlide component**

This component renders a single full-viewport slide with:
- A gradient background using the app's noise texture pattern
- A slot for the illustration (React node)
- Title and body text
- Scroll-snap alignment

```tsx
import type { ReactNode } from 'react';

interface Props {
  title: string;
  body: string;
  accentColor: string;       // e.g. '#f59e0b'
  gradientColor: string;     // e.g. '#f59e0b22' — used in radial gradient
  illustration: ReactNode;
}

export default function OnboardingSlide({ title, body, gradientColor, illustration }: Props) {
  return (
    <div
      className="w-screen h-[100svh] flex-shrink-0 snap-center flex flex-col items-center justify-center px-8 relative overflow-hidden"
      style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E"), radial-gradient(ellipse 80% 40% at 50% -10%, ${gradientColor}, transparent)`,
        backgroundSize: '256px 256px, 100% 100%',
        backgroundRepeat: 'repeat, no-repeat',
        backgroundColor: '#0f1117',
      }}
    >
      {/* Illustration area */}
      <div className="mb-8">
        {illustration}
      </div>

      {/* Text */}
      <h2 className="text-2xl font-bold text-gray-100 text-center mb-3 max-w-xs">
        {title}
      </h2>
      <p className="text-base text-gray-400 text-center leading-relaxed max-w-[280px]">
        {body}
      </p>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/OnboardingSlide.tsx
git commit -m "feat(onboarding): create OnboardingSlide component"
```

---

### Task 3: Create Onboarding Page

**Files:**
- Create: `src/pages/Onboarding.tsx`

- [ ] **Step 1: Create the Onboarding page**

This is the main page with:
- Horizontal scroll-snap carousel
- Skip button (top-right, all slides)
- Progress dots (bottom-center, pill-shaped, color-matched)
- Get Started button (last slide only, in the CTA area)
- Swipe tracking via IntersectionObserver
- Navigation to `/login` (or `/` if already authenticated)

```tsx
import { useRef, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { Star, Bell, Drama, Satellite } from 'lucide-react';
import OnboardingSlide from '../components/OnboardingSlide';

const ONBOARDING_KEY = 'nookmind_onboarding_completed';

const SLIDES = [
  { accentColor: '#f59e0b', gradientColor: '#f59e0b' },
  { accentColor: '#6366f1', gradientColor: '#6366f1' },
  { accentColor: '#14b8a6', gradientColor: '#14b8a6' },
];

/* ── Illustrations ── */

function Slide1Illustration() {
  return (
    <div className="relative w-40 h-40 flex items-center justify-center">
      {/* Floating background icons */}
      <span className="absolute top-0 left-0 text-3xl opacity-15 animate-[float1_6s_ease-in-out_infinite]">🎬</span>
      <span className="absolute top-4 right-0 text-2xl opacity-12 animate-[float2_7s_ease-in-out_infinite]">📺</span>
      <span className="absolute bottom-2 left-4 text-2xl opacity-10 animate-[float3_8s_ease-in-out_infinite]">📖</span>
      {/* Logo */}
      <img src="/logo.svg" alt="NookMind" className="w-20 h-20 drop-shadow-[0_8px_32px_rgba(245,158,11,0.27)]" />
    </div>
  );
}

function Slide2Illustration() {
  const icons = [
    { Icon: Star, delay: '0s' },
    { Icon: Bell, delay: '1.5s' },
    { Icon: Drama, delay: '3s' },
    { Icon: Satellite, delay: '4.5s' },
  ];
  return (
    <div className="flex gap-5 flex-wrap justify-center">
      {icons.map(({ Icon, delay }, i) => (
        <div
          key={i}
          className="w-14 h-14 rounded-[14px] bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center"
          style={{ animation: `float${(i % 3) + 1} ${6 + i}s ease-in-out ${delay} infinite` }}
        >
          <Icon size={24} className="text-indigo-400" />
        </div>
      ))}
    </div>
  );
}

function Slide3Illustration() {
  return (
    <div className="flex gap-3 items-end">
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/20 to-teal-500/5 border border-teal-500/20" />
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/30 to-teal-500/10 border border-teal-500/30 scale-110" />
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/20 to-teal-500/5 border border-teal-500/20" />
    </div>
  );
}

/* ── Page ── */

export default function Onboarding() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { user } = useAuth();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [activeIndex, setActiveIndex] = useState(0);

  const finish = useCallback(() => {
    localStorage.setItem(ONBOARDING_KEY, 'true');
    navigate(user ? '/' : '/login', { replace: true });
  }, [navigate, user]);

  // Track active slide via IntersectionObserver
  useEffect(() => {
    const container = scrollRef.current;
    if (!container) return;

    const slides = container.children;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const index = Array.from(slides).indexOf(entry.target as HTMLElement);
            if (index >= 0) setActiveIndex(index);
          }
        });
      },
      { root: container, threshold: 0.5 }
    );

    Array.from(slides).forEach((slide) => observer.observe(slide));
    return () => observer.disconnect();
  }, []);

  const illustrations = [<Slide1Illustration />, <Slide2Illustration />, <Slide3Illustration />];
  const titleKeys = ['onboarding.slide1Title', 'onboarding.slide2Title', 'onboarding.slide3Title'] as const;
  const bodyKeys = ['onboarding.slide1Body', 'onboarding.slide2Body', 'onboarding.slide3Body'] as const;

  return (
    <div className="relative w-screen h-[100svh] overflow-hidden bg-[#0f1117]">
      {/* Skip button */}
      <button
        onClick={finish}
        className="absolute top-4 right-4 z-20 text-sm text-gray-500 hover:text-gray-300 transition-colors px-3 py-1.5"
        style={{ paddingTop: 'calc(0.375rem + env(safe-area-inset-top))' }}
      >
        {t('onboarding.skip')}
      </button>

      {/* Carousel */}
      <div
        ref={scrollRef}
        className="flex w-full h-full overflow-x-auto snap-x snap-mandatory scrollbar-none"
        style={{ scrollbarWidth: 'none' }}
      >
        {SLIDES.map((slide, i) => (
          <OnboardingSlide
            key={i}
            title={t(titleKeys[i])}
            body={t(bodyKeys[i])}
            accentColor={slide.accentColor}
            gradientColor={slide.gradientColor}
            illustration={illustrations[i]}
          />
        ))}
      </div>

      {/* Bottom area: progress dots + Get Started */}
      <div className="absolute bottom-0 left-0 right-0 z-20 flex flex-col items-center gap-5 pb-12">
        {/* Get Started button — only on last slide */}
        {activeIndex === SLIDES.length - 1 && (
          <button
            onClick={finish}
            className="px-8 py-3 rounded-full font-semibold text-white text-sm shadow-[0_4px_20px_rgba(20,184,166,0.27)]"
            style={{ background: 'linear-gradient(135deg, #14b8a6, #0d9488)' }}
          >
            {t('onboarding.getStarted')}
          </button>
        )}

        {/* Progress dots */}
        <div className="flex gap-1.5">
          {SLIDES.map((slide, i) => (
            <div
              key={i}
              className="h-1.5 rounded-full transition-all duration-300"
              style={{
                width: activeIndex === i ? 20 : 6,
                backgroundColor: activeIndex === i ? slide.accentColor : '#4b5563',
              }}
            />
          ))}
        </div>
      </div>

      {/* Floating icon keyframes */}
      <style>{`
        @keyframes float1 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(4px, -6px); }
        }
        @keyframes float2 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(-5px, 5px); }
        }
        @keyframes float3 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(3px, 4px); }
        }
      `}</style>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/pages/Onboarding.tsx
git commit -m "feat(onboarding): create Onboarding page with swipe carousel"
```

---

### Task 4: Wire Up Routing

**Files:**
- Modify: `src/App.tsx`

- [ ] **Step 1: Add the onboarding route and redirect wrapper**

In `src/App.tsx`:

1. Import `Onboarding` and add a helper component `OnboardingGate` that checks localStorage:

```tsx
// Add import at the top:
import Onboarding from './pages/Onboarding';

// Add this component before the App function:
function OnboardingGate() {
  const onboardingDone = localStorage.getItem('nookmind_onboarding_completed') === 'true';
  if (!onboardingDone) return <Navigate to="/onboarding" replace />;
  return <Login />;
}
```

2. Add the `/onboarding` route and replace the `/login` element:

```tsx
// In the <Routes> block, add before the /login route:
<Route path="/onboarding" element={<Onboarding />} />

// Change the /login route from:
<Route path="/login" element={<Login />} />
// To:
<Route path="/login" element={<OnboardingGate />} />
```

This way:
- `/onboarding` always renders the onboarding page
- `/login` redirects to `/onboarding` if not completed, otherwise shows login
- `ProtectedRoute` already redirects unauthenticated users to `/login`, which then checks onboarding

- [ ] **Step 2: Verify the app starts without errors**

Run: `npm run dev`

Open in browser. You should be redirected to `/onboarding` on first visit. After swiping through and clicking "Get Started", you should land on `/login`.

- [ ] **Step 3: Commit**

```bash
git add src/App.tsx
git commit -m "feat(onboarding): wire up /onboarding route with first-visit redirect"
```

---

### Task 5: Add Replay in Settings

**Files:**
- Modify: `src/components/SettingsPanel.tsx`

- [ ] **Step 1: Add replay button and navigation**

In `src/components/SettingsPanel.tsx`:

1. Add imports at the top:

```tsx
import { useNavigate } from 'react-router-dom';
import { RotateCcw } from 'lucide-react';
```

Also add `RotateCcw` to the existing lucide-react import line.

2. Inside the component, add the navigate hook:

```tsx
const navigate = useNavigate();
```

3. Add a replay handler:

```tsx
const handleReplayOnboarding = () => {
  localStorage.removeItem('nookmind_onboarding_completed');
  onClose();
  navigate('/onboarding');
};
```

4. In the About section, after the clear cache button's closing `</div>` (line 329), add a new row:

```tsx
<div className="border-t border-black/[0.06] dark:border-white/[0.06] pt-3 mt-1">
  <button
    onClick={handleReplayOnboarding}
    className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-black/[0.04] dark:hover:bg-white/[0.04] transition-colors"
  >
    <RotateCcw size={14} />
    {t('settings.replayOnboarding')}
  </button>
</div>
```

- [ ] **Step 2: Test the replay flow**

1. Open the app, complete onboarding, log in
2. Open Settings → About section → click "Replay onboarding"
3. You should navigate to `/onboarding`
4. On the last slide, "Get Started" should take you to `/` (home) since you're already authenticated

- [ ] **Step 3: Commit**

```bash
git add src/components/SettingsPanel.tsx
git commit -m "feat(onboarding): add replay button in settings"
```

---

### Task 6: Polish & Edge Cases

**Files:**
- Modify: `src/pages/Onboarding.tsx` (if needed)
- Modify: `src/components/OnboardingSlide.tsx` (if needed)

- [ ] **Step 1: Test on mobile viewport**

Open browser dev tools, switch to a mobile viewport (iPhone 14 / Pixel 7). Verify:
- Slides fill the full viewport height (100svh)
- Swipe works smoothly with snap
- Skip button respects safe-area-inset-top
- Progress dots are visible above the safe area
- "Get Started" button appears/disappears correctly when swiping to/from last slide
- No horizontal scrollbar visible

- [ ] **Step 2: Test light mode**

Toggle to light mode. The onboarding always uses a dark background (`#0f1117`) regardless of theme since it's a standalone pre-login experience. Verify it looks correct and there's no flash of light-mode background.

- [ ] **Step 3: Test the full flow end-to-end**

1. Clear localStorage (`localStorage.clear()` in console)
2. Navigate to `/` → should redirect to `/login` → should redirect to `/onboarding`
3. Swipe through all 3 slides, verify progress dots update and color-match
4. Click "Get Started" → should go to `/login`
5. Log in → should go to `/`
6. Open Settings → "Replay onboarding" → should go to `/onboarding`
7. Click "Get Started" → should go to `/` (already authenticated)
8. Click "Skip" on any slide → same behavior as "Get Started"

- [ ] **Step 4: Fix any issues found during testing**

Address any visual or functional issues discovered in steps 1-3.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat(onboarding): polish and edge case fixes"
```
