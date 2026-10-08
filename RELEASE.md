# Publier NookMind

Cette page concerne l'app Android native (`native/`). Le site et les fonctions `api/` sont sur
Vercel. La publication complète sur le Play Store (fiche, déclarations, test
fermé de 14 jours) est dans [`docs/play-store-publication.md`](docs/play-store-publication.md).

## Sortir une version Android

Le build se fait sur GitHub, pas sur le Mac : rien à installer, rien à compiler en local.

1. **Notes de version.** Modifier `native/distribution/whatsnew/whatsnew-fr-FR` et
   `whatsnew-en-US` (500 caractères maximum chacune), commit, push sur `main`.
2. **Lancer.** GitHub → **Actions** → **Android release** → **Run workflow**, branche `main` :
   - *Version* : laisser vide pour prendre la dernière version sortie et ajouter 1 au dernier
     chiffre (2.0.3 donne 2.0.4). Écrire `2.1.0` pour changer de palier.
   - *Track* : `alpha` par défaut, le test fermé. Google relit chaque release avant que les
     testeurs la reçoivent, de quelques heures à quelques jours. `internal` envoie en test interne,
     sans relecture et disponible en quelques minutes : utile pour essayer un build soi-même avant
     de le donner aux testeurs, puis le promouvoir en test fermé depuis Play Console (même AAB,
     pas de nouvelle version).
   - *Status* : `completed` pour que les testeurs l'aient tout de suite, `draft` pour la valider
     soi-même dans Play Console avant.
3. **Attendre une dizaine de minutes.** À la fin du run :
   - la version est dans Play Console, sur la piste choisie (en test fermé, en attente de la
     relecture de Google) ;
   - le commit porte le tag `android-vX.Y.Z` ;
   - l'AAB, un APK installable et le fichier de mapping R8 sont dans les **Artifacts** du run.

Le résumé du run donne la version, l'empreinte de la clé qui a signé, le nombre d'erreurs R8 et la
liste des permissions demandées par l'app.

### Le numéro de version

Il n'y a qu'un numéro à choisir, `X.Y.Z`. Le `versionCode` qu'exige Google en est déduit :
`X × 10000 + Y × 100 + Z`, donc 2.0.1 donne 20001. Conséquences :

- `Y` et `Z` vont de 0 à 99 ;
- plus rien à incrémenter à la main dans `build.gradle.kts` ;
- un build local sans paramètre sort en `2.0.0` (20000). Pour en forcer un autre :
  `./gradlew :composeApp:bundleRelease -PappVersionName=2.0.4`.

Le workflow refuse une version déjà sortie (le tag existe) ou inférieure à la dernière : Play la
rejetterait de toute façon, mais après dix minutes de build.

### Ce qui arrête une release

Plutôt qu'une app qui part avec une fonction morte, le workflow s'arrête si :

- un des six secrets du backend manque (Supabase, TMDB, Google Books, API Vercel, client Google) ;
- `GOOGLE_SERVICES_JSON` ne déclare pas `fr.paulbr.nookmind` (plus de notifications) ;
- le keystore ne s'ouvre pas avec le mot de passe donné, ou l'AAB sort non signé ;
- l'app demande l'identifiant publicitaire (`AD_ID`) alors que Play Console déclare que non.

### Le contrôle à chaque push

Le même workflow tourne aussi, sans rien envoyer, à chaque push qui modifie un fichier Gradle ou
`proguard-rules.pro`. Le build debug ne passe pas par R8 : c'est le seul endroit où une règle de
conservation cassée se voit avant qu'une release ne parte avec.

## Mise en place, une seule fois

### 1. La clé de signature dans GitHub

Dépôt → **Settings** → **Secrets and variables** → **Actions** → **New repository secret** :

| Secret | Valeur |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Sur le Mac : `base64 -i ~/nookmind-release.jks \| pbcopy`, puis coller |
| `RELEASE_KEYSTORE_PASSWORD` | Le `storePassword` de `native/keystore.properties` |
| `RELEASE_KEY_PASSWORD` | Le `keyPassword`. Facultatif s'il est identique au précédent |
| `RELEASE_KEY_ALIAS` | Facultatif, `nookmind` par défaut |

Les six secrets du backend et `GOOGLE_SERVICES_JSON` existent déjà, le workflow debug s'en sert.

Un secret GitHub ne se relit pas : ce n'est **pas** une sauvegarde du keystore. Le `.jks` et ses mots
de passe doivent rester dans un gestionnaire de mots de passe, en plus du Mac.

### 2. Le premier envoi, à la main

L'API de Google ne connaît une app qu'une fois qu'un premier bundle a été envoyé à la main dans
Play Console. Donc, la première fois :

