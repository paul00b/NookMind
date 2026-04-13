# Streaming Providers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show clickable streaming provider logos (Netflix, Canal+, etc.) on movie and series detail modals, filtered to subscription-only ("flatrate") availability.

**Architecture:** Use TMDB's `/movie/{id}/watch/providers` and `/tv/{id}/watch/providers` endpoints (powered by JustWatch) which return providers grouped by monetization type (`flatrate`, `rent`, `buy`) per country. We fetch providers alongside existing detail fetches, cache them in session storage, and render small clickable logos that deep-link to the provider's app/website. The user's country is derived from the existing `getTmdbLocale()` pattern.

**Tech Stack:** TMDB Watch Providers API, React, TypeScript, Tailwind CSS, i18next

---

## File Structure

| Action | File | Responsibility |
|--------|------|---------------|
| Create | `src/components/WatchProviders.tsx` | Reusable component: renders provider logos as clickable links |
| Modify | `src/lib/tmdb.ts` | Add `fetchMovieWatchProviders()` and `fetchSeriesWatchProviders()` functions |
| Modify | `src/types/index.ts` | Add `WatchProvider` and `WatchProvidersResult` types |
| Modify | `src/components/MovieDetailModal.tsx` | Fetch and display watch providers |
| Modify | `src/components/SeriesDetailModal.tsx` | Fetch and display watch providers |
| Modify | `src/i18n/locales/en.ts` | Add `watchProviders` translation keys |
| Modify | `src/i18n/locales/fr.ts` | Add `watchProviders` translation keys |

---

### Task 1: Add TypeScript Types

**Files:**
- Modify: `src/types/index.ts`

- [ ] **Step 1: Add WatchProvider types to types/index.ts**

Add the following at the end of the file, before the closing of the file:

```typescript
export interface WatchProvider {
  provider_id: number;
  provider_name: string;
  logo_path: string;
}

export interface WatchProvidersResult {
  flatrate: WatchProvider[];
  link: string | null;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/types/index.ts
git commit -m "feat: add WatchProvider types for streaming availability"
```

---

### Task 2: Add TMDB Watch Provider Fetch Functions

**Files:**
- Modify: `src/lib/tmdb.ts`

- [ ] **Step 1: Add the country code helper and fetch functions to tmdb.ts**

Add the following function after the existing `getTmdbLocale()` function (around line 11):

```typescript
function getWatchRegion(): string {
  const lang = typeof navigator !== 'undefined' ? navigator.language : 'en-US';
  if (lang.startsWith('fr')) return 'FR';
  return 'US';
}
```

Then add the following two functions at the end of the file, after `extractSeriesData`:

```typescript
export async function fetchMovieWatchProviders(tmdbId: number): Promise<WatchProvidersResult> {
  const cacheKey = `nookmind_tmdb_movie_wp_${tmdbId}`;
  const cached = getSessionCache<WatchProvidersResult>(cacheKey);
  if (cached) return cached;

  try {
    const res = await fetch(buildUrl(`/movie/${tmdbId}/watch/providers`));
    if (!res.ok) return { flatrate: [], link: null };
    const data = await res.json();
    const region = getWatchRegion();
    const countryData = data.results?.[region];
    const result: WatchProvidersResult = {
      flatrate: countryData?.flatrate ?? [],
      link: countryData?.link ?? null,
    };
    setSessionCache(cacheKey, result);
    return result;
  } catch {
    return { flatrate: [], link: null };
  }
}

export async function fetchSeriesWatchProviders(tmdbId: number): Promise<WatchProvidersResult> {
  const cacheKey = `nookmind_tmdb_series_wp_${tmdbId}`;
  const cached = getSessionCache<WatchProvidersResult>(cacheKey);
  if (cached) return cached;

  try {
    const res = await fetch(buildUrl(`/tv/${tmdbId}/watch/providers`));
    if (!res.ok) return { flatrate: [], link: null };
    const data = await res.json();
    const region = getWatchRegion();
    const countryData = data.results?.[region];
    const result: WatchProvidersResult = {
      flatrate: countryData?.flatrate ?? [],
      link: countryData?.link ?? null,
    };
    setSessionCache(cacheKey, result);
    return result;
  } catch {
    return { flatrate: [], link: null };
  }
}
```

