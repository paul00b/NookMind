# Onboarding Slides — Design Spec

**Date:** 2026-04-13
**Status:** Approved

---

## Overview

A 3-slide onboarding carousel shown before the login screen for first-time visitors. Each slide introduces a core value proposition of NookMind with a distinct gradient color matching the app's media modes. The onboarding is swipeable, skippable, and replayable from Settings.

---

## Slides Content

### Slide 1 — Welcome (Amber)

- **Title:** "Track movies, series & books"
- **Body:** "Your watchlist, your reading list, all in one place. Free and ads-free, forever."
- **Visual:** NookMind logo (use the actual nookmind logo in oublic/logo.svg) centered, with floating semi-transparent background icons (film, TV, book)
- **Gradient:** Amber (#f59e0b) — matches the Books media mode

### Slide 2 — Smart Features (Indigo)

- **Title:** "Never miss a thing"
- **Body:** "IMDB ratings per episode, new episode alerts, cast info, and streaming providers at a glance."
- **Visual:** 4 feature icons in rounded square tiles (star/ratings, bell/alerts, masks/cast, satellite/providers)
- **Animation:** The 4 icons are "floating", slightly moving up, down, left and right - very subtle movement, smooth animation, premium feel
- **Gradient:** Indigo (#6366f1) — matches the Movies media mode

### Slide 3 — Discover & Collect (Teal)

- **Title:** "Discover & collect"
- **Body:** "Trending now, upcoming releases. Build collections and track your watching stats."
- **Visual:** Stylized card/poster shapes suggesting a collection browse view
- **Gradient:** Teal (#14b8a6) — matches the Series media mode
- **CTA Button:** "Get Started" — teal gradient, full-rounded pill shape

---

## Illustration Strategy

- Default illustrations are CSS-based: floating Lucide icons and gradient shapes
- The illustration area per slide is designed as a swappable component so custom illustrations can be provided later
- Background uses the same gradient technique as the app's existing mode backgrounds (radial gradient + noise texture)

---

## Navigation & Interaction

### Swipe

- Touch drag left/right with momentum and snap-to-slide behavior
- CSS scroll-snap for smooth native feel (`scroll-snap-type: x mandatory` on the container, `scroll-snap-align: center` on each slide)

### Skip Button

- Top-right corner on every slide
- Text: "Skip" — subtle, gray color (#9ca3af)
- Action: jumps to login screen (or home if already authenticated)

### Progress Indicators

- Pill-shaped dots at bottom center
- Active dot: wider (elongated pill, ~20px) + colored with the current slide's accent color
- Inactive dots: small circles (6px), gray (#4b5563)
- Dots transition smoothly when swiping between slides

### Get Started Button

- Only visible on the last slide (Slide 3)
- Teal gradient background, white text, rounded-full pill
- Box shadow with teal glow
- Action: navigates to login (or home if already authenticated)

---

## Persistence & Replay

### First-time display

- On app load, before showing the login screen, check `localStorage` for `nookmind_onboarding_completed`
- If the key is absent or `false`, show the onboarding
- After completing (reaching "Get Started") or skipping, set the key to `true`

### Replay from Settings

- Add a "Replay onboarding" option in the Settings page
- When tapped: clears the localStorage flag and navigates to the onboarding route
- If the user is already authenticated, the "Get Started" button on the last slide navigates to Home instead of Login

---

## Routing

- New route: `/onboarding`
- The route is **not** protected — accessible without authentication
- App entry logic (in the router or a wrapper):
  1. If `onboarding_completed` is falsy in localStorage → redirect to `/onboarding`
  2. If completed but not authenticated → show `/login`
  3. If completed and authenticated → show `/` (home)

---

## i18n

All user-facing strings go through i18next. Keys to add in both `en` and `fr` translation files:

| Key | EN | FR |
|-----|----|----|
| `onboarding.slide1.title` | Track movies, series & books | Suivez vos films, séries et livres |
| `onboarding.slide1.body` | Your watchlist, your reading list, all in one place. Free and ads-free, forever. | Votre liste de visionnage, votre liste de lecture, tout au même endroit. Gratuit et sans pub, pour toujours. |
| `onboarding.slide2.title` | Never miss a thing | Ne manquez plus rien |
| `onboarding.slide2.body` | IMDB ratings per episode, new episode alerts, cast info, and streaming providers at a glance. | Notes IMDB par épisode, alertes de nouveaux épisodes, casting et plateformes de streaming en un coup d'œil. |
| `onboarding.slide3.title` | Discover & collect | Découvrez et collectionnez |
| `onboarding.slide3.body` | Trending now, upcoming releases. Build collections and track your watching stats. | Tendances du moment, prochaines sorties. Créez des collections et suivez vos statistiques. |
| `onboarding.skip` | Skip | Passer |
| `onboarding.getStarted` | Get Started | Commencer |
| `settings.replayOnboarding` | Replay onboarding | Revoir la présentation |

---

## Component Structure

```
src/
  pages/
    Onboarding.tsx          — Main onboarding page with carousel logic
  components/
    OnboardingSlide.tsx     — Single slide component (gradient, illustration, text)
```

- `Onboarding.tsx`: Horizontal scroll container with snap, manages current slide index, handles skip/get-started navigation
- `OnboardingSlide.tsx`: Receives props (title, body, gradient color, illustration component, accent color) — renders a full-viewport slide

---

## Technical Notes

- Use CSS `scroll-snap` for the carousel — no external carousel library needed
- Each slide is `100vw` wide, container has `overflow-x: auto` with `scroll-snap-type: x mandatory`
- Track active slide via `IntersectionObserver` or `scroll` event to update progress dots
- Slides are full viewport height (`100svh`) for immersive feel
- Gradient backgrounds use the same noise texture pattern as the existing mode backgrounds in `index.css`
- No new dependencies required
