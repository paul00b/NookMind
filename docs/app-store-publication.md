# Publier NookMind sur l'App Store

Date : 2026-10-08
App concernée : l'hôte iOS `native/iosApp/` et le code partagé Kotlin, bundle ID `fr.paulbr.nookmind`.

Même structure que `docs/play-store-publication.md`. Ce qui se fait dans les consoles (Apple,
Google Cloud, Firebase, Supabase) est pour toi ; le code et la CI sont déjà en place.

---

## 0. Ce qui est fait dans le code

| | État |
|---|---|
| Build simulateur (`ios-simulator-build.yml`) | Vert : compilation, tests partagés, lancement, captures |
| Build Release pour iPhone (`ios-release.yml`, contrôle) | Compile le framework Kotlin et les paquets Swift en arm64, sans signer |
| Sign in with Apple | Pont Swift écrit, activé quand `NOOKMIND_PAID_TEAM = YES` |
| Connexion Google | GoogleSignIn 9.2, même contrat qu'Android (client web, nonce). **Vérifiée sur iPhone** le 2026-10-08 |
| Notifications | Firebase Messaging 12 : permission, jeton, affichage au premier plan, ouverture au tap |
| Manifeste de confidentialité | `PrivacyInfo.xcprivacy`, NSUserDefaults déclaré (raison `CA92.1`) |
| Déclaration de chiffrement | `ITSAppUsesNonExemptEncryption = NO` dans `Info.plist` : pas de question à chaque envoi |
| Xcode 26 | Exigé par l'App Store depuis le 28 avril 2026 ([Apple](https://developer.apple.com/news/upcoming-requirements/)), la CI l'utilise |
| Suppression de compte dans l'app | Existe, Apple l'exige aussi |
| iPad | Non : iPhone seul pour la première version (`TARGETED_DEVICE_FAMILY: "1"`). Voir §12 |

**Vérifié sur iPhone le 2026-10-08**, avec un Apple ID gratuit et le build `fr.paulbr.nookmind.dev` :
l'app, la bande-annonce YouTube (correctif de l'erreur 153), les cinq retours haptiques, et la
connexion Google de bout en bout, nonce vérifié par Supabase. **Pas encore exécutés** : Sign in with
Apple et les notifications, qui demandent le compte payant, et la signature pour l'App Store.

---

## 1. Tester sur ton iPhone, avant de payer

`docs/ios-test-plan.md`. Avec ton Apple ID gratuit, l'app s'installe pour 7 jours. Tout se teste
sauf Sign in with Apple et les notifications. La connexion Google marche aussi, une fois le client
iOS créé (§5).

Ça vaut le coup avant de payer : un problème d'affichage ou de comportement sur un vrai iPhone se
corrige de la même façon, compte payant ou pas.

---

## 2. Le compte Apple Developer

<https://developer.apple.com/programs/enroll/>. 99 USD par an, affiché en euros à l'inscription.

- **Individuel** : le plus simple. Ton nom légal s'affiche comme vendeur sur l'App Store.
- **Organisation** : le nom de l'entité s'affiche, mais il faut une entité juridique et un numéro
  D-U-N-S.

L'Apple ID doit avoir la validation en deux étapes. Compter un à deux jours de vérification.

Une fois validé, récupérer le **Team ID** (10 caractères) : developer.apple.com → Account →
Membership details. Il sert à trois endroits :

- secret GitHub `APPLE_TEAM_ID` ;
- `native/iosApp/NookMind/Local.xcconfig` sur le Mac, avec le passage en compte payant :
  ```
  DEVELOPMENT_TEAM = TONTEAMID
  NOOKMIND_PAID_TEAM = YES
  ```
- la clé APNs dans Firebase (§7).

---

## 3. L'identifiant de l'app

Xcode sait le créer tout seul, mais le faire à la main garantit que les deux capacités sont
cochées.

developer.apple.com → Certificates, Identifiers & Profiles → **Identifiers** → `+` → App IDs →
App :

| Champ | Valeur |
|---|---|
| Description | NookMind |
| Bundle ID | Explicit, `fr.paulbr.nookmind` |
| Capabilities | **Sign in with Apple** et **Push Notifications** |

**Si Apple répond que `fr.paulbr.nookmind` n'est pas disponible** : il appartient déjà à une autre
équipe. Le projet Capacitor (`ios/App`) signe avec `fr.paulbr.nookmind` et l'équipe `SJ42PK8VK6`,
qui l'a très probablement réservé lors des essais de mai. Trois issues, dans l'ordre de préférence :

