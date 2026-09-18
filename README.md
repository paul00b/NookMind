# NookMind

Suivi personnel de lectures, films et séries. Un seul dépôt, plusieurs produits qui partagent la
même base de données et les mêmes textes.

## Ce que contient ce dépôt

| Dossier | Ce que c'est | Où ça tourne |
|---|---|---|
| racine (`src/`, `index.html`, `vite.config.ts`) | le site React 19 + Vite, qui est aussi la PWA | Vercel |
| `api/` | 9 fonctions serverless TypeScript | Vercel |
| `native/` | l'app mobile native, Kotlin Multiplatform + Compose ; l'hôte iOS est dans `native/iosApp/` | Play Store (Android), App Store (iOS, en cours) |
| `android/`, `ios/`, `capacitor.config.ts` | l'ancien paquet Capacitor, le site emballé dans une WebView | Play Store, jusqu'à la bascule vers `native/` |
| `supabase-*.sql` | le schéma Postgres, migrations dans l'ordre | Supabase |
| `docs/` | plan de réécriture native, état d'avancement, specs |  |

Le site et l'app native sont deux implémentations du même produit, pas deux produits. Ils ne
partagent pas de code (TypeScript d'un côté, Kotlin de l'autre) mais ils partagent trois choses
réelles, et c'est la raison pour laquelle tout vit dans un seul dépôt :

**Les textes.** `native/tools/gen-strings.mjs` lit `src/i18n/locales/{en,fr}.ts` et en génère les
545 chaînes Compose de `native/composeApp/src/commonMain/composeResources/`. Une seule source de
vérité pour les libellés des deux apps. À relancer après chaque modification des dictionnaires web.

**Les endpoints.** L'app native appelle les mêmes fonctions Vercel que le site, via son
`API_BASE_URL` : `/api/imdb-graphql`, `/api/imdb-suggest`, `/api/watch-providers`, `/api/push/test`
et `/api/account/delete`. Ce ne sont pas des équivalents, c'est le même code qui tourne.

**Le schéma.** Les `supabase-*.sql` valent pour les deux, et les modèles Kotlin de
`native/composeApp/src/commonMain/kotlin/fr/paulbr/nookmind/core/model/` suivent les mêmes tables
que les types TypeScript du site.

Séparer en plusieurs dépôts casserait ces trois liens. À l'inverse, un découpage en
`apps/` + `packages/` n'apporterait rien : `packages/` sert à partager du code, et il n'y a pas de
code partageable entre du TypeScript et du Kotlin.

## Le site et la PWA

```bash
npm install --legacy-peer-deps   # les peer deps Capacitor 8 ne s'accordent pas toutes seules
npm run dev                      # serveur de dev
npm run build                    # tsc -b puis vite build, sortie dans dist/
npm run test                     # vitest
npm run lint                     # eslint
```

La PWA est produite par `vite-plugin-pwa` en stratégie `injectManifest`, le service worker est
`src/sw.ts`. Elle est désactivée quand `VITE_NATIVE_BUILD=1`, c'est-à-dire dans le build destiné à
Capacitor, où un service worker n'aurait aucun sens.

Les variables d'environnement sont listées dans `.env.example`. Elles sont aussi dans les réglages
du projet Vercel, qui est la copie de référence si le `.env` local se perd.

## Les fonctions serverless

Elles vivent dans `api/` et Vercel les déploie sans configuration : un fichier, une route. La seule
chose non évidente est le cron de `vercel.json`, qui appelle `/api/push/send-daily` tous les jours à
9 h UTC pour les notifications quotidiennes.

## L'app native

Tout est documenté dans [`native/README.md`](native/README.md) : prérequis, les trois fichiers de
configuration hors git, comment compiler, les générateurs, les tests et le catalogue de captures.

Le raccourci utile : un APK debug installable sort de GitHub Actions à chaque push touchant
`native/`, sans dépendre d'aucune machine locale. Dépôt → **Actions** → le run →
**Artifacts** → `nookmind-debug-apk`.

```bash
cd native
./gradlew :composeApp:run              # aperçu desktop, le plus rapide, branché sur le vrai Supabase
./gradlew :composeApp:assembleDebug    # APK debug
./gradlew :composeApp:desktopTest      # tests de la logique partagée
./gradlew :composeApp:checkApis        # vérifie les secrets et appelle chaque backend
```

L'app native remplace le paquet Capacitor, pas le site. Le site continue d'exister pour le
navigateur et pour l'installation en PWA.

## Capacitor, et pourquoi il est encore là

`android/`, `ios/`, `capacitor.config.ts`, les dépendances `@capacitor/*` et les scripts `cap:*` du
`package.json` sont l'ancien paquet : le site emballé dans une WebView. C'est ce qui est publié sur
le Play Store aujourd'hui, donc c'est aussi le seul moyen de livrer un correctif aux personnes qui
l'ont déjà installé.

Il part quand l'app native est publiée et validée en production, pas avant. La suppression est
l'étape 5 de `docs/native-rewrite-plan.md`.

Attention en attendant : il y a deux projets iOS, `ios/` à la racine qui est le wrapper
Capacitor, et `native/iosApp/` qui est l'hôte de l'app native. Ils n'ont rien à voir.

## Outils

`.tool-versions` fixe Node 24.14.0 et Temurin 17. Le JDK 17 est une contrainte d'AGP 8.13, pas un
choix. Gradle vient du wrapper, il n'y a rien à installer.

## Où regarder ensuite

- [`docs/NEXT-STEPS.md`](docs/NEXT-STEPS.md) — l'état d'avancement de la réécriture native et ce
  qu'il reste à faire
- [`docs/native-rewrite-plan.md`](docs/native-rewrite-plan.md) — pourquoi KMP, la correspondance
  écran par écran avec le site, la phase iOS
- [`native/README.md`](native/README.md) — tout le détail du build natif
- [`RELEASE.md`](RELEASE.md) — publication, keystores, identifiants
