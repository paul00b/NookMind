# Où en est cette branche, et ce qu'il reste

Branche : `claude/optimistic-albattani-rj2h54`
Dernière mise à jour : 2026-09-18

## Fait

### La réécriture native

- 113 fichiers Kotlin, l'essentiel dans `commonMain` et donc réutilisable par iOS
- 140 tests, 0 échec (70 desktop + 70 Android), 35 captures d'écran
- Le retour haptique : 5 signaux nommés par intention (`HapticCue`) avec repli par niveau d'API,
  un interrupteur dans les réglages, échecs silencieux en arrière-plan.
  Design : `docs/superpowers/specs/2026-09-16-haptics-design.md`.
  Plan et historique de revue : `docs/superpowers/plans/2026-09-16-haptics.md`.
  Vérifié sur appareil, les cinq signaux sont justes.

### Le build Android sur GitHub Actions

`.github/workflows/android-debug-apk.yml` compile un APK debug installable à chaque push touchant
`native/`, sans dépendre d'aucune machine locale. Récupération : dépôt → **Actions** → le run →
**Artifacts** → `nookmind-debug-apk`.

Les 8 secrets du dépôt sont renseignés, l'étape « What is missing » ne signale plus rien, et les
derniers runs sont verts. L'empreinte de la clé qui signe réellement l'APK, relue par `apksigner`
dans le fichier produit :

```
00:88:4A:0C:2F:67:77:0F:F9:AC:7E:45:89:1B:AD:3D:5B:80:C0:8C
```

### La connexion Google sur Android

Confirmée fonctionnelle sur appareil. Elle a demandé trois corrections enchaînées, documentées dans
les commits `84bcd09`, `7460e68` et `6b89924` :

1. `GetGoogleIdOption` est l'API du bandeau One Tap passif, qui ne propose que les comptes ayant
   déjà autorisé l'app. Remplacée par `GetSignInWithGoogleOption`, qui ouvre toujours le sélecteur.
2. Supabase compare la revendication `nonce` du jeton au SHA-256 de la valeur brute qu'on lui
   passe. Le code hachait le nonce mais perdait la valeur brute.
3. Le client web OAuth passé en `GOOGLE_AUTH_WEB_CLIENT_ID` doit appartenir au **même projet Google
   Cloud** que celui où sont déclarées les empreintes SHA-1. Sinon : `{16} Account reauth failed`,
   dont le message ne dit rien de la cause.

### L'historique git

La branche partait d'un commit racine sans aucun ancêtre commun avec `main`, ce qui la rendait
impossible à merger. Les 50 commits ont été rejoués sur `main` ; l'arbre résultant est identique au
bit près à ce qu'il était avant. La branche est maintenant un descendant linéaire de `main`,
50 commits d'avance, 0 de retard.

## Configuration machine

La machine de développement est un **Mac**. Trois fichiers vivent hors de git et doivent exister
avant tout build. Le détail complet est dans `native/README.md` §2 ; en résumé :

1. **`native/secrets.properties`** — six clés, les mêmes que les `VITE_*` du `.env` du site.
2. **`native/composeApp/google-services.json`** — projet Firebase `nookmind-8f5be`. Doit déclarer
   `fr.paulbr.nookmind` **et** `fr.paulbr.nookmind.debug`, sinon le plugin Gradle fait échouer le
   build debug.
3. **`native/keystore.properties`** — uniquement pour les builds de release. Voir plus bas.

Sur le Mac, la clé de signature debug est le `~/.android/debug.keystore` habituel et AGP la trouve
seul : il n'y a rien à faire. C'est ailleurs, et sur CI, qu'il faut `native/composeApp/debug.keystore`,
écrit depuis le secret `DEBUG_KEYSTORE_BASE64`.

> **La `GOOGLE_BOOKS_API_KEY` du `.env` est périmée** et renvoie 503. Une clé plus récente l'a
> remplacée ; reconstruire `secrets.properties` depuis `.env` sans reporter la nouvelle casse à
> nouveau la recherche de livres. La copie de référence est dans Google Cloud Console,
> *APIs & Services → Credentials*. Ça vaut le coup de mettre aussi `.env` et l'environnement Vercel
> à jour pour que le site et l'app native cessent de diverger.
>
> Cette clé n'est pas un mot de passe : elle est compilée dans l'APK et servie dans le bundle du
> site, donc lisible dans les deux cas. Ce qui la protège, c'est la restriction posée dessus dans
> Google Cloud (la limiter à l'API Books et aux plateformes censées l'appeler).

Tout vérifier d'un coup : `./gradlew :composeApp:checkApis`, qui relit les secrets et appelle chaque
backend en affichant le vrai code HTTP.

## Ce qu'il reste, dans l'ordre

### 1. Le keystore de release

C'est le blocage pour publier. **Première chose à faire : regarder `~/nookmind-release.jks` sur le
Mac.** `RELEASE.md` l'y situe explicitement, avec l'alias `nookmind` et le mot de passe dans un
`android/keystore.properties` non versionné.

Une version précédente de ce document concluait « introuvable, cherché » : la recherche avait été
faite sur une machine Windows qui n'est pas la machine de développement. Cette conclusion ne vaut
rien, le keystore n'a jamais été cherché au bon endroit.

S'il est réellement perdu, aller voir Play Console → *Setup → App signing*. Si **Play App Signing**
est actif, Google détient la vraie clé et la clé d'upload peut être réinitialisée, donc c'est
récupérable. Sinon la fiche ne peut plus jamais être mise à jour et il faudrait republier sous un
autre nom de package.

Une fois trouvé, créer `native/keystore.properties` :

