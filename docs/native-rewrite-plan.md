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
| Android | minSdk 24, compileSdk/targetSdk 36 | Même plancher que le paquet Capacitor : aucun appareil ne perd les mises à jour. Le seul appel Android 8+ du code (création du canal de notification) est gardé par un test de version. |
| supabase-kt | 3.8.0 | Auth (e-mail, ID token Google/Apple) + Postgrest. |
| Ktor | 3.5.2 | Client HTTP (OkHttp sur Android, Java sur desktop, Darwin sur iOS). |
| Coil | 3.3.0 | Chargement d'images multiplateforme (build compatible Compose 1.8.x). |
| kotlinx.serialization / datetime / coroutines | 1.11.0 / 0.8.0 / 1.11.0 | |
| multiplatform-settings | 1.3.0 | Remplace `localStorage`/`sessionStorage`. |
| Haze | 1.6.10 | Flou d'arrière-plan du dock et du sélecteur de mode (verre dépoli), absent de Compose 1.8. |
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

## 8. Phase iOS (en cours)

**Fait et vérifié sur GitHub Actions (`ios-simulator-build.yml`, runner macOS 26, Xcode 26.6, premier run vert le 2026-09-18) :**

- les neuf `actual` iOS dans `iosMain/` : plateforme, niveau d'API haptique (jamais dégradé, la table est celle d'Android), langue de l'appareil, formatage des dates (`NSDateFormatter`, mêmes motifs TR35 que java.time), préférences (`NSUserDefaults`, une suite par magasin), sortie de l'app (sans effet, Apple l'interdit), journaux, ouverture d'URL, trailer (`WKWebView` dans `UIKitView`). Le bitmap de bruit prévu ici n'existe plus, `Ambiance.kt` est du Compose pur ;
- les trois interfaces de pont vers Swift (`IosAppleSignInBridge`, `IosGoogleSignInBridge`, `IosPushBridge`) et leur adaptation vers les providers `suspend` du code commun, nonces SHA-256 compris ;
- `IosApp`, racine de composition (pendant de `NookMindApplication`), et `MainViewController()` ;
- l'hôte `native/iosApp/` : `project.yml` XcodeGen, SwiftUI `App` + `AppDelegate`, `ComposeView`, `AppleSignInBridge.swift` (`ASAuthorizationController`), `Info.plist`, entitlements, `PrivacyInfo.xcprivacy`, icône et écran de lancement repris de l'ancien projet ;
- le bundle id est `fr.paulbr.nookmind`, sans suffixe debug : aucune app iOS n'a jamais été publiée, il n'y a rien avec quoi cohabiter.

**Reste, et demande un iPhone plus une équipe Apple Developer payante :**

- Sign in with Apple : la capacité sur l'App ID, l'entitlement, et `NookMindAppleSignInEnabled` à `true`. Le code est écrit ;
- Google : paquet SPM `GoogleSignIn-iOS`, client OAuth iOS dans le projet Google Cloud du client web, implémentation Swift de `IosGoogleSignInBridge` ;
- push : paquet SPM `firebase-ios-sdk`, `GoogleService-Info.plist`, clé APNs dans Firebase, implémentation Swift de `IosPushBridge`, réception et routage d'un tap dans l'AppDelegate (`IosApp.openRoute`) ;
- vérifier les cinq signaux haptiques sur l'appareil ;
- signature, TestFlight, captures App Store.

## 9. Différences assumées

Écarts volontaires entre la web app et l'app native, tous vérifiés écran par écran :

- **Le flou d'arrière-plan des pilules de navigation passe par Haze** (`dev.chrisbanes.haze`), Compose n'ayant pas d'équivalent natif en 1.8. Le dock et le sélecteur de mode sont donc du vrai verre dépoli : le contenu derrière est flouté à 32 dp et teinté à 45 %, sans le grain que la bibliothèque peut ajouter. Sous Android 12, faute de `RenderEffect`, Haze retombe sur une teinte opaque proche de l'ancien rendu.
- **Les voiles des feuilles et des dialogues n'ont pas de flou.** Le web y met un `backdrop-blur-xs`, soit 2 px, invisible à l'œil ; le voile sombre est identique, le flou en moins.
- **Pas de prompt « Installer l'application » ni de service worker** : ces deux écrans n'ont plus d'objet une fois l'app installée depuis le store. `InstallPromptSheet` n'est donc pas porté ; `NotificationPromptSheet`, lui, l'est.
- **Le nom d'affichage modifié dans les réglages est persisté** dans les métadonnées Supabase de l'utilisateur. Sur le web il ne l'était pas : la modification disparaissait au rechargement. C'était un bug, il est corrigé ici.
- **Le trailer YouTube s'affiche dans une WebView**, faute d'iframe. Même lecteur, même comportement, mais le plein écran dépend de la WebView du système.
- **Pas de grain sur l'ambiance, et en-tête transparent.** La web app superpose un bruit `feTurbulence` au halo et pose une barre opaque à 85 % en haut. Sur un écran à forte densité ce grain lit comme du sablage plutôt que comme du grain argentique, donc il a été retiré : le halo est un dégradé propre, et il traverse l'en-tête, qui n'a plus de fond. Choix de design assumé, décidé après avoir vu le rendu sur appareil. Le web garde l'ancien traitement, les deux diffèrent ici volontairement.
- **Désactiver les notifications supprime vraiment la ligne FCM.** Sur le web, la désactivation ne changeait qu'un état local (`setNativeSubscribed(false)` dans `SettingsPanel.tsx`) : la ligne restait en base et le cron continuait d'envoyer. Deuxième correction du même endroit : les trois préférences (épisodes, saisons, sorties) sont réécrites depuis l'état courant à la réactivation, là où le web les forçait toutes les trois à `true`.
- **Rien n'est repris du stockage de l'app Capacitor.** Session, thème, mode d'affichage, ordre des sections et drapeau d'onboarding vivaient dans le `localStorage` de la WebView ; l'app native utilise les préférences Android, avec les mêmes clés mais un autre support. À la mise à jour, l'utilisateur se reconnecte une fois et revoit l'onboarding ; sa bibliothèque, elle, est dans Supabase et revient intacte. Lire l'ancien `localStorage` supposerait de parser le LevelDB de la WebView : disproportionné pour une gêne unique.
- **Les dates sont formatées par `java.time`** sur Android et desktop, pas par `Intl.DateTimeFormat`. Les motifs ont été alignés locale par locale (`d MMM yyyy` en français, `MMMM d, yyyy` en anglais) ; un écart de ponctuation reste possible sur des locales exotiques.

## 10. Vérification (état exact)

L'environnement de travail utilisé pour cette réécriture n'a accès ni à Google Maven ni au SDK
Android (politique réseau). La cible Android n'a donc jamais été assemblée. Pour ne pas livrer du
code jamais exécuté pour autant, une chaîne de vérification hors ligne a été montée autour de la
cible desktop.

### Ce qui a été compilé, exécuté et regardé

| Vérification | Comment | Résultat |
|---|---|---|
| Code partagé (`commonMain`, ~14 600 lignes, 86 fichiers) | Compilé sur la cible desktop JVM | Compile |
| Logique métier | 56 tests unitaires (`commonTest`) : URLs d'API, aides de formatage, prochain épisode, utilitaires de séries, statistiques de séries, corps des insertions Supabase, calcul du glisser-déposer | 56 réussis, 0 échec |
| Rendu de chaque écran | 33 captures headless via `ImageComposeScene`, en anglais **et** en français, clair et sombre, plus une mise en page tablette 1024x768 | 33 + 33 rendues et relues une à une |
| Couche `androidMain` | Module de contrôle de types compilant le code Android contre le jar `android-all` de Robolectric et des stubs androidx/Firebase écrits à la main | Compile |
| Ressources | Audit des 545 chaînes : 483 utilisées, 62 inutilisées (toutes déjà inutilisées côté web ou propres à des états d'une autre plateforme), 0 manquante | Aucune chaîne absente |

Deux détails d'outillage, utiles si la chaîne doit être remontée :

- les forks JetBrains d'androidx pour desktop sont en retard sur `androidx.collection`, ce qui fait
  échouer le rendu (`NoClassDefFoundError: OrderedScatterSetKt`). Le contournement est de compiler
  `androidx.collection` et `androidx.annotation` depuis les sources AOSP dans un module à part ;
- le contrôle de types de `androidMain` se fait contre le jar `android-all` de Robolectric, qui
  contient le vrai framework Android, complété par des stubs pour ce que Robolectric ne fournit pas
  (Credential Manager, Firebase Messaging, splash screen).

### Parité des API et des colonnes, vérifiée ligne à ligne

Comparaison systématique du code web et du code natif, appel par appel et champ par champ :

- **Colonnes Supabase** : les cinq entités (`books`, `movies`, `series`, les trois tables de collections et leurs tables de jointure, `push_subscriptions`) ont exactement les mêmes noms de colonnes des deux côtés, valeurs d'énumération comprises. Les clés d'upsert des tables de jointure sont identiques.
- **TMDB** : mêmes chemins, mêmes paramètres, même `append_to_response`, même locale (`fr-FR` / `en-US`), même pagination (`page < 5`), mêmes tailles d'images (`w92`, `w185`, `w300`, `w400`, `w500`) aux mêmes endroits, même repli du trailer sur `en-US`.
- **Google Books, routes Vercel** : mêmes chemins, mêmes paramètres, mêmes corps de requête, mêmes délais d'expiration (8 s, 10 s, 15 s) et mêmes durées de cache (6 h pour TMDB, 24 h pour IMDb).
- **Deux différences assumées** : le natif n'appelle pas `/api/push/subscribe` ni `/api/push/unsubscribe`, qui sont la voie web-push du navigateur ; il écrit dans `push_subscriptions` comme le faisait déjà la branche native de la web app. Les recherches par genre et par auteur de la web app n'ont pas d'équivalent natif, mais elles n'ont aucun appelant côté web non plus : la page Discover est un écran « Coming Soon » et la barre de navigation pointe en réalité sur « À suivre ».

Un bug réel est sorti de cette comparaison et a été corrigé : l'encodeur des écritures Supabase omettait les propriétés restées à leur valeur par défaut, ce qui faisait échouer l'ajout d'un livre sans auteur (`books.author` est NOT NULL sans valeur par défaut). Sept tests verrouillent désormais le contenu du corps d'insertion.

### Ce qui n'a pas pu être vérifié ici

- **L'assemblage Android lui-même** : AGP, la fusion des manifestes, la génération de `R`, R8. La
  première ouverture dans Android Studio est la première vraie compilation Android.
- **Les écrans « À suivre » avec des données réelles** : ils dépendent de TMDB, inatteignable hors
  ligne. Ils rendent donc vide en capture, ce qui est le comportement attendu, identique au web qui
  ne rend rien quand les deux listes sont vides (`NextUpMovies.tsx`).
- **Le comportement à l'exécution sur appareil** : connexion Google (dépend des SHA-1 déclarés),
  réception des push, gestes et clavier, fluidité des listes longues.

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
| `minSdk` 24 refusé par la fusion des manifestes (une dépendance exigeant plus) | Non vérifiable ici : Google Maven est inaccessible, donc aucun manifeste de dépendance n'a pu être lu. L'erreur nomme la bibliothèque fautive ; la remonter à 26 dans `libs.versions.toml` débloque (au prix d'Android 7.x). |
