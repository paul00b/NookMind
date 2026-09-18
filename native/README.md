# NookMind — application native (Kotlin Multiplatform + Compose)

Réécriture native de la web app NookMind. La quasi-totalité du code (logique, réseau, écrans) vit
dans `commonMain` et sert tel quel aux deux cibles : Android est livrable, iOS a son hôte SwiftUI
dans `iosApp/` et se construit sur GitHub Actions, il lui reste les services natifs (voir la
section iOS plus bas).

Cette app remplace le paquet Capacitor (`android/`, `ios/` à la racine du dépôt), pas la web app,
qui continue de vivre à côté.

## Sommaire

1. [Ce qu'il faut installer](#1-ce-quil-faut-installer)
2. [Configuration avant le premier build](#2-configuration-avant-le-premier-build)
3. [Compiler et lancer](#3-compiler-et-lancer)
4. [Structure du projet](#4-structure-du-projet)
5. [Générateurs (chaînes et icônes)](#5-générateurs-chaînes-et-icônes)
6. [Tests et captures d'écran](#6-tests-et-captures-décran)
7. [Ce qu'il faut tester sur appareil](#7-ce-quil-faut-tester-sur-appareil)
8. [Publication Play Store](#8-publication-play-store)
9. [Limites de l'environnement de développement utilisé pour la réécriture](#9-limites-de-lenvironnement-de-développement-utilisé-pour-la-réécriture)

## 1. Ce qu'il faut installer

| Outil | Version | Remarque |
|---|---|---|
| JDK | 17 | Requis par AGP 8.13 ; le wrapper Gradle utilise le JDK du `JAVA_HOME`. |
| Android Studio | Ladybug ou plus récent | Facultatif : tout marche aussi en ligne de commande. |
| Android SDK | compileSdk 36, build-tools correspondants | `minSdk` 24 (Android 7.0), comme le paquet Capacitor. |
| Xcode | 26.4 ou plus récent | iOS uniquement, donc macOS uniquement. C'est la version que Kotlin 2.4.20 valide. |
| XcodeGen | dernière | iOS uniquement : `brew install xcodegen`. Génère le projet Xcode depuis `iosApp/project.yml`. |
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

Le build affiche un avertissement nommant chaque valeur absente et ce qu'elle casse, donc une clé
oubliée ne passe pas inaperçue. Si tu ne retrouves pas une valeur en local, elle est aussi dans les
variables d'environnement du projet Vercel qui héberge la web app.

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

**Et ce client web doit appartenir au projet Google Cloud où sont enregistrées les empreintes SHA-1
de l'application.** Un identifiant client OAuth commence par le numéro de son projet, et celui du
projet Firebase est le `project_number` de `google-services.json`. Si les deux nombres diffèrent, la
connexion Google échoue avec `{16} Account reauth failed` : le jeton est demandé à un projet qui
n'a aucun client Android déclaré pour ce package, donc rien n'y autorise l'application. Le message
ne dit rien de tout ça, d'où la valeur de cette comparaison de préfixes.

Le cas se produit facilement quand la web app et Firebase vivent dans deux projets différents.
Deux issues : déclarer le client Android dans le projet du client web, ou basculer l'application sur
le client web du projet Firebase et ajouter celui-ci à la liste des « Client IDs » du fournisseur
Google de Supabase, qui en accepte plusieurs séparés par des virgules, le client web principal en
premier. La seconde évite d'avoir à gérer les clients Android à la main à chaque nouvelle empreinte.

### `native/composeApp/google-services.json`

À télécharger depuis la console Firebase (projet `nookmind-8f5be`, application Android
`fr.paulbr.nookmind`). Sans lui le build fonctionne et l'app se lance, mais les notifications push
sont inertes : le plugin `google-services` n'est appliqué que si le fichier existe, et un
avertissement le rappelle au moment de la configuration Gradle.

**Le build debug a besoin de son propre enregistrement.** Son `applicationId` porte le suffixe
`.debug`, et le plugin `google-services` refuse de configurer un build dont l'`applicationId`
n'apparaît dans aucun client du fichier :

```
No matching client found for package name 'fr.paulbr.nookmind.debug'
```

Deux issues, au choix :

- déclarer `fr.paulbr.nookmind.debug` comme deuxième application Android du projet Firebase, puis
  retélécharger le `google-services.json` (il contiendra les deux clients). C'est ce qu'il faut de
  toute façon pour tester les notifications sur un build debug ;
- ou retirer `applicationIdSuffix = ".debug"` du bloc `debug` de `composeApp/build.gradle.kts`, au
  prix de ne plus pouvoir garder l'ancienne app et la nouvelle installées côte à côte.

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

### Aperçu desktop, le plus rapide

```bash
cd native
./gradlew :composeApp:run
```

La fenêtre fait 430x932, la taille d'un téléphone. C'est exactement le même code que sur Android,
branché sur le vrai Supabase : la bibliothèque, les recherches, les fiches, les collections et les
réglages fonctionnent pour de bon.

Deux choses ne marchent pas sur desktop, et c'est normal : la connexion Google affiche « Google
sign-in is not available on this platform » (connecte-toi par e-mail), et les réglages affichent que
les notifications ne sont pas gérées.

Les préférences locales (thème, mode d'affichage, ordre des sections, session) sont rangées dans les
préférences Java de l'utilisateur, pas dans le dépôt. Pour repartir de zéro : se déconnecter depuis
les réglages, et « Revoir l'onboarding » pour réafficher le tutoriel d'accueil.

### Quand une recherche échoue ou qu'un écran reste vide

```bash
./gradlew :composeApp:checkApis
```

La commande relit `secrets.properties`, signale toute clé absente ou restée à la valeur d'exemple,
puis appelle Google Books, TMDB, les routes Vercel et Supabase et affiche le vrai code HTTP.

C'est nécessaire parce que les trois écrans de recherche affichent « La recherche a expiré » quelle
que soit la panne, exactement comme la web app : une clé refusée est indiscernable d'une coupure
réseau. Le vrai motif part aussi dans la console de l'app, préfixé `[search]`.

Les symptômes les plus courants :

| Ce que tu vois | Cause presque toujours |
|---|---|
| « La recherche a expiré » sur Films ou Séries | `TMDB_API_KEY` absente ou refusée |
| « La recherche a expiré » sur Livres | `GOOGLE_BOOKS_API_KEY` absente, ou quota Google atteint |
| L'onglet « À suivre » reste vide en Films et Séries | même cause : il est entièrement alimenté par TMDB |
| Pas de note IMDb, pas de plateformes de streaming | `API_BASE_URL` absente |

Le build prévient déjà au moment de générer les secrets, avec une ligne par valeur manquante et ce
qu'elle casse. Si tu as raté ce message, `checkApis` le redit et va jusqu'à l'appel réseau.

L'onglet « À suivre » qui n'affiche rien quand TMDB ne répond pas est le comportement de la web app,
qui ne rend rien non plus quand les deux listes sont vides. Ce n'est pas un écran cassé.

### Android

```bash
./gradlew :composeApp:assembleDebug     # APK debug (applicationId fr.paulbr.nookmind.debug)
./gradlew :composeApp:installDebug      # installe sur l'appareil ou l'émulateur connecté
./gradlew :composeApp:bundleRelease     # AAB signé pour le Play Store
```

Le build debug porte le suffixe `.debug` sur l'`applicationId`, il cohabite donc avec la version du
Play Store sur le même appareil. Ce suffixe fait de lui une application distincte aux yeux de
Google, qui a donc besoin de son propre client OAuth Android : package `fr.paulbr.nookmind.debug`
et SHA-1 du keystore qui signe l'APK. Voir la section sur la signature de debug plus bas.

### APK construit par GitHub Actions, quand la machine locale résiste

Le workflow `.github/workflows/android-debug-apk.yml` compile l'APK debug sur un runner GitHub.
Ce runner a déjà le SDK Android, un JDK 17 et un accès direct au dépôt Maven de Google, donc il
ne dépend ni du SDK local, ni de la version de Java sur le PATH, ni de l'interception TLS d'un
antivirus. C'est la façon la plus simple d'obtenir un APK installable, et elle ne dépend d'aucune
machine en particulier.

Il se déclenche à chaque push sur `main` ou sur une branche `claude/**` qui touche `native/`, et
manuellement depuis l'onglet Actions une fois le fichier présent sur `main` (GitHub n'affiche le
bouton « Run workflow » que pour les workflows de la branche par défaut).

Récupérer l'APK : dépôt → **Actions** → le run → section **Artifacts** → `nookmind-debug-apk`.
C'est un `.zip` qui contient `NookMind-debug.apk`.

Les clés viennent des secrets du dépôt, pas de `secrets.properties` qui n'est pas versionné. À
renseigner une fois dans **Settings → Secrets and variables → Actions**, mêmes noms que dans
`secrets.properties` :

| Secret | Ce qui casse sans lui |
| --- | --- |
| `SUPABASE_URL` | la connexion et toute la bibliothèque |
| `SUPABASE_ANON_KEY` | la connexion et toute la bibliothèque |
| `API_BASE_URL` | notes IMDb, plateformes de streaming, suppression de compte |
| `TMDB_API_KEY` | recherche films et séries, et tout l'onglet À suivre |
| `GOOGLE_BOOKS_API_KEY` | recherche de livres (marche sans clé, mais fortement limitée) |
| `GOOGLE_AUTH_WEB_CLIENT_ID` | connexion Google |
| `GOOGLE_SERVICES_JSON` | notifications push — contenu de `google-services.json`, collé tel quel ou en base64 |
| `DEBUG_KEYSTORE_BASE64` | signature stable — sans lui, connexion Google impossible et réinstallation refusée |

Un secret absent ne fait pas échouer le build : il produit une app dont la fonctionnalité
correspondante est morte, et le résumé du run liste ce qui manque.

Le dépôt est public, donc l'artefact est téléchargeable par n'importe qui. Ça ne change rien au
niveau d'exposition : toutes ces clés sont déjà dans le bundle JavaScript du site web, et la clé
anon Supabase est publique par conception (c'est le RLS qui protège les données). Le keystore de
release, lui, n'est jamais utilisé par ce workflow.

### La signature de debug, et pourquoi elle doit être figée

Google Sign-In s'appuie sur le certificat qui signe l'APK : le Credential Manager exige un client
OAuth Android déclaré pour le couple exact (nom de package, SHA-1). Une même clé doit donc signer
tous les builds debug, où qu'ils soient produits, et ce doit être celle déclarée chez Google. Deux
APK signés différemment refusent par ailleurs de se remplacer l'un l'autre sur l'appareil.

Sans indication, AGP signe avec le `~/.android/debug.keystore` de la machine, en en fabriquant un
s'il n'y en a pas. C'est juste sur la machine de développement dont l'empreinte est enregistrée, et
faux partout ailleurs, à commencer par un runner GitHub qui démarre vierge.

**La clé de référence est donc le `~/.android/debug.keystore` de la machine de développement**, dont
le SHA-1 est déclaré dans le client OAuth Android « Android Debug » du projet Google Cloud de la web
app. Conséquence pratique : sur cette machine il n'y a rien à faire, AGP la trouve tout seul.
Ailleurs, et sur CI, `native/composeApp/debug.keystore` porte la même clé. Le fichier est ignoré par
git, et sur CI il est écrit depuis le secret `DEBUG_KEYSTORE_BASE64` :

```bash
base64 -i ~/.android/debug.keystore -o keystore.txt   # macOS
base64 -w 0 ~/.android/debug.keystore > keystore.txt  # Linux
```

Chaque run GitHub relit l'empreinte dans l'APK produit avec `apksigner` et l'affiche dans son
résumé. C'est cette valeur, et pas celle du keystore, qui doit correspondre à ce qui est déclaré
chez Google : elle prouve que la configuration de signature a bien été appliquée.

### Connexion Google sur un build debug

Le suffixe `.debug` fait du build de test une application distincte aux yeux de Google. Il lui faut
donc son propre client OAuth Android, dans **le projet Google Cloud qui possède le client web passé
en `GOOGLE_AUTH_WEB_CLIENT_ID`** — pas ailleurs, et notamment pas dans le projet Firebase si les
deux diffèrent. C'est le client nommé « Android Debug », avec le package `fr.paulbr.nookmind.debug`
et le SHA-1 de la clé ci-dessus.

Un client OAuth Android ne porte qu'un seul couple package plus empreinte, et Google impose son
unicité entre projets. Changer de clé de signature veut donc dire modifier ce client, pas en
ajouter un second.

### Notifications push sur un build debug

Celles-ci passent par Firebase, qui peut être un autre projet. Il faut que
`fr.paulbr.nookmind.debug` y existe comme application Android, pour que `google-services.json`
contienne un client à son nom : sans quoi le plugin `google-services` fait échouer le build.

Aucune empreinte SHA-1 n'est nécessaire pour les notifications. Firebase n'en demande une que pour
la connexion Google, les Dynamic Links et l'authentification par téléphone.

Console Firebase, projet `nookmind-8f5be` :

1. Paramètres du projet, **Ajouter une application** → Android.
2. Nom du package : `fr.paulbr.nookmind.debug`. Laisser le champ SHA-1 vide.
3. Télécharger le `google-services.json` obtenu. Il contient désormais les deux clients, celui de
   production et celui de debug, et remplace l'ancien.
4. Pour CI : ouvrir le fichier, tout sélectionner, coller dans le secret `GOOGLE_SERVICES_JSON`.
   Le workflow accepte aussi bien le JSON brut que sa version base64, donc aucune commande
   d'encodage à trouver. Un contenu qui n'est ni l'un ni l'autre est écarté avec un message
   dans le résumé du run, plutôt que de faire échouer le build.

### iOS

Tout se passe sur un Mac, et rien d'autre ne peut compiler cette cible : Kotlin/Native pour Apple
a besoin de Xcode. Le `build.gradle.kts` ne déclare d'ailleurs les cibles `iosArm64` et
`iosSimulatorArm64` que sur un hôte macOS, ce qui fait que `src/iosMain/` est simplement ignoré
partout ailleurs et que le build Android sous Linux n'en sait rien.

```bash
brew install xcodegen                 # une fois
cd native/iosApp
xcodegen generate                     # produit NookMind.xcodeproj, ignoré par git
open NookMind.xcodeproj               # puis Cmd+R sur un simulateur
```

Le projet Xcode est **généré** depuis `iosApp/project.yml` et n'est pas versionné : un réglage
changé dans l'interface de Xcode est perdu au prochain `xcodegen generate`, il faut le faire dans
le `project.yml`. Ce qui est propre à ta machine va dans `iosApp/NookMind/Local.xcconfig` (ignoré
par git), typiquement ta team de signature :

```
DEVELOPMENT_TEAM = SJ42PK8VK6
```

Une phase de build du projet appelle `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`,
qui compile le framework Kotlin `ComposeApp` et copie les ressources Compose dans le bundle. Xcode
lancé depuis le Finder n'hérite pas du `JAVA_HOME` du shell : le script le retrouve avec
`/usr/libexec/java_home -v 17`, à défaut prend le JDK d'Android Studio.

**Ce que le simulateur couvre :** tout le code partagé, la connexion e-mail, la bibliothèque, les
recherches, les fiches, les collections, les réglages, le trailer YouTube (WKWebView). C'est
exactement ce que le workflow `.github/workflows/ios-simulator-build.yml` vérifie à chaque push
touchant `native/` : il compile, fait tourner les tests partagés sur cible iOS, construit l'app,
la lance, vérifie qu'elle tourne encore 25 secondes plus tard et dépose des captures (clair,
sombre, français) dans les artefacts du run.

**Ce que le simulateur ne couvre pas, et rien d'automatique ne le fera :**

| Fonctionnalité | Ce qu'il lui manque | Où ça se branche |
|---|---|---|
| Sign in with Apple | la capacité sur l'App ID (équipe Apple Developer payante), puis passer `NookMindAppleSignInEnabled` à `true` dans `Info.plist` et ajouter `com.apple.developer.applesignin` aux entitlements | `AppleSignInBridge.swift`, déjà écrit |
| Connexion Google | le paquet SPM `GoogleSignIn-iOS`, un client OAuth **iOS** dans le projet Google Cloud du client web (comme sur Android), et le schéma d'URL inversé dans `Info.plist` | implémenter `IosGoogleSignInBridge` en Swift |
| Notifications push | le paquet SPM `firebase-ios-sdk` (FirebaseMessaging), `GoogleService-Info.plist` dans `iosApp/NookMind/`, la clé APNs téléversée dans Firebase, `aps-environment` aux entitlements | implémenter `IosPushBridge` en Swift, brancher l'AppDelegate |
| Haptiques | un vrai iPhone : le simulateur n'a pas de moteur haptique | rien à écrire a priori, Compose mappe `HapticFeedbackType` vers UIKit ; à vérifier signal par signal |

Les trois interfaces de pont (`IosAppleSignInBridge`, `IosGoogleSignInBridge`, `IosPushBridge`)
sont définies côté Kotlin dans `iosMain/.../ios/Bridges.kt`. Elles prennent des callbacks et pas
des `suspend`, parce que Kotlin ne sait pas appeler en suspension une méthode implémentée en
Swift ; l'adaptation vers les `suspend` des providers communs est faite là aussi. Les nonces
(SHA-256 donné au fournisseur, valeur brute donnée à Supabase) y sont générés, Swift ne fait que
présenter les écrans système. Un pont laissé à `nil` dans `AppDelegate.swift` donne au code
partagé l'implémentation « indisponible » du desktop : le bouton disparaît, rien ne casse.

### Le SDK Android est requis même pour l'aperçu desktop

Le module `composeApp` déclare la cible Android, donc le plugin Android est configuré à chaque
invocation de Gradle. Sans SDK :

```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
or by setting the sdk.dir path in your project's local.properties file.
```

Installer Android Studio suffit (il pose le SDK et renseigne le chemin). Sinon, créer
`native/local.properties` avec `sdk.dir=/chemin/vers/Android/sdk`, ou exporter `ANDROID_HOME`.
Ce fichier est propre à ta machine et ne doit pas être versionné.

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
      commonTest/      56 tests unitaires de la logique métier
      jvmSharedMain/   actuals partagés Android + desktop (java.time, java.util.Locale)
      androidMain/     Application, MainActivity, Google Sign-In, service FCM, WebView,
                       manifeste, ressources et icônes Android
      desktopMain/     point d'entrée desktop + outils de capture d'écran
      iosMain/         actuals iOS (NSDateFormatter, NSUserDefaults, WKWebView), ponts vers Swift,
                       IosApp (racine de composition) et MainViewController
  iosApp/              hôte SwiftUI : project.yml (XcodeGen), AppDelegate, ComposeView,
                       AppleSignInBridge, Info.plist, entitlements, PrivacyInfo, assets
  tools/               générateurs Node (chaînes, icônes)
  gradle/libs.versions.toml   catalogue de versions
```

Le code partagé ne connaît aucune API de plateforme. Les huit points de contact sont des
déclarations `expect` (`core/platform/Platform.kt`) pour la langue de l'appareil, le formatage des
dates, le stockage des préférences, la sortie de l'app, les journaux, l'ouverture d'une URL externe,
le type de plateforme et le niveau d'API pour les haptiques, plus un `expect` d'UI (l'intégration
YouTube). C'est tout ce qu'une nouvelle plateforme a à fournir : les neuf `actual` iOS tiennent
dans deux fichiers.

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
./gradlew :composeApp:desktopTest        # 56 tests de la logique partagée
./gradlew :composeApp:iosSimulatorArm64Test  # les mêmes, sur un simulateur iOS (macOS uniquement)
./gradlew :composeApp:screenshots        # rend le catalogue en PNG, sans écran
./gradlew :composeApp:checkApis          # vérifie les secrets et appelle chaque backend
```

Sur macOS, `--device="iPhone 17"` (nom ou UDID, `xcrun simctl list devices` pour la liste) choisit
le simulateur des tests iOS ; sans lui, c'est le modèle par défaut du plugin Kotlin, qui n'existe
pas toujours dans le Xcode installé. C'est ce que fait la CI, qui lit la liste du runner.

Le catalogue de captures (`desktopMain/tools/ScreenshotCatalog.kt`) rend 35 écrans sur des données
fictives, en clair et en sombre, pour comparer pixel à pixel avec la web app. Options :

```bash
./gradlew :composeApp:screenshots -PoutDir=build/shots-fr -Plocale=fr
./gradlew :composeApp:screenshots -Ponly=books-home,series-library
```

Une entrée du catalogue peut fixer sa propre taille, ce qui sert à vérifier la mise en page large
(barre latérale à partir de 768 dp) sans changer d'appareil.

## 7. Ce qu'il faut tester sur appareil

Le code partagé est couvert par les tests et les captures. Ce qui suit ne l'est pas : ce sont les
chemins qui touchent au système Android, invérifiables ailleurs que sur un vrai téléphone.

### Ce qui change pour un utilisateur déjà installé

L'ancienne app stockait sa session et ses préférences dans le `localStorage` de la WebView
Capacitor. L'app native utilise les préférences Android. Les clés sont les mêmes, le support ne
l'est pas : **rien n'est repris automatiquement**. Concrètement, à la mise à jour :

| | |
|---|---|
| Livres, films, séries, collections, notes, avancement | Intacts, ils vivent dans Supabase |
| Session | Perdue : il faut se reconnecter une fois |
| Onboarding | Réaffiché une fois |
| Thème, mode d'affichage, ordre des sections | Remis par défaut |
| Notifications | À réactiver dans les réglages (le jeton FCM change) |

Aucune donnée n'est perdue, mais la première ouverture n'est pas silencieuse. À tester en
priorité : installer l'app native par-dessus l'ancienne (même `applicationId`, même keystore) et
vérifier que la reconnexion ramène bien toute la bibliothèque.

### Check-list

Connexion
- [ ] Connexion Google (le chemin le plus fragile : il dépend des SHA-1 déclarés)
- [ ] Connexion e-mail + mot de passe, et création de compte
- [ ] Déconnexion, puis reconnexion : la bibliothèque revient
- [ ] Fermer et rouvrir l'app : la session est restaurée sans repasser par l'écran de connexion

Données
- [ ] Ajouter un livre, un film, une série ; vérifier qu'ils apparaissent aussi dans la web app
- [ ] Modifier une note, un avancement, une date : même chose dans les deux sens
- [ ] Créer une collection, y ranger des éléments, la supprimer
- [ ] Supprimer un élément

Système
- [ ] Notifications : activer, envoyer le test depuis les réglages, recevoir, toucher la
      notification et vérifier qu'elle ouvre le bon écran
- [ ] Désactiver les notifications, puis vérifier qu'il n'en arrive plus
- [ ] Trailer YouTube : lecture et plein écran
- [ ] Liens externes (plateformes de streaming) : ouverture dans le navigateur ou l'app dédiée
- [ ] Bouton retour Android depuis chaque feuille et chaque écran
- [ ] Rotation et retour, clavier qui ne masque pas les champs
- [ ] Thème clair, sombre, et suivi du réglage système

À faire sur un build **de release** et pas seulement en debug : R8 peut casser la connexion Google,
la réception des notifications et la désérialisation des réponses réseau, et ces trois chemins ne
sont pas couverts par les tests.

## 8. Publication Play Store

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

## 9. Limites de l'environnement de développement utilisé pour la réécriture

La réécriture a été faite dans un environnement sans accès à Google Maven ni au SDK Android. Le code
partagé y a été compilé, testé et rendu en images via la cible desktop, mais la cible Android n'a
jamais été assemblée. La première compilation dans Android Studio est donc la première vraie
compilation de `androidMain` et de la configuration Android du Gradle. Le détail de ce qui a été
vérifié et comment est dans la section 10 de `docs/native-rewrite-plan.md`.
