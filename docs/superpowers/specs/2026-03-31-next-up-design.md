# "À suivre / Next Up" — Design Spec

**Date:** 2026-03-31
**Status:** Approved
**Replaces:** Discovery/Discover section (removed in d583834)

---

## Overview

Repurpose the `/discover` route as a **"Next Up" (À suivre)** page — a progress dashboard showing what the user should watch, read, or look forward to next. No recommendations, no discovery. Purely progress-tracking and upcoming content.

The page follows the existing **media mode pattern**: content switches based on the active mode (books / movies / series), exactly like Home and Library.

---

## Data Model Changes

### Books — new status + field

**`BookStatus`** gains a third value:
```ts
type BookStatus = 'read' | 'want_to_read' | 'reading';
```

**`Book`** gains one nullable field:
```ts
current_page: number | null;
```

**Supabase migration:**
```sql
ALTER TABLE books ADD COLUMN IF NOT EXISTS current_page INTEGER;
```

No changes to movies or series data models — `watched_episodes` (already in place) is sufficient for series progress.

---

## Page Structure

### Routing

Route `/discover` is kept as-is. A new `NextUpSwitch` component (matching the pattern of `HomeSwitch` / `LibrarySwitch`) renders the correct sub-page based on `MediaModeContext`:

- `series` → `NextUpSeries`
- `books` → `NextUpBooks`
- `movies` → `NextUpMovies`

### Navigation label

- EN: **"Next Up"**
- FR: **"À suivre"**
- Icon: keep compass, or switch to `PlayCircle` / `ChevronRight` (decide during implementation)

---

## Series — NextUpSeries

### Logic: computing the next episode

For each series with `status === 'watching'`:

1. From `watched_episodes`, find the lowest unwatched episode across all seasons (e.g. if S1 complete and S2E1–E3 watched → next is S2E4; if S1E1–E5 of 10 watched → next is S1E6).
2. If all locally-known episodes are watched, call TMDB `/tv/{tmdb_id}` to get `next_episode_to_air`.
3. Episode counts per season come from `episodeCounts` (already fetched lazily in `SeasonGrid`) or from a fresh TMDB season call if not cached.

### Episode states

| State | Condition | Display |
|---|---|---|
| **Available** | Episode exists and air date ≤ today | "S2E4 · Episode name" + green badge "Disponible" |
| **Coming soon** | Air date > today | Episode name (if known) + orange badge "Dans X jours · 14 avr." |
| **Up to date** | All aired episodes watched, nothing announced | Grey badge "À jour ✓" |

### Countdown + date

Both shown together when an episode is upcoming:
- "Dans 3 jours · 14 avril 2026"
- If same day: "Aujourd'hui"

### Card anatomy

Vertical list, one card per watching series:
- Small poster (left)
- Series title (bold)
- Progress line: "Saison 2 · Épisode 4 sur 10"
- State badge (coloured pill)
- Episode name + air info (if applicable)

### Performance

- TMDB calls made on page load, one per watching series.
- Results cached in `sessionStorage` for the browser session (keyed by `tmdb_id`).
- If TMDB call fails for a series: show what we know from local data, skip `next_episode_to_air`.

### Empty state

No series with `watching` status → message: *"Marque une série comme 'En cours' pour voir tes prochains épisodes ici."*

---

## Books — NextUpBooks

### Reading status

- Library gets a new **"En cours"** tab (first position, like other modes).
- `BookDetailModal` and `AddBookModal` expose the `reading` status as a selectable option.
- When status is set to `reading`, a "Page actuelle" field appears.

### Progress tracking

- `current_page` is editable inline on the Next Up card (tap the number → input → save). No need to open the detail modal.
- Progress bar = `current_page / page_count` (only shown if `page_count` is known from Google Books).
- Display: "Page 142 / 380" or just "Page 142" if total unknown.

### Card anatomy

One card per book with `status === 'reading'` (supports multiple books in progress):
- Cover (left)
- Title + author
- Progress bar (if page_count known)
- Page counter (inline-editable)
- Quick action: "Marquer comme lu ✓" button

### Empty state

No book in `reading` status → show first book from `want_to_read` list with CTA: *"Commencer ce livre"* (sets status to `reading`). If `want_to_read` is also empty → generic empty message.

---

## Movies — NextUpMovies

### Data source

TMDB `/movie/upcoming` — returns films releasing in the coming weeks.

### Personalisation

User's preferred genres are computed from their watched movies (top genres by frequency, same approach as old Discover). The upcoming list is filtered/sorted to prioritise matching genres.

Fallback: if user has fewer than 3 watched movies, show unfiltered popular upcoming releases.

### Display

Horizontal scrollable row of cards (up to 10 films), each showing:
- Poster
- Title
- Release date: "Dans X jours" + "14 avr. 2026" (or "Sorti le X" if very recent)
- Primary genre
- Quick action: "+ À voir" button (adds to watchlist without opening a modal)

Section title: **"Sorties à venir"**
Subtitle: *"Basé sur vos genres favoris"* (or *"Sorties populaires"* if no genre data)

### Performance & errors

- Cache: 24h in `sessionStorage` (upcoming releases don't change hourly).
- If TMDB fails: section silently hidden (no error shown to user).
- If TMDB returns 0 results after filtering: show unfiltered results as fallback.

### Empty state

Not applicable — if TMDB works, there are always upcoming movies. Section hidden on API failure.

---

## Unchanged

- `BottomNav`, `Sidebar`, `MediaModeContext` — no structural changes.
- `HomeSwitch`, `LibrarySwitch` — untouched.
- All existing series/movie/book modals — only `BookDetailModal` and `AddBookModal` gain the new `reading` status option.

---

## Out of Scope

- Notifications / push alerts for episode releases.
- Calendar view or timeline.
- Syncing progress with external services (Trakt, Goodreads, etc.).
- Sorting/filtering within the Next Up pages.