1. Si `SJ42PK8VK6` est un compte à toi (un autre Apple ID), publier depuis ce compte, ou supprimer
   l'identifiant dans son portail s'il est payant.
2. Si c'était une équipe gratuite, attendre : ses enregistrements expirent au bout de quelques jours
   sans utilisation, d'après un ingénieur Apple ([forum Apple](https://developer.apple.com/forums/thread/80294)),
   sans délai garanti.
3. Sinon, publier l'app iOS sous un autre identifiant, par exemple `fr.paulbr.nookmind.app`. Rien
   n'oblige l'identifiant iOS à être celui d'Android ; il faut alors le changer dans
   `native/iosApp/project.yml`, dans l'app iOS Firebase et dans le client OAuth iOS.

Les builds de test sur ton iPhone ne sont pas concernés : ils signent sous `fr.paulbr.nookmind.dev`
(`NOOKMIND_BUNDLE_ID_SUFFIX` dans `Local.xcconfig`, voir `docs/ios-test-plan.md`).

---

## 4. Créer l'app dans App Store Connect

<https://appstoreconnect.apple.com> → Apps → `+` → New App :

| Champ | Valeur |
|---|---|
| Plateforme | iOS |
| Nom | NookMind (30 caractères max, unique sur tout l'App Store : si c'est pris, « NookMind - Livres, films » par exemple) |
| Langue principale | Français (France) |
| Bundle ID | `fr.paulbr.nookmind`, dans la liste après l'étape 3 |
| SKU | `nookmind-ios` (interne, jamais affiché) |

L'envoi automatique échoue tant que cette fiche n'existe pas.

---

## 5. Connexion Google sur iOS

Fonctionne aussi avec un compte gratuit, donc faisable dès l'étape 1.