1. Lancer **Android release** (sans le secret `PLAY_SERVICE_ACCOUNT_JSON`, le workflow construit et
   signe, mais n'envoie rien).
2. Télécharger l'artefact du run, en sortir `NookMind-2.0.0.aab`.
3. Play Console → **Tester et publier** → **Tests** → **Tests internes** → **Créer une release** →
   envoyer l'AAB. Suite dans `docs/play-store-publication.md` §6.1.

### 3. Le compte de service Play, pour que les envois suivants se fassent seuls

1. **Google Cloud Console**, dans le projet Firebase `nookmind-8f5be` (n'importe quel projet
   convient, autant ne pas en créer un) → **APIs & Services** → **Library** → activer
   **Google Play Android Developer API**.
2. **IAM & Admin** → **Service Accounts** → **Create service account**, par exemple
   `play-release`. Aucun rôle Google Cloud n'est nécessaire. Puis **Keys** → **Add key** →
   **JSON** : un fichier se télécharge.
3. **Play Console** → **Utilisateurs et autorisations** → **Inviter de nouveaux utilisateurs** →
   l'adresse e-mail du compte de service (`play-release@nookmind-8f5be.iam.gserviceaccount.com`).
   Onglet **Autorisations des applications** → ajouter NookMind → cocher seulement **Publier des
   applications sur les canaux de test**. Inviter.
4. Secret GitHub `PLAY_SERVICE_ACCOUNT_JSON` : tout le contenu du fichier JSON. Supprimer ensuite le
   fichier du Mac.

Les autorisations Play peuvent mettre un moment à s'appliquer. Si le premier envoi automatique
répond `The caller does not have permission`, réessayer plus tard avant de chercher plus loin.

Si l'envoi répond `Only releases with status draft may be created on draft app`, c'est que l'app
n'a encore jamais été publiée sur aucune piste : relancer avec *Status* `draft`, puis déployer la
release depuis Play Console.

## Tester une release

Tester la version installée **depuis le Play Store**, pas seulement l'APK : c'est elle que Google a
re-signée, et c'est sur elle que la connexion Google peut casser
(`docs/play-store-publication.md` §6.2). Pour recevoir les releases du test fermé, il faut être
soi-même dans la liste de ses testeurs et inscrit par son lien : un téléphone inscrit seulement au
test interne reste sur la dernière version envoyée en interne.

R8 minifie la release et peut casser ce qui marche en debug. À vérifier à chaque release qui touche
aux dépendances ou aux règles ProGuard :

- [ ] connexion Google et connexion e-mail
- [ ] notification : activer, envoyer le test depuis les réglages, la recevoir, la toucher
- [ ] ouverture d'un lien vers une plateforme de streaming
- [ ] trailer YouTube
- [ ] suppression de compte, avec un compte jetable

Les plantages remontent dans Play Console (**Surveiller et améliorer** → **Plantages et ANR**),
avec les vrais noms de classes : le workflow envoie le mapping R8 avec chaque bundle.

## iOS

Même principe qu'Android : GitHub → Actions → **iOS release** → Run workflow, et le build part sur
TestFlight. Il faut d'abord un compte Apple Developer payant, quelques réglages dans les consoles et
six secrets GitHub : tout est dans [`docs/app-store-publication.md`](docs/app-store-publication.md).
Avant de payer, l'app se teste sur un iPhone avec un Apple ID gratuit : `docs/ios-test-plan.md`.

## L'ancien paquet Capacitor

`android/` reste le seul moyen de corriger l'app Capacitor tant qu'elle est sur le Play Store. Son
build, sur le Mac :

```bash
npm install --legacy-peer-deps
npm run cap:sync
cd android && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew bundleRelease
# android/app/build/outputs/bundle/release/app-release.aab
```

Il utilise `android/keystore.properties`, même keystore et même alias que l'app native.

## Infos techniques

| Élément | Valeur |
|---|---|
| App ID | `fr.paulbr.nookmind` (Android et iOS) |
| Version | `X.Y.Z`, versionCode `X×10000 + Y×100 + Z`. Le paquet Capacitor s'était arrêté à 1 / `1.0` |
| Min / target Android SDK | 24 (Android 7.0) / 36 |
| Clé d'upload | `~/nookmind-release.jks`, alias `nookmind`, SHA-1 `6E:F9:BE:07:50:1A:CC:74:82:54:69:4B:48:90:36:C7:84:1D:CA:A5` |
| Projet Firebase | `nookmind-8f5be` |
| Workflow de release | `.github/workflows/android-release.yml` |
| Notes de version | `native/distribution/whatsnew/` |
