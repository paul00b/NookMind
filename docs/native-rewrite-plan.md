# NookMind — Plan de réécriture native (Android d'abord, iOS ensuite)

**Date :** 2026-09-15
**Statut :** plan appliqué pour Android dans `native/` (voir section 10 pour l'état exact de vérification)
**Contexte :** l'app existante est une web app React 19 + Vite + Tailwind 4, packagée dans Capacitor. Objectif : la recoder en app native, sans perdre une seule fonctionnalité, avec le même design, Android d'abord puis iOS.

---

## 1. Objectif

Remplacer le shell Capacitor (WebView) par une vraie app native :

- même périmètre fonctionnel que la web app (3 modes : Livres / Films / Séries, recherche, bibliothèque avec collections, "À suivre", réglages, notifications push, connexion e-mail + Google, suppression de compte, onboarding, FR/EN, thème clair/sombre/système) ;
- même design (typographies Playfair Display + Inter, palette Tailwind, cartes arrondies, bottom nav flottante en pilules, sheets à poignée, ambiance de fond par mode, badges de statut, heatmap des notes IMDb, etc.) ;
- Android livré en premier, iOS ensuite en réutilisant le maximum.

Le backend ne change pas : Supabase (auth + Postgres + RLS), les fonctions Vercel `/api/*` (proxy IMDb, push quotidien, suppression de compte, deep links des plateformes de streaming), TMDB, Google Books, Firebase Cloud Messaging. La web app reste déployable telle quelle.

## 2. Choix technique

### Décision : Kotlin Multiplatform (KMP) + Compose Multiplatform

| Option | Verdict | Pourquoi |
|---|---|---|
| **Kotlin + Jetpack Compose (Android), puis Swift + SwiftUI (iOS)** | Écarté | Vraiment natif sur les deux plateformes, mais deux bases de code complètes à écrire *et* à maintenir en parallèle pour un projet porté par une seule personne. Chaque évolution (nouveau champ Supabase, nouvelle règle de statut de série) se fait deux fois. |
| **KMP + Compose Multiplatform** | **Retenu** | Sur Android, Compose Multiplatform *est* Jetpack Compose : l'app Android est 100 % native (pas de WebView, pas de runtime JS). Toute la logique (modèles, réseau, règles métier des séries, préférences, i18n) et toute l'UI vivent dans `commonMain`. Pour iOS, il ne restera que la couche plateforme (voir section 8). |
| React Native / Flutter | Écarté | Réécriture UI complète aussi, mais avec un runtime intermédiaire (JS ou Skia/Dart) : ce n'est pas ce que "native" veut dire ici, et cela ne réutilise rien de la logique existante non plus. |
| Rester sur Capacitor | Écarté | C'est ce que tu veux quitter : sensation WebView, gestes/sheets/claviers approximatifs, dépendance aux plugins. |

**Point d'honnêteté sur iOS.** Avec KMP, deux voies existent pour la phase iOS :
1. **UI Compose partagée sur iOS** : coût minimal (quelques `actual` + projet Xcode), design pixel-identique garanti. Compose dessine ses propres composants (via Skia dans une `UIView`), donc ce n'est pas de l'UIKit natif, même si le ressenti est très proche (scroll, clavier et texte natifs depuis Compose 1.7/1.8).
2. **SwiftUI au-dessus de la logique Kotlin partagée** : ressenti 100 % Apple, mais il faut réécrire les écrans en SwiftUI (~40 écrans/sheets).

Le projet est structuré pour laisser les deux options ouvertes : la logique n'a aucune dépendance UI, l'UI n'a aucune dépendance plateforme hors `expect/actual`. Ma recommandation : commencer iOS avec l'option 1 (livrable rapide, parité totale), et ne passer certains écrans en SwiftUI que si un ressenti "iOS pur" devient une priorité.

### Versions

| Composant | Version | Remarque |
|---|---|---|
| Kotlin | 2.4.20 | |
| Compose Multiplatform | 1.8.2 | Dernière version dont les artefacts desktop sont autonomes sur Maven Central, ce qui a permis de compiler et d'exécuter le code partagé dans l'environnement de travail (voir section 10). Montée vers 1.12.x = une ligne dans `gradle/libs.versions.toml`, à faire dans Android Studio. |
| Android Gradle Plugin | 8.13.0 | Identique au projet Capacitor (déjà en cache sur ta machine). |
| Gradle | 8.14.3 | Wrapper inclus. |
| Android | minSdk 26, compileSdk/targetSdk 36 | minSdk relevé de 24 à 26 (Android 8.0, 2017) : polices variables et API modernes sans code de compatibilité. |
| supabase-kt | 3.8.0 | Auth (e-mail, ID token Google/Apple) + Postgrest. |
| Ktor | 3.5.2 | Client HTTP (OkHttp sur Android, Java sur desktop, Darwin sur iOS). |
| Coil | 3.3.0 | Chargement d'images multiplateforme (build compatible Compose 1.8.x). |
| kotlinx.serialization / datetime / coroutines | 1.11.0 / 0.8.0 / 1.11.0 | |
| multiplatform-settings | 1.3.0 | Remplace `localStorage`/`sessionStorage`. |
| Firebase Messaging (BoM 34) + Credential Manager 1.5 + googleid | Android uniquement | Push FCM et connexion Google native. |

## 3. Architecture

```
native/
  composeApp/
    src/commonMain/kotlin/fr/paulbr/nookmind/
      app/            racine (App, container, navigation, toasts, thème)
      core/config     AppSecrets (généré depuis secrets.properties) + AppConfig
      core/model      Book, Movie, Series, catégories, types TMDB/IMDb/Google Books
      core/network    HttpClient Ktor, TmdbApi, GoogleBooksApi, ImdbApi (via Vercel), NookMindApi (push, delete, watch providers)
      core/data       AuthRepository, LibraryRepository<T> (books/movies/series), CollectionRepository<T> (3 tables de catégories), PushRepository, AppPreferences, caches
      core/domain     seriesUtils, searchSectionOrder, imdbRatingStyle, stats séries, dates
      core/designsystem  tokens (couleurs, typo, formes), composants réutilisables, icônes Lucide
      core/platform   expect/actual (date locale, bitmap de bruit, ouverture d'URL, lecteur de trailer, push, Google Sign-In)
      feature/onboarding, auth, home, library, nextup, details, settings, legal
    src/commonMain/composeResources/  strings.xml (en) + values-fr/strings.xml, polices, logo
    src/jvmSharedMain  actuals communs Android + desktop (java.time, Locale)
    src/androidMain    Application, MainActivity, FCM service, Google Sign-In, WebView trailer, ressources (icônes, splash)
    src/desktopMain    aperçu desktop + outil de captures d'écran headless (`tools/`)
    src/commonTest     tests unitaires (portage des suites vitest)
  tools/gen-strings.mjs   src/i18n/locales/*.ts  → strings.xml (source de vérité unique pour les textes)
  tools/gen-icons.mjs     icônes Lucide (SVG) → ImageVector Kotlin (mêmes glyphes que lucide-react)
```

Principes :

- **Une seule source de vérité pour le contenu** : les textes FR/EN sont générés depuis les dictionnaires de la web app ; les icônes sont les mêmes fichiers Lucide que ceux utilisés par `lucide-react`.
- **État** : les repositories exposent des `StateFlow` (liste + `loading`), l'UI les collecte ; l'état d'écran suit le même découpage que les hooks React (`remember` ≈ `useState`, `LaunchedEffect` ≈ `useEffect`). Pas de framework de DI : un `AppContainer` construit tout au démarrage.
- **Sheets** : `ModalBottomSheet` Material 3 avec poignée custom, coins 24 dp, fond carte, glisser pour fermer, empilables (détail → épisode → acteur → ajout → acteur…).
- **Navigation** : trois onglets (Recherche / Bibliothèque / À suivre) + mode média (Séries / Films / Livres) persistant, panneau Réglages latéral, écrans plein écran Onboarding / Connexion / Confidentialité / CGU. Le bouton retour Android ferme d'abord la sheet ou le panneau ouvert, puis revient à l'onglet Recherche, puis quitte.
- **Tablette** : au-delà de 768 dp de large, la sidebar desktop de la web app est reprise (mode toggle + nav + profil) et la bottom nav disparaît, comme sur le web.

## 4. Inventaire de parité (web → natif)

| Web (React) | Natif (Compose) | Notes |
|---|---|---|
| `Onboarding` + `OnboardingSlide` | `OnboardingScreen` | 3 slides, fond bruit + halo coloré, points de progression, "Passer", "Commencer". |
| `Login` | `LoginScreen` | E-mail/mot de passe, inscription, œil sur le mot de passe, Google, Apple (iOS uniquement, comme aujourd'hui). |
| `AuthCallback` | — | Redirection OAuth web uniquement ; le natif utilise les ID tokens. |
| `AppLayout` (ambiance, swipe de mode, prompts) | `MainScaffold` | Swipe horizontal avec amortissement 0,25 et seuil 60 px, ambiance de fond par mode, prompt notifications après 5 s. |
| `MobileTopBar`, `BottomNav`, `Sidebar` | `TopBar`, `BottomNav`, `Sidebar` | Pilules flottantes 85 % d'opacité (pas de flou d'arrière-plan, voir section 9). |
| `Home` / `MovieHome` / `SeriesHome` | `BooksHome` / `MoviesHome` / `SeriesHome` | Recherche avec debounce 600 ms et minimum 3 caractères, dropdown avec squelettes, badges "Déjà ajouté", sections réordonnables et masquables. |
| `TrendingMoviesSlider`, `TrendingSeriesSlider` | idem | Onglets de catégorie, chargement paginé au défilement (5 pages max), filtrage des titres déjà suivis. |
| `Library` / `MovieLibrary` / `SeriesLibrary` | idem | Onglets de statut + collections, création/suppression de collection, filtres genre/auteur, tri, grille/liste (persisté), séparateurs par année (films), split En cours / En attente et stats (séries). |
| `NextUpBooks` / `NextUpMovies` / `NextUpSeries` | idem | Progression de lecture, sorties récentes/à venir triées par genres favoris, prochains épisodes avec animations de sortie/entrée et calcul de l'épisode suivant. |
| `AddBookModal`, `AddMovieModal`, `AddSeriesModal` | `AddBookSheet`, `AddMovieSheet`, `AddSeriesSheet` | Mode aperçu + mode édition (films), note IMDb, casting, détection de doublon, grille de saisons. |
| `BookDetailModal`, `MovieDetailModal`, `SeriesDetailModal` | `…DetailSheet` | Note demi-étoiles, notes personnelles éditables, collections, suppression avec confirmation, plateformes de streaming, trailer, casting, saisons/épisodes, heatmap IMDb. |
| `SeriesPreviewSheet`, `ActorSheet`, `EpisodeDetailSheet`, `TrailerModal` | idem | Le trailer s'ouvre dans une WebView plein écran (YouTube embed) sur Android. |
| `SeasonGrid` | `SeasonGrid` | Saisons cliquables, épisodes, "Tout marquer", propositions "ajouter les précédents / retirer les suivants", épisodes non diffusés. |
| `CategoryItemPickerModal` (+ 3 variantes) | `CategoryItemPickerSheet` | Recherche, sélection multiple, "Déjà ajouté". |
| `SeriesStatsSheet` + `useSeriesStats` | idem | Temps total (minutes TMDB), épisodes, saisons, note moyenne, genre et créateur favoris. |
| `SettingsPanel` | `SettingsPanel` | Profil, thème, éditeur d'ordre des sections (drag & drop + visibilité), notifications FCM + test, "Mettre à jour l'app" (vide les caches), rejouer l'onboarding, suppression de compte, liens légaux. |
| `NotificationPromptSheet` | idem | Version FCM uniquement. |
| `InstallPromptSheet`, `useInstallPrompt`, service worker | — | Spécifique PWA : sans objet sur une app installée. |
| `Privacy`, `Terms` | `PrivacyScreen`, `TermsScreen` | Contenu identique. |
| `StarRating` | `StarRating` | Demi-étoiles, glisser pour noter. |
| `WatchProviders` + `/api/watch-providers` | idem | Logos TMDB, deep links JustWatch via l'API Vercel, fallback recherche par plateforme. |
| `EpisodeRatingBadge`, `imdbRatingStyle` | idem | Mêmes seuils de couleur. |
| `useTmdbSeriesRefresh` | `SeriesRefreshJob` | Rafraîchissement TMDB toutes les 24 h des séries en cours/terminées. |
| Toasts (`react-hot-toast`) | `ToastHost` | Bas centré au-dessus de la nav, fond `#1a1f2e`, 3 s. |
| `@vercel/analytics` | — | Web uniquement. |

## 5. Design system

| Tailwind (web) | Compose (natif) |
|---|---|
| Fond clair `#f8f6f1`, sombre `#0f1117` ; carte `#fff` / `#1a1f2e` ; bordures `black/8` / `white/8` | `NookColors` (clair/sombre) exposés via `NookTheme` |
| Gris Tailwind 50→900, ambre/indigo/teal 400→700, émeraude, bleu, violet, sky, rouge | `Palette` (mêmes hex) |
| `font-serif` Playfair Display 400/600/700 + italique, `font-sans` Inter 300→700 | polices embarquées (`composeResources/font`) |
| `rounded-xl` 12, `rounded-2xl` 16, `rounded-3xl` 24, `rounded-full` | `NookShapes` |
| `card`, `btn-primary`, `btn-ghost`, `input`, `nav-link` | `NookCard`, `PrimaryButton`, `GhostButton`, `NookTextField`, items de nav |
| `mode-bg-*` (bruit SVG feTurbulence + halo radial ambre/indigo/teal, opacité 25 %/30 %) | `ModeAmbianceBackground` : dégradé radial + bitmap de bruit tuilé généré au démarrage |
| Animations `fade-in`, `slide-up`, `slide-in-right`, `next-up-slide-in/out`, FLIP des sections | `AnimatedVisibility`, `animateItem`, `animateContentSize`, animations de décalage |
| Statuts : lu/vu = émeraude, en cours = bleu, à voir = ambre, en attente = violet, futur = sky | `StatusBadge` |
| Thème `system` / `light` / `dark` via classe `dark` | `ThemeMode` persistant + `isSystemInDarkTheme()` |

## 6. Données et intégrations natives

- **Supabase** : mêmes tables (`books`, `movies`, `series`, `*_categories`, `*_category_items`, `push_subscriptions`), mêmes RLS ; `rating` en `Double?` (demi-étoiles), `watched_seasons` / `watched_episodes` en JSON.
- **Auth** : e-mail/mot de passe (inscription avec confirmation par e-mail comme aujourd'hui), Google via Credential Manager (ID token → `signInWithIdToken`, même client web OAuth que Supabase, SHA-1 déjà déclarés pour `fr.paulbr.nookmind`), Apple réservé à iOS, suppression de compte via `/api/account/delete`, déconnexion qui efface aussi le flag d'onboarding.
- **Push** : FCM (canal `default`, importance haute), enregistrement du token dans `push_subscriptions` (`transport = fcm`), rafraîchissement du token, test via `/api/push/test`, tap sur une notification → bibliothèque.
- **Caches** : équivalents de `sessionStorage` (TMDB 6 h, IMDb 24 h, sorties films 24 h) et `localStorage` (thème, mode, ordre des sections, vue grille/liste, onboarding, prompts) via `multiplatform-settings`. "Mettre à jour l'application" vide les caches réseau.
- **Système** : splash Android 12+ (`core-splashscreen`) sur fond `#0f1117`, barre de statut adaptée au thème, edge-to-edge avec insets, bouton retour géré, orientation libre sans recréation d'activité.
- **Secrets** : `native/secrets.properties` (ignoré par git) → objet `AppSecrets` généré au build ; mêmes valeurs que les `VITE_*` du web.

## 7. Build et publication

1. Copier `native/secrets.properties.example` vers `native/secrets.properties` et renseigner les valeurs (identiques au `.env` web).
2. Copier `google-services.json` (console Firebase, projet `nookmind-8f5be`) dans `native/composeApp/`.
3. Copier `keystore.properties` (déjà utilisé par le projet Capacitor) dans `native/` ; l'alias `nookmind` et le keystore `~/nookmind-release.jks` restent les mêmes → même signature, donc mise à jour transparente de l'app déjà publiée (`versionCode` 2, `versionName` 2.0.0).
4. Ouvrir `native/` dans Android Studio (ou `./gradlew :composeApp:assembleDebug`) ; release : `./gradlew :composeApp:bundleRelease`.
5. Aperçu desktop : `./gradlew :composeApp:run` ; captures headless : `./gradlew :composeApp:screenshots`.

## 8. Phase iOS (à venir)

Ce qu'il restera à faire, tout le reste étant déjà partagé :

- projet Xcode `iosApp/` (SwiftUI `App` hébergeant `ComposeUIViewController`), cibles `iosArm64` / `iosSimulatorArm64` déjà déclarées dans le Gradle (activées automatiquement sur macOS) ;
- `actual` iOS : formatage de dates (`NSDateFormatter`), bitmap de bruit (Skia), lecteur de trailer (`WKWebView`), Google Sign-In (SDK Google), Apple Sign-In (`ASAuthorizationController` → ID token + nonce, déjà prévu côté `AuthRepository`), push (APNs → FCM), stockage (`NSUserDefaults` via multiplatform-settings) ;
- `PrivacyInfo.xcprivacy`, corriger le bundle id (`fr.paulbr.bookmind` → `fr.paulbr.nookmind` dans l'ancien projet), signature et TestFlight.

## 9. Différences assumées

- Pas de flou d'arrière-plan (`backdrop-blur`) derrière les pilules de navigation : Compose n'a pas d'équivalent stable ; fond à 85 % d'opacité identique au web sans le flou.
- Pas de prompt "Installer l'application" ni de service worker (PWA).
- Le nom d'affichage modifié dans les réglages est enregistré dans les métadonnées Supabase de l'utilisateur (sur le web il n'était pas persisté : c'était un bug).
- Le trailer YouTube s'affiche dans une WebView (l'iframe n'a pas d'équivalent natif).

## 10. Vérification (état exact)

L'environnement de travail utilisé pour cette réécriture n'a pas accès à Google Maven ni au SDK Android (politique réseau). En conséquence :

- **Compilé et exécuté ici** : tout le code partagé (`commonMain`, ~90 % de l'app : logique, réseau, UI Compose) via la cible desktop JVM, avec les forks JetBrains des bibliothèques androidx ; tests unitaires (`commonTest`) exécutés ; captures d'écran headless de chaque écran (voir `native/docs/screenshots/` si présent) pour comparer au design web.
- **Écrit mais non compilé ici** : la couche `androidMain` (Application, MainActivity, Google Sign-In, service FCM, WebView) et la configuration Android du Gradle. Ce sont des fichiers courts qui utilisent des API stables ; ils doivent être compilés une première fois dans Android Studio.
- **À valider sur appareil** : connexion Google (dépend des SHA-1 déclarés), réception des push, comportement du clavier et des gestes, performances de la liste.

## 11. Étapes suivantes pour toi

1. Secrets + `google-services.json` + `keystore.properties` (section 7).
2. Ouvrir `native/` dans Android Studio, laisser Gradle synchroniser, lancer sur un appareil.
3. Corriger ce que la première compilation Android remonte éventuellement (couche `androidMain`).
4. Tester le parcours complet, comparer visuellement avec la web app.
5. Quand l'app native est validée : supprimer `android/`, `ios/`, `capacitor.config.ts` et les dépendances `@capacitor/*` du `package.json`, mettre à jour `RELEASE.md`.
6. Phase iOS (section 8).

## 12. Risques

| Risque | Mitigation |
|---|---|
| Erreurs de compilation dans `androidMain` (non vérifiable ici) | Couche minimale, API stables ; à corriger dans Android Studio. |
| Écart visuel fin (rendu des polices, ombres) | Captures headless comparées au web ; ajustements de tokens centralisés. |
| Connexion Google refusée (`DEVELOPER_ERROR`) | Même `applicationId` et même keystore que l'app Capacitor : les SHA-1 déjà déclarés s'appliquent ; vérifier le SHA-1 du build debug (`~/.android/debug.keystore`). |
| Token FCM absent | Nécessite `google-services.json` du projet Firebase ; le build fonctionne sans, les notifications non. |
| Montée de version Compose 1.8.2 → 1.12 | Une ligne dans le catalogue ; à faire dans Android Studio où Google Maven est accessible. |