Also add the import at the top of the file:

```typescript
import type { TmdbMovie, TmdbSeries, TmdbSeasonDetails, WatchProvidersResult } from '../types';
```

- [ ] **Step 2: Commit**

```bash
git add src/lib/tmdb.ts
git commit -m "feat: add TMDB watch provider fetch functions with session caching"
```

---

### Task 3: Add i18n Translation Keys

**Files:**
- Modify: `src/i18n/locales/en.ts`
- Modify: `src/i18n/locales/fr.ts`

- [ ] **Step 1: Add English translations**

Add the following section to `en.ts`, after the `seriesCard` section (before the closing `};`):

```typescript
watchProviders: {
  availableOn: 'Available on',
  noProviders: 'Not available in streaming',
},
```

- [ ] **Step 2: Add French translations**

Add the following section to `fr.ts`, after the `seriesCard` section (before the closing `};`):

```typescript
watchProviders: {
  availableOn: 'Disponible sur',
  noProviders: 'Non disponible en streaming',
},
```

- [ ] **Step 3: Commit**

```bash
git add src/i18n/locales/en.ts src/i18n/locales/fr.ts
git commit -m "feat: add watch providers i18n translations (en/fr)"
```

---

### Task 4: Create WatchProviders Component

**Files:**
- Create: `src/components/WatchProviders.tsx`

- [ ] **Step 1: Create the WatchProviders component**

```tsx
import type { WatchProvidersResult } from '../types';
import { useTranslation } from 'react-i18next';

interface Props {
  providers: WatchProvidersResult | null;
  loading?: boolean;
}

export default function WatchProviders({ providers, loading }: Props) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="flex gap-2">
        {[1, 2, 3].map(i => (
          <div key={i} className="w-9 h-9 rounded-lg bg-gray-200 dark:bg-gray-700 animate-pulse" />
        ))}
      </div>
    );
  }

  if (!providers || providers.flatrate.length === 0) return null;

  return (
    <div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">{t('watchProviders.availableOn')}</p>
      <div className="flex flex-wrap gap-2">
        {providers.flatrate.map(provider => (
          <a
            key={provider.provider_id}
            href={providers.link ?? '#'}
            target="_blank"
            rel="noopener noreferrer"
            title={provider.provider_name}
            className="block w-9 h-9 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 hover:scale-110 transition-transform"
          >
            <img
              src={`https://image.tmdb.org/t/p/w92${provider.logo_path}`}
              alt={provider.provider_name}
              className="w-full h-full object-cover"
              loading="lazy"
            />
          </a>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/components/WatchProviders.tsx
