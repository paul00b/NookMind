# NookMind — application native (Kotlin Multiplatform + Compose)

Réécriture native de la web app NookMind. Android en premier, iOS ensuite : la quasi-totalité
du code (logique, réseau, écrans) vit dans `commonMain` et sera réutilisée telle quelle par la
cible iOS.

Cette app remplace le paquet Capacitor (`android/`, `ios/` à la racine du dépôt), pas la web app,
qui continue de vivre à côté.

## Sommaire

1. [Ce qu'il faut installer](#1-ce-quil-faut-installer)
2. [Configuration avant le premier build](#2-configuration-avant-le-premier-build)
3. [Compiler et lancer](#3-compiler-et-lancer)
4. [Structure du projet](#4-structure-du-projet)
5. [Générateurs (chaînes et icônes)](#5-générateurs-chaînes-et-icônes)
6. [Tests et captures d'écran](#6-tests-et-captures-décran)
7. [Publication Play Store](#7-publication-play-store)
8. [Limites de l'environnement de développement utilisé pour la réécriture](#8-limites-de-lenvironnement-de-développement-utilisé-pour-la-réécriture)

## 1. Ce qu'il faut installer

| Outil | Version | Remarque |
|---|---|---|
| JDK | 17 | Requis par AGP 8.13 ; le wrapper Gradle utilise le JDK du `JAVA_HOME`. |
| Android Studio | Ladybug ou plus récent | Facultatif : tout marche aussi en ligne de commande. |
| Android SDK | compileSdk 36, build-tools correspondants | `minSdk` 24 (Android 7.0), comme le paquet Capacitor. |
| Node.js | 18+ | Uniquement pour les générateurs de la section 5. |

Gradle est fourni par le wrapper (8.14.3), rien à installer.

Les dépendances sont résolues depuis Google Maven et Maven Central. Le premier `./gradlew` a donc
besoin d'un accès réseau à `dl.google.com` et `repo1.maven.org`.

## 2. Configuration avant le premier build

Trois fichiers, tous ignorés par git, tous à créer à la main.

### `native/secrets.properties`

```bash
cp secrets.properties.example secrets.properties
```

Puis remplis les six valeurs. Ce sont les mêmes que les variables `VITE_*` du `.env` de la web app :

| Clé | Équivalent web |
|---|---|
| `SUPABASE_URL` | `VITE_SUPABASE_URL` |
| `SUPABASE_ANON_KEY` | `VITE_SUPABASE_ANON_KEY` |
| `API_BASE_URL` | domaine Vercel qui héberge les fonctions `/api` |
| `GOOGLE_BOOKS_API_KEY` | `VITE_GOOGLE_BOOKS_API_KEY` |
| `TMDB_API_KEY` | `VITE_TMDB_API_KEY` |
| `GOOGLE_AUTH_WEB_CLIENT_ID` | client OAuth **web** partagé avec Supabase |

Chaque clé peut aussi être passée par une variable d'environnement du même nom, ce qui est plus
pratique en CI. Le build génère à partir de là un objet Kotlin `fr.paulbr.nookmind.core.config.AppSecrets`.

`GOOGLE_AUTH_WEB_CLIENT_ID` est bien le client **web**, pas le client Android : le Credential Manager
demande un jeton d'identité destiné au backend Supabase, qui ne reconnaît que ce client-là.

### `native/composeApp/google-services.json`

À télécharger depuis la console Firebase (projet `nookmind-8f5be`, application Android
`fr.paulbr.nookmind`). Sans lui le build fonctionne et l'app se lance, mais les notifications push
sont inertes : le plugin `google-services` n'est appliqué que si le fichier existe, et un
avertissement le rappelle au moment de la configuration Gradle.

### `native/keystore.properties`

Uniquement pour les builds de release. Même contenu que celui du projet Capacitor :

```properties
storeFile=/chemin/absolu/vers/nookmind-release.jks
storePassword=…
keyAlias=nookmind
keyPassword=…
```

Le même keystore que l'app publiée, donc la mise à jour reste transparente pour les utilisateurs
installés et les empreintes SHA-1 déjà déclarées dans Firebase et Google Cloud continuent de valoir
pour la connexion Google. Sans ce fichier le bloc `signingConfigs` n'est simplement pas créé et
`assembleRelease` produit un APK non signé.

## 3. Compiler et lancer

```bash
cd native

./gradlew :composeApp:assembleDebug     # APK debug (applicationId fr.paulbr.nookmind.debug)
./gradlew :composeApp:installDebug      # installe sur l'appareil ou l'émulateur connecté
./gradlew :composeApp:bundleRelease     # AAB signé pour le Play Store
```

Aperçu desktop, pratique pour itérer sur l'UI sans émulateur :

```bash
./gradlew :composeApp:run
```

Le build debug porte le suffixe `.debug` sur l'`applicationId`, il cohabite donc avec la version du
Play Store sur le même appareil. Attention : ce suffixe change l'empreinte attendue par Google
Sign-In, il faut déclarer le SHA-1 du keystore de debug (`~/.android/debug.keystore`, mot de passe
`android`) dans la console Google Cloud pour tester la connexion Google en debug.

## 4. Structure du projet

```
native/
  composeApp/
    src/
      commonMain/      86 fichiers Kotlin, ~14 600 lignes — tout le code partagé
        app/           AppContainer (racine de composition), App(), thème + état global
        core/
          config/      AppSecrets (généré), URLs de l'API
          data/        dépôts exposant des StateFlow, cache JSON, préférences, Patch
          designsystem/ thème, tokens, typographie, icônes Lucide, composants
          domain/      logique pure (statistiques de séries, prochain épisode, aides)
          model/       modèles sérialisables partagés avec Supabase et les API tierces
          network/     clients Ktor : Supabase, TMDB, Google Books, IMDb, API NookMind
          platform/    déclarations expect + interfaces des services natifs
          ui/          contrôleur de toasts, utilitaires de composition
        feature/       un dossier par domaine : auth, books, movies, series, library,
                       nextup, home, collections, settings, shell, onboarding, legal, common
        composeResources/  strings.xml (en) et values-fr/strings.xml (générés)
      commonTest/      40 tests unitaires de la logique métier
      jvmSharedMain/   actuals partagés Android + desktop (java.time, java.util.Locale)
      androidMain/     Application, MainActivity, Google Sign-In, service FCM, WebView,
                       bitmap de bruit, manifeste, ressources et icônes Android
      desktopMain/     point d'entrée desktop + outils de capture d'écran
  tools/               générateurs Node (chaînes, icônes)
  gradle/libs.versions.toml   catalogue de versions
```

Le code partagé ne connaît aucune API de plateforme. Les sept points de contact sont des
déclarations `expect` (`core/platform/Platform.kt`) pour la langue de l'appareil, le formatage des
dates, le stockage des préférences, la sortie de l'app, les journaux, l'ouverture d'une URL externe
et le type de plateforme, plus deux `expect` d'UI (le bruit d'ambiance et l'intégration YouTube).

La connexion Google, la connexion Apple et les notifications passent par des interfaces
(`core/platform/NativeServices.kt`) injectées dans `AppContainer` au démarrage. Sur desktop elles
prennent leur implémentation « indisponible » par défaut, ce qui permet de faire tourner toute
l'app sans SDK mobile.

## 5. Générateurs (chaînes et icônes)

Deux ressources sont dérivées de la web app pour qu'il n'y ait jamais deux sources de vérité.

**Chaînes** — 545 ressources en anglais et en français, pluriels compris, produites depuis
`src/i18n/locales/{en,fr}.ts` :

```bash
node native/tools/gen-strings.mjs
```

À relancer après chaque modification des dictionnaires web. Les textes qui n'existaient que codés en
dur dans les composants React sont regroupés dans la constante `EXTRA` du script.

**Icônes** — les glyphes Lucide utilisés par l'app, convertis en `ImageVector` Compose :

```bash
npm pack lucide-static && tar xzf lucide-static-*.tgz
node native/tools/gen-icons.mjs package/icons
```

Même viewport 24x24 et même trait de 2 px arrondi que `lucide-react` côté web, c'est ce qui fait que
les deux apps ont exactement la même silhouette d'icônes.

## 6. Tests et captures d'écran

```bash
./gradlew :composeApp:desktopTest        # 40 tests de la logique partagée
./gradlew :composeApp:screenshots        # rend le catalogue en PNG, sans écran
```

Le catalogue de captures (`desktopMain/tools/ScreenshotCatalog.kt`) rend 33 écrans sur des données
fictives, en clair et en sombre, pour comparer pixel à pixel avec la web app. Options :

```bash
./gradlew :composeApp:screenshots -PoutDir=build/shots-fr -Plocale=fr
./gradlew :composeApp:screenshots -Ponly=books-home,series-library
```

Une entrée du catalogue peut fixer sa propre taille, ce qui sert à vérifier la mise en page large
(barre latérale à partir de 768 dp) sans changer d'appareil.

## 7. Publication Play Store

`versionCode` et `versionName` sont dans `composeApp/build.gradle.kts` et valent 2 / `2.0.0` ; le
paquet Capacitor s'arrêtait à 1 / `1.0`. Les deux sont à incrémenter à chaque livraison.

Le build de release active R8 et le rétrécissement des ressources. Les règles de conservation sont
dans `composeApp/proguard-rules.pro` : sérialiseurs kotlinx, Supabase, ressources Compose,
Credential Manager et le service FCM (instancié par réflexion depuis le manifeste).

```bash
./gradlew :composeApp:bundleRelease
# composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Vérifie sur un build de release avant d'envoyer : connexion Google, réception d'une notification,
et l'ouverture des liens externes. Ce sont les trois chemins que R8 peut casser.

## 8. Limites de l'environnement de développement utilisé pour la réécriture

La réécriture a été faite dans un environnement sans accès à Google Maven ni au SDK Android. Le code
partagé y a été compilé, testé et rendu en images via la cible desktop, mais la cible Android n'a
jamais été assemblée. La première compilation dans Android Studio est donc la première vraie
compilation de `androidMain` et de la configuration Android du Gradle. Le détail de ce qui a été
vérifié et comment est dans la section 10 de `docs/native-rewrite-plan.md`.