1. **Google Cloud Console**, dans **le projet du client web** (le nombre au début de
   `GOOGLE_AUTH_WEB_CLIENT_ID` est le numéro du projet), comme pour Android → APIs & Services →
   Identifiants → Créer des identifiants → ID client OAuth → type **iOS** → bundle ID
   `fr.paulbr.nookmind` → Créer. Copier l'ID client.

   Le `.env` du site contient peut-être déjà un client iOS (`VITE_GOOGLE_AUTH_IOS_CLIENT_ID`, de
   l'époque Capacitor). Avant de le réutiliser, vérifier dans Google Cloud qu'il est bien dans le
   projet du client web et que son bundle ID est `fr.paulbr.nookmind` : l'ancien projet Xcode
   Capacitor utilisait `fr.paulbr.bookmind`.

2. **Supabase** → Authentication → Providers → Google → **Client IDs** : ajouter l'ID client iOS
   après celui du client web, séparés par une virgule. Supabase accepte un jeton émis pour l'un ou
   l'autre ([Supabase](https://supabase.com/docs/guides/auth/social-login/auth-google)).

   Laisser **Skip nonce check** désactivé. La doc Supabase conseille de l'activer pour iOS, parce
   que GoogleSignIn ne savait pas transmettre de nonce avant la version 9. L'app utilise la 9.2 et
   envoie un nonce exactement comme sur Android : vérifié sur iPhone le 2026-10-08, la connexion
   aboutit avec la vérification active.

   Le build de test signe sous `fr.paulbr.nookmind.dev` (voir `docs/ios-test-plan.md`) et a son
   propre client iOS, « iOS Dev », déclaré lui aussi dans Supabase. La version App Store aura besoin
   d'un second client pour `fr.paulbr.nookmind`, à ajouter de la même façon.

3. **Secret GitHub** `GOOGLE_AUTH_IOS_CLIENT_ID` : l'ID client complet. Le workflow en déduit
   la forme inversée.

4. **Sur le Mac**, dans `Local.xcconfig` :
   ```
   GOOGLE_AUTH_IOS_CLIENT_ID = 1234567890-abcdef.apps.googleusercontent.com
   GOOGLE_AUTH_IOS_REVERSED_CLIENT_ID = com.googleusercontent.apps.1234567890-abcdef
   ```
   Si les deux ne correspondent pas, le bouton Google est masqué plutôt que de faire planter l'app.

---

## 6. Sign in with Apple

Obligatoire en pratique : la règle 4.8 de l'App Store impose, dès qu'une app propose une connexion
Google, une autre option qui protège la vie privée, et Apple a confirmé que Sign in with Apple la
satisfait ([forum Apple](https://developer.apple.com/forums/thread/760974)).

1. La capacité sur l'App ID (§3).
2. **Supabase** → Authentication → Providers → **Apple** → activer → **Client IDs** :
   `fr.paulbr.nookmind`. Pas de clé secrète ni de Services ID : ils ne servent qu'au flux web
   (OAuth), l'app utilise le flux natif ([Supabase](https://supabase.com/docs/guides/auth/social-login/auth-apple)).
3. Rien d'autre : `NOOKMIND_PAID_TEAM = YES` active le bouton. Le workflow de release le met
   toujours.

---

## 7. Notifications

1. **Clé APNs** : developer.apple.com → Certificates, Identifiers & Profiles → **Keys** → `+` →
   nom « NookMind APNs » → cocher **Apple Push Notifications service (APNs)** → environnement
   **Sandbox & Production** → Register → **télécharger le `.p8`**. Apple ne le laisse télécharger
   qu'une fois : le ranger avec le keystore Android. Noter le **Key ID**.
2. **Firebase** (projet `nookmind-8f5be`) → Paramètres du projet → Général → Ajouter une
   application → **iOS** → bundle ID `fr.paulbr.nookmind` → télécharger
   **`GoogleService-Info.plist`**. Ignorer les étapes d'installation du SDK : c'est déjà fait.
3. **Firebase** → Paramètres du projet → **Cloud Messaging** → Configuration de l'application
   Apple → Clé d'authentification APNs → Importer : le `.p8`, le Key ID, le Team ID.
4. **Secret GitHub** `GOOGLE_SERVICE_INFO_PLIST` : tout le contenu du fichier.
5. **Sur le Mac** : poser le fichier dans `native/iosApp/NookMind/` (il est ignoré par git), puis
   relancer `xcodegen generate` : XcodeGen n'embarque que les fichiers présents au moment où il
   génère le projet.

Rien à changer côté serveur : les routes `api/push/*` envoient déjà le format APNs via FCM.

Sans ce fichier, l'app part quand même : la section Notifications est simplement masquée. Ça permet
d'ouvrir TestFlight avant d'avoir fait cette partie.

---

## 8. La clé d'API App Store Connect

C'est elle qui permet à GitHub de signer et d'envoyer, sans Mac ni certificat à gérer.

App Store Connect → Utilisateurs et accès → **Intégrations** → App Store Connect API → Clés
d'équipe → Générer :

- nom : « GitHub release » ;
- accès : **Admin**. Le workflow signe avec un certificat de distribution géré par Apple (signature
  cloud), et Apple réserve ce certificat au rôle Admin : avec un rôle inférieur, la signature échoue
  avec une erreur de permission ([forum Apple](https://developer.apple.com/forums/thread/698117)).

Télécharger le `.p8` (une seule fois, lui aussi), noter le **Key ID** et l'**Issuer ID** affiché en
haut de la page.

### Les secrets GitHub, au complet

| Secret | Valeur | Obligatoire |
|---|---|---|
| `APP_STORE_CONNECT_API_KEY` | Le contenu du `.p8`, lignes BEGIN et END comprises | Oui |
| `APP_STORE_CONNECT_API_KEY_ID` | Key ID de la clé d'API | Oui |
| `APP_STORE_CONNECT_API_ISSUER_ID` | Issuer ID | Oui |
| `APPLE_TEAM_ID` | Team ID | Oui |
| `GOOGLE_AUTH_IOS_CLIENT_ID` | ID client OAuth iOS | Non : sans lui, pas de bouton Google |
| `GOOGLE_SERVICE_INFO_PLIST` | Contenu de `GoogleService-Info.plist` | Non : sans lui, pas de notifications |

Les six clés du backend (`SUPABASE_URL`, etc.) existent déjà, les workflows Android s'en servent.

Une clé Admin est puissante : si elle fuit, la révoquer dans la même page et en générer une autre.

---

## 9. Sortir une version iOS

GitHub → Actions → **iOS release** → Run workflow, version vide.

Le workflow archive l'app en Release pour iPhone, la signe, l'envoie à App Store Connect et pose le
tag `ios-vX.Y.Z`. Compter 25 à 40 minutes : la compilation Kotlin/Native en Release est la partie
lente. Apple traite ensuite le build 5 à 30 minutes avant qu'il apparaisse dans TestFlight.

Numérotation : la même règle qu'Android (`X.Y.Z`, build `X×10000 + Y×100 + Z`), avec sa propre
série de tags `ios-v`, puisque les deux apps sortent indépendamment. La première sera 2.0.0
(build 20000).

Le résumé du run indique ce que contient le build : Apple activé ou non, Google configuré ou non,
Firebase présent ou non, et les entitlements réellement signés.

---

## 10. TestFlight

**Testeurs internes** : jusqu'à 100, ce sont des utilisateurs de ton App Store Connect (Utilisateurs
et accès → inviter, le rôle « Assistance client » suffit). Pas de relecture d'Apple : ils ont le
build dès qu'il est traité. Ils installent l'app **TestFlight** et acceptent l'invitation.

**Testeurs externes** : jusqu'à 10 000, par e-mail ou par **lien public**, sans compte App Store
Connect. La première version de chaque numéro de version passe par la Beta App Review, en général
en une journée environ ([Expo](https://docs.expo.dev/submit/testflight/)). Il faut remplir les
informations de test : description, e-mail de retour, URL de confidentialité, et un **compte de
démo e-mail + mot de passe** pour le relecteur (le même que pour Play).

Pas de règle des 12 testeurs pendant 14 jours : tu peux soumettre à l'App Store quand tu veux.

Un build TestFlight expire au bout de 90 jours.

---

## 11. Soumettre à l'App Store

App Store Connect → l'app → la version 2.0.0 :

| Élément | Contrainte |
|---|---|
| Captures iPhone | 6,9 pouces : **1320 × 2868** px en portrait, 10 max ; App Store Connect en dérive les autres tailles ([guide](https://studio.adalo.com/blog/app-store-screenshot-sizes-2026)) |
| Captures iPad | Aucune : l'app est iPhone seul (§12) |
| Texte promotionnel | 170 caractères, modifiable sans nouvelle version |
| Description | 4 000 caractères |
| Mots-clés | 100 caractères au total, séparés par des virgules |
| URL d'assistance | Obligatoire, par exemple `https://nookmind.paulbr.fr` |
| URL de confidentialité | `https://nookmind.paulbr.fr/privacy` |
| Catégorie | Divertissement, comme sur Play |
| Classification par âge | Questionnaire, mêmes réponses que l'IARC de Play |
| Confidentialité de l'app | Voir ci-dessous |
| Informations pour la relecture | Le compte de démo, et une note : « Se connecter par e-mail » |

**Confidentialité de l'app** (les « étiquettes nutritionnelles ») : les mêmes données que la
Sécurité des données de Play. Adresse e-mail, nom (facultatif), identifiant utilisateur, contenu
généré par l'utilisateur (bibliothèque, notes) : collectés, liés à l'utilisateur, pour le
fonctionnement de l'app, **sans suivi** publicitaire.

La politique de confidentialité cite Supabase, Vercel, Firebase, TMDB, Google Books et IMDb. Avec
Sign in with Apple, Apple devient aussi un fournisseur d'identité : à ajouter sur la page avant la
soumission.

Relecture : en général 24 à 48 heures.

---

## 12. L'iPad : iPhone seul pour la première version

Décidé le 2026-10-08 : `TARGETED_DEVICE_FAMILY: "1"` dans `native/iosApp/project.yml`. Pas de
captures iPad à fournir, et la mise en page en barre latérale, jamais vérifiée sur un vrai iPad, n'est
pas exposée à la relecture.

L'iPad pourra être ajouté dans une mise à jour (`"1,2"`). L'inverse est impossible : une fois une
version publiée avec le support iPad, App Store Connect refuse un envoi qui tourne sur moins
d'appareils que la version en vente ([Apple, QA1623](https://developer.apple.com/library/archive/qa/qa1623/_index.html)).

Une app iPhone reste installable sur iPad, agrandie en mode compatibilité, et le relecteur peut
l'y essayer ([forum Apple](https://developer.apple.com/forums/thread/781735)) : il faut seulement
qu'elle y fonctionne, pas qu'elle y soit belle.

---

## Check-list express

- [x] App testée sur ton iPhone avec le compte gratuit (`docs/ios-test-plan.md`), Google compris
- [x] Décision iPad : iPhone seul (§12)
- [ ] Compte Apple Developer validé, Team ID noté
- [ ] App ID `fr.paulbr.nookmind` avec Sign in with Apple et Push Notifications
- [ ] App créée dans App Store Connect
- [ ] Client OAuth iOS dans le projet du client web, ajouté aux Client IDs Google de Supabase
- [ ] Provider Apple activé dans Supabase avec `fr.paulbr.nookmind`
- [ ] Clé APNs créée, importée dans Firebase ; app iOS ajoutée à Firebase
- [ ] Clé d'API App Store Connect (Admin) ; les six secrets GitHub iOS
- [ ] `.p8` APNs et `.p8` d'API sauvegardés hors du Mac
- [ ] Première release TestFlight, installée et testée : e-mail, Google, Apple, notification
- [ ] Politique de confidentialité : Apple ajouté
- [ ] Fiche, captures, confidentialité, classification, compte de démo
- [ ] Soumission à l'App Store