```properties
storeFile=/chemin/absolu/vers/nookmind-release.jks
storePassword=…
keyAlias=nookmind
keyPassword=…
```

### 2. Compiler et tester une release signée

```bash
cd native
./gradlew :composeApp:assembleRelease   # APK
./gradlew :composeApp:bundleRelease     # AAB pour le Play Store
```

La release utilise l'`applicationId` `fr.paulbr.nookmind`, celui déjà publié, donc les SHA-1
enregistrés chez Google valent déjà et il n'y a rien à changer côté console.

**Retester sur le build de release en particulier.** R8 peut casser exactement trois choses, et rien
d'autre ne les rattrape :

- [ ] connexion Google (elle marche en debug, ça ne prouve rien pour la release)
- [ ] réception d'une notification
- [ ] ouverture des liens externes (plateformes de streaming)

Penser à incrémenter `versionCode` / `versionName` dans `composeApp/build.gradle.kts` (actuellement
2 / `2.0.0` ; le paquet Capacitor s'était arrêté à 1 / `1.0`).

### 3. Le reste de la check-list appareil

La liste complète est dans `native/README.md` §7. Ce qui n'est pas encore coché :

- [ ] notifications : activer, envoyer le test depuis les réglages, recevoir, **toucher** la
      notification et vérifier qu'elle ouvre le bon écran et pas seulement l'accueil
- [ ] installer par-dessus l'ancienne app et vérifier que la reconnexion ramène toute la
      bibliothèque
- [ ] trailer YouTube, lecture et plein écran
- [ ] bouton retour Android depuis chaque feuille et chaque écran
- [ ] rotation, clavier qui ne masque pas les champs

### 4. Ce que les utilisateurs déjà installés vont vivre

L'app Capacitor rangeait sa session dans le `localStorage` de la WebView, l'app native utilise les
préférences Android. Mêmes clés, support différent : **rien ne migre automatiquement**.

| | |
|---|---|
| Livres, films, séries, collections, notes, avancement | Intacts, ils vivent dans Supabase |
| Session | Perdue, une reconnexion |
| Onboarding | Réaffiché une fois |
| Thème, mode d'affichage, ordre des sections | Remis par défaut |
| Notifications | À réactiver (le jeton FCM change) |

### 5. Supprimer Capacitor

Une fois l'app native publiée et validée en production, et pas avant : `android/`, `ios/`,
`capacitor.config.ts`, `scripts/fix-spm-paths.cjs`, `resources/`, les dépendances `@capacitor/*` et
les scripts `cap:*` du `package.json`.

Tant que le Play Store sert la version Capacitor, ce dossier est le seul moyen de livrer un
correctif aux personnes qui l'ont déjà installée.

### 6. iOS

La phase A est écrite : les neuf `actual` dans `iosMain/`, les ponts vers Swift, la racine de
composition `IosApp`, l'hôte SwiftUI dans `native/iosApp/` (projet généré par XcodeGen), et le
workflow `ios-simulator-build.yml` qui compile, teste, lance sur simulateur et prend des captures
sur un runner macOS 26. Le détail, et ce que le simulateur ne peut pas couvrir, est dans
`native/README.md`, section iOS.

Ce qu'il reste, dans l'ordre :

- [x] premier run vert le 2026-09-18 : Xcode 26.6 accepté par Kotlin 2.4.20 sans avertissement,
      tests partagés verts sur iPhone 17 Pro / iOS 26.5, app lancée et vivante, captures produites.
      18 minutes à froid, 10 avec les caches
- [ ] sur le Mac : `brew install xcodegen`, `cd native/iosApp && xcodegen generate`, ouvrir le
      projet, `Local.xcconfig` avec la team, lancer sur simulateur puis sur l'iPhone
- [ ] compte Apple Developer payant, puis Sign in with Apple (capacité + entitlement + le flag
      `NookMindAppleSignInEnabled`)
- [ ] connexion Google iOS (`GoogleSignIn-iOS`, client OAuth iOS dans le bon projet Google Cloud)
- [ ] notifications (`firebase-ios-sdk`, `GoogleService-Info.plist`, clé APNs)
- [ ] vérifier les cinq signaux haptiques sur l'appareil
- [ ] TestFlight

## Travaux reportés

**Spec 2, actée mais pas écrite :** menu d'actions rapides en appui long sur les cartes de la
bibliothèque, et tirer pour rafraîchir sur tous les écrans principaux. Les deux héritent gratuitement
du vocabulaire haptique : l'activation de l'appui long et le seuil du tirage sont exactement les
moments ambigus que les haptiques servent à lever.

**Problèmes préexistants trouvés pendant la revue, aucun introduit par ce travail :**

- `NookToggle` est invisible pour TalkBack : une `Box` avec `.clickable`, sans `Modifier.toggleable`,
  sans `Role.Switch`, sans `stateDescription`. Concerne les quatre interrupteurs.
- Toucher une étoile déjà sélectionnée déclenche quand même un PATCH réseau et un toast de succès
  pour une opération nulle. L'haptique est correctement silencieuse là ; l'écriture ne l'est pas.
- `CollectionChip` ressemble à une puce, se trouve juste sous une rangée de puces qui réagissent, et
  est entièrement muet : pas d'haptique, et son chemin de succès ne lève pas de toast non plus.

**Niveau non testé :** le repli par version d'API n'est pas vérifiable sur le téléphone Android 16,
qui est sur le niveau le plus haut où tout marche nativement. Un émulateur API 29 ou 31 confirmerait
que les puces, notes, interrupteurs et l'onboarding vibrent bien sur les appareils plus anciens. À
faire avant une publication sur le store ; l'image système est un téléchargement d'environ 1 Go et
aucun AVD de ce type n'existe encore.