git commit -m "feat: create WatchProviders component with clickable logos"
```

---

### Task 5: Integrate Watch Providers into MovieDetailModal

**Files:**
- Modify: `src/components/MovieDetailModal.tsx`

- [ ] **Step 1: Add imports**

Add to the existing imports at the top of `MovieDetailModal.tsx`:

```typescript
import WatchProviders from './WatchProviders';
import { fetchMovieDetails, fetchMovieWatchProviders, getPosterUrl } from '../lib/tmdb';
import type { WatchProvidersResult } from '../types';
```

Remove the old import line:
```typescript
import { fetchMovieDetails, getPosterUrl } from '../lib/tmdb';
```

- [ ] **Step 2: Add state and fetch logic**

Inside the `MovieDetailModal` component, after the `const [tmdbMovie, setTmdbMovie] = useState<TmdbMovie | null>(null);` line, add:

```typescript
const [watchProviders, setWatchProviders] = useState<WatchProvidersResult | null>(null);
const [loadingProviders, setLoadingProviders] = useState(false);
```

Then modify the existing `useEffect` that fetches movie details (the one starting with `if (!movie.tmdb_id) return;`) to also fetch providers. Replace the entire useEffect:

```typescript
useEffect(() => {
  if (!movie.tmdb_id) return;
  let active = true;
  fetchMovieDetails(movie.tmdb_id).then(details => {
    if (active) setTmdbMovie(details);
  });
  setLoadingProviders(true);
  fetchMovieWatchProviders(movie.tmdb_id).then(result => {
    if (active) {
      setWatchProviders(result);
      setLoadingProviders(false);
    }
  });
  return () => { active = false; };
}, [movie.tmdb_id]);
```

- [ ] **Step 3: Add WatchProviders to the JSX**

Insert the `WatchProviders` component in the detail modal. Place it right after the badges/genre section (after the closing `</div>` of the `flex flex-wrap gap-2 text-sm` div, around line 130), inside the `flex-1 min-w-0 space-y-4` div:

```tsx
<WatchProviders providers={watchProviders} loading={loadingProviders} />
```

- [ ] **Step 4: Commit**

```bash
git add src/components/MovieDetailModal.tsx
git commit -m "feat: show streaming providers in movie detail modal"
```

---

### Task 6: Integrate Watch Providers into SeriesDetailModal

**Files:**
- Modify: `src/components/SeriesDetailModal.tsx`

- [ ] **Step 1: Add imports**

Add to the existing imports at the top of `SeriesDetailModal.tsx`:

```typescript
import WatchProviders from './WatchProviders';
import { fetchSeriesWatchProviders } from '../lib/tmdb';
import type { WatchProvidersResult } from '../types';
```

Update the existing tmdb import line to add `fetchSeriesWatchProviders` — or since it's a separate import, just add the new import line. The existing import from `'../lib/tmdb'` already imports `fetchSeasonDetails, fetchSeriesDetails, extractSeriesData, getPosterUrl`.

- [ ] **Step 2: Add state and fetch logic**

Inside the `SeriesDetailModal` component, after the `const [showCastSection, setShowCastSection] = useState(false);` line, add:

```typescript
const [watchProviders, setWatchProviders] = useState<WatchProvidersResult | null>(null);
const [loadingProviders, setLoadingProviders] = useState(false);
```

Then add a new useEffect after the existing TMDB refresh useEffect (after the one that calls `fetchSeriesDetails`):

```typescript
useEffect(() => {
  if (!series.tmdb_id) return;
  let active = true;
  setLoadingProviders(true);
  fetchSeriesWatchProviders(series.tmdb_id).then(result => {
    if (active) {
      setWatchProviders(result);
      setLoadingProviders(false);
    }
  });
  return () => { active = false; };
}, [series.tmdb_id]);
```

- [ ] **Step 3: Add WatchProviders to the JSX**

Insert the `WatchProviders` component in the series detail modal. Place it in the header section, after the badges div (the `flex flex-wrap gap-1.5 text-sm` div), still inside the `flex-1 min-w-0 flex flex-col justify-center gap-2` parent div:

```tsx
<WatchProviders providers={watchProviders} loading={loadingProviders} />
```

- [ ] **Step 4: Commit**

```bash
git add src/components/SeriesDetailModal.tsx
git commit -m "feat: show streaming providers in series detail modal"
```

---

### Task 7: Manual Testing & Verification

- [ ] **Step 1: Start the dev server**

```bash
npm run dev
```

- [ ] **Step 2: Test movie detail modal**

Open a movie that is known to be on Netflix or another platform (e.g., a Netflix original). Verify:
- Provider logos appear below the genre/status badges
- Logos are small (36x36px), rounded, and have a hover scale effect
- Clicking a logo opens the JustWatch/TMDB link in a new tab
- Only subscription-included providers show (no "rent" or "buy" options)
- Movies with no streaming availability show nothing (no empty section)

- [ ] **Step 3: Test series detail modal**

Open a series in the detail modal. Verify the same behavior as movies.

- [ ] **Step 4: Test with no providers**

Open a movie/series that is very old or obscure. Verify the section is hidden gracefully.

- [ ] **Step 5: Test loading state**

On a slow connection (throttle in devtools), verify the skeleton placeholders appear briefly before logos load.

- [ ] **Step 6: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: polish streaming providers display"
```
