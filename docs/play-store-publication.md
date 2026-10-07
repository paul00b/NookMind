# Publier NookMind sur le Play Store

Date : 2026-09-28
Build concerné : l'app native `native/` (`2.0.0`, versionCode 20000, `fr.paulbr.nookmind`).

Ce guide suppose une **première publication** : la fiche n'existe pas encore dans Play Console.
Si elle existe déjà, avec le paquet Capacitor en versionCode 1, sauter les étapes 1 et 3 et aller
directement à l'étape 2, puis 6.

---

## 0. État du projet vérifié le 2026-09-28

| | État |
|---|---|
| Keystore `~/nookmind-release.jks`, alias `nookmind` | ✅ présent sur le Mac |
| `android/keystore.properties` (ancien paquet) | ✅ présent, pointe vers le bon `.jks` |
| `native/keystore.properties` | ✅ copié depuis `android/` le 2026-09-28 |
| `native/composeApp/google-services.json` | ✅ en place le 2026-09-28 (projet `nookmind-8f5be`, déclare `fr.paulbr.nookmind` et `.debug`) |
| `native/secrets.properties` | ✅ présent |
| `native/local.properties` (chemin du SDK Android) | ✅ créé le 2026-09-28 |
| `targetSdk` 36 | ✅ conforme aux exigences Play actuelles |
| Suppression de compte dans l'app | ✅ existe |
| Politique de confidentialité `https://nookmind.paulbr.fr/privacy` | ✅ complétée le 2026-09-28 (FR/EN, contact, suppression) : à déployer |
| Build de release | ✅ compilé le 2026-09-28 (APK + AAB), signé avec la clé `nookmind` (SHA-1 `6E:F9:BE:07:50:1A:CC:74:82:54:69:4B:48:90:36:C7:84:1D:CA:A5`). Recompilé avec `google-services.json`. ⚠️ Pas encore testé sur appareil |
| Permissions du build | `INTERNET`, `POST_NOTIFICATIONS`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, réception FCM, `USE_BIOMETRIC`/`USE_FINGERPRINT` (ajoutées par Credential Manager). Pas d'`AD_ID` : répondre « Non » à l'identifiant publicitaire |

---

## 1. Créer le compte développeur

1. Aller sur <https://play.google.com/console/signup>.
2. Choisir le type de compte :
   - **Personnel** : le plus simple pour un projet solo. Contrainte importante : un compte
     personnel créé après novembre 2023 doit faire un **test fermé avec au moins 12 testeurs
     inscrits pendant 14 jours d'affilée** avant de pouvoir demander l'accès à la production.
     Il faut donc prévoir environ 3 semaines entre le premier envoi et la mise en ligne publique.
   - **Organisation** : pas de test obligatoire, mais il faut un numéro D-U-N-S (gratuit, quelques
     jours à quelques semaines d'obtention) et une entité juridique.
3. Payer les frais d'inscription (25 $, une seule fois).
4. Vérifier son identité (pièce d'identité) et ses coordonnées. Compter de quelques heures à
   quelques jours.
5. Vérifier l'accès à un appareil Android réel : Play Console le demande via son application
   mobile.

> **Recommandé :** choisir « Personnel » et lancer tout de suite le recrutement des 12 testeurs
> (amis, famille, collègues). Il leur faut un compte Google et un téléphone Android. C'est le
> chemin critique, tout le reste se fait en parallèle.

---

## 2. Préparer et tester le build de release

### 2.1 Les deux fichiers manquants

```bash
cd "/Users/Paul/Desktop/Projets Dev/NookMind"

# Même contenu que l'ancien paquet : même keystore, même alias
cp android/keystore.properties native/keystore.properties
```

Pour `google-services.json` : Firebase Console → projet `nookmind-8f5be` → ⚙️ Paramètres du
projet → Vos applications → l'app Android → télécharger `google-services.json`, puis le placer
dans `native/composeApp/`. Il doit déclarer `fr.paulbr.nookmind` **et**
`fr.paulbr.nookmind.debug` (voir `native/README.md` §2).

### 2.2 Sauvegarder le keystore

Si `~/nookmind-release.jks` est perdu, la fiche ne peut plus être mise à jour. Avant d'aller plus
loin, copier le `.jks` **et** ses mots de passe dans au moins deux endroits hors du Mac :
gestionnaire de mots de passe (1Password, Bitwarden) et un stockage chiffré.

Avec Play App Signing (obligatoire pour les nouvelles apps), ce fichier devient la **clé d'upload** :
Google garde la vraie clé de signature. Une clé d'upload perdue se réinitialise via le support
Google, mais c'est lent. Mieux vaut ne pas la perdre.

### 2.3 Compiler

Le plus simple : GitHub → Actions → **Android release** → Run workflow. Il produit l'AAB signé et un
APK de release dans les artefacts du run, sans rien compiler sur le Mac (`RELEASE.md`). En local :

```bash
cd native
./gradlew :composeApp:assembleRelease   # APK, pour tester sur ton téléphone
./gradlew :composeApp:bundleRelease     # AAB, le fichier à envoyer au Play Store
```

Fichiers produits :

- `native/composeApp/build/outputs/apk/release/composeApp-release.apk`
- `native/composeApp/build/outputs/bundle/release/composeApp-release.aab`

Vérifier que l'AAB est bien signé avec la clé `nookmind` :

```bash
keytool -printcert -jarfile composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Si la commande répond « Not a signed jar file », c'est que `native/keystore.properties` n'a pas
été lu.

### 2.3 bis Avertissement R8, corrigé

Le build affichait des dizaines de fois : `R8: An error occurred when parsing kotlin metadata`. La
version de R8 livrée avec AGP 8.13 ne connaît pas les métadonnées de Kotlin 2.4, qui demande R8
9.1.29 ou plus récent. `native/settings.gradle.kts` épingle désormais cette version. Le résumé de
chaque run du workflow **Android release** affiche le nombre de ces erreurs : il doit rester à 0.
S'il remonte (après une montée de Kotlin par exemple), c'est le premier suspect d'un écran vide ou
d'un plantage en release seulement.

### 2.4 Tester la release sur ton téléphone

```bash
adb install -r composeApp/build/outputs/apk/release/composeApp-release.apk
```

R8 (la minification activée en release) peut casser trois choses qui marchent en debug. Les tester
**sur ce build-là** :

- [ ] connexion Google
- [ ] connexion e-mail
- [ ] notification : activer, envoyer le test depuis les réglages, la recevoir, la toucher
- [ ] ouverture d'un lien vers une plateforme de streaming
- [ ] trailer YouTube
- [ ] suppression de compte (avec un compte jetable)

Si l'ancienne app Capacitor est installée sur le téléphone, désinstalle-la d'abord : elle n'est
pas signée pareil que ce build tant qu'aucun des deux ne vient du Play Store, et Android refusera
l'installation par-dessus.

---

## 3. Créer l'app dans Play Console

Play Console → **Créer une application** :

| Champ | Valeur |
|---|---|
| Nom de l'application | NookMind (30 caractères max) |
| Langue par défaut | Français (France) – fr-FR |
| Application ou jeu | Application |
| Gratuite ou payante | Gratuite (**définitif** : une app gratuite ne peut jamais devenir payante) |
| Déclarations | Cocher les règles du programme et les lois d'exportation américaines |

---

## 4. Remplir « Configurer votre application »

Le tableau de bord liste les tâches à cocher. Elles sont toutes dans **Surveiller et améliorer →
Règles et programmes → Contenu de l'application**. Voici quoi répondre pour NookMind.

### 4.1 Politique de confidentialité

URL : `https://nookmind.paulbr.fr/privacy`

La page (`src/pages/Privacy.tsx`) a été complétée le 2026-09-28 : adresse de contact, jeton de
notification, services tiers (Supabase, Vercel, Firebase, TMDB, Google Books, IMDb), durée de
conservation, droits RGPD et procédure de suppression. Elle s'affiche en français ou en anglais
selon la langue du navigateur, avec un bouton pour basculer. Vérifier qu'elle est en ligne avant
de déclarer l'URL.

### 4.2 Accès à l'application

NookMind exige une connexion, donc : « Tout ou partie des fonctionnalités sont soumises à des
restrictions ». Fournir aux relecteurs de Google :

- un **compte e-mail + mot de passe de démo** (pas Google : le relecteur ne peut pas se connecter
  à ton compte Google), avec quelques livres, films et séries déjà dans la bibliothèque ;
- les instructions : « Ouvrir l'app, passer l'onboarding, choisir Se connecter par e-mail ».

C'est le motif de refus le plus fréquent. Tester soi-même la connexion avec ce compte sur le build
de release.

### 4.3 Annonces

Non, l'app ne contient pas d'annonces.

### 4.4 Classification du contenu

Questionnaire IARC. Catégorie : « Toutes les autres applications ». Répondre non à la violence, la
sexualité, les jeux d'argent, etc. Pour les interactions utilisateurs : les données restent
privées à chaque compte, il n'y a ni partage ni chat, donc non. Résultat attendu : PEGI 3 /
Tout public.

Le contenu pour adultes de TMDB est exclu : toutes les recherches passent
`include_adult=false` (`TmdbApi.kt`).

### 4.5 Public cible et contenu

Choisir **13 ans et plus** (ou 18+). Ne pas cocher les tranches de moins de 13 ans : sinon l'app
tombe sous le programme Familles, avec des exigences bien plus lourdes.

### 4.6 Application d'actualités, applis gouvernementales, fonctionnalités financières, santé

Non à tout.

### 4.7 Identifiant publicitaire

Non. Firebase Messaging seul n'ajoute pas la permission `AD_ID` (c'est Firebase Analytics qui le
fait). Pour s'en assurer après un build :

```bash
find native/composeApp/build -path "*merged_manifest*" -name AndroidManifest.xml \
  -exec grep -l "AD_ID" {} \;
```

Aucune sortie = pas d'identifiant publicitaire, réponse « Non » correcte.

### 4.8 Sécurité des données

La section la plus longue. Réponses pour NookMind :

**Vue d'ensemble**

| Question | Réponse |
|---|---|
| L'app collecte ou partage des données ? | Oui |
| Données chiffrées en transit ? | Oui (HTTPS partout) |
| Les utilisateurs peuvent demander la suppression ? | Oui |

**Types de données collectées**

| Type | Collecté | Partagé | Obligatoire | Finalité |
|---|---|---|---|---|
| Informations personnelles → Adresse e-mail | Oui | Non | Oui | Gestion du compte, fonctionnalité de l'app |
| Informations personnelles → Nom | Oui (`full_name` : nom Google/Apple ou nom d'affichage choisi dans les paramètres) | Non | Non | Gestion du compte |
| Informations personnelles → ID utilisateur | Oui (UUID Supabase) | Non | Oui | Gestion du compte, fonctionnalité de l'app |
| Activité dans l'app → Autre contenu généré par l'utilisateur | Oui (bibliothèque, notes, avancement, collections) | Non | Oui | Fonctionnalité de l'app |
| Identifiants de l'appareil ou autres → jeton FCM | Oui | Non | Non (seulement si notifications activées) | Fonctionnalité de l'app |

« Partagé » veut dire transmis à un tiers pour son propre usage. Supabase, Firebase et Vercel sont
des sous-traitants qui agissent pour ton compte, donc ce n'est **pas** du partage au sens de
Google.

### 4.9 Suppression du compte

Google demande **une URL publique** où un utilisateur peut demander la suppression de son compte
**sans réinstaller l'app**. La suppression dans l'app ne suffit pas.

URL à déclarer : `https://nookmind.paulbr.fr/delete-account`. Elle ouvre la politique de
confidentialité directement sur la section de suppression, qui explique la suppression dans l'app
(Paramètres → Supprimer le compte), propose un bouton « Demander la suppression de mon compte »
(e-mail prérempli à broussolle.paul@gmail.com), et liste ce qui est supprimé.

Questions du formulaire :

- L'app permet de créer un compte : Oui
- Suppression partielle des données sans supprimer le compte : Non (on peut retirer un titre, mais
  pas de fonction dédiée « effacer mes données »)
- Données conservées après suppression : aucune (`api/account/delete.ts` efface les six tables puis
  l'utilisateur Supabase)

---

## 5. La fiche du Play Store

**Développer la visibilité → Présence sur le Play Store → Fiche principale du Play Store**

| Élément | Contrainte |
|---|---|
| Nom de l'application | 30 caractères max |
| Description courte | 80 caractères max |
| Description complète | 4 000 caractères max, pas de mots-clés en vrac ni de classements (« n°1 ») |
| Icône | PNG 32 bits, **512 × 512**, 1 Mo max. `resources/icon.png` fait 1024 × 1024 : la réduire avec `sips -z 512 512 resources/icon.png --out icon-512.png` |
| Image de présentation | JPG ou PNG 24 bits sans transparence, **1024 × 500**, obligatoire |
| Captures téléphone | 2 à 8, PNG ou JPG, 320 à 3 840 px de côté, ratio max 2:1. **4 captures d'au moins 1 080 px** pour être éligible aux mises en avant |
| Captures tablette 7" et 10" | Facultatives |
| Vidéo | Facultative, lien YouTube |

Puis **Traductions → Ajouter des traductions → Anglais (États-Unis) en-US** pour publier la fiche
anglaise : même structure, captures en anglais si tu en as.

**Paramètres de la fiche Play Store** (même menu) :

- Catégorie : **Divertissement** (ou Livres et références, si les livres sont l'usage principal)
- Tags : jusqu'à 5, choisis dans la liste de Google
- Adresse e-mail de contact : **obligatoire et publique**
- Site web : `https://nookmind.paulbr.fr`

---

## 6. Tester via le Play Store

### 6.1 Test interne, en premier

**Tester et publier → Tests → Tests internes → Créer une release**

1. Play App Signing : accepter la gestion par Google (option par défaut).
2. Envoyer `NookMind-2.0.0.aab`, tiré des artefacts du workflow **Android release**. C'est le seul
   envoi à faire à la main : ensuite le workflow envoie lui-même, une fois le compte de service
   configuré (`RELEASE.md`, mise en place §3).
3. Nom de la release : `2.0.0 (20000)`. Notes de version : le contenu de
   `native/distribution/whatsnew/`, en fr-FR et en-US.
4. Onglet **Testeurs** : créer une liste avec ton adresse, copier le **lien d'inscription**, l'ouvrir
   sur ton téléphone, accepter, installer depuis le Play Store.

Le test interne est disponible en quelques minutes, sans relecture.

### 6.2 ⚠️ La connexion Google va casser, et c'est normal

Avec Play App Signing, **Google re-signe l'app avec sa propre clé**. L'app installée depuis le
Play Store n'a donc pas la même empreinte SHA-1 que celle que tu as compilée, et Google Sign-In la
refuse (`DEVELOPER_ERROR` ou `{16} Account reauth failed`).

Correction :

1. Play Console → **Tester et publier → Configuration → Intégrité de l'application → Signature
   d'application** : copier le SHA-1 **et** le SHA-256 du « certificat de la clé de signature
   d'application ».
2. **Google Cloud Console** (le même projet que le client web `GOOGLE_AUTH_WEB_CLIENT_ID`, voir
   `docs/NEXT-STEPS.md`) → APIs & Services → Credentials → créer un **nouveau client OAuth
   Android** pour `fr.paulbr.nookmind` avec ce SHA-1. Garder celui de la clé d'upload, pour tes
   builds locaux.
3. **Firebase Console** → Paramètres du projet → l'app Android → Ajouter une empreinte → coller le
   SHA-1 et le SHA-256.
4. Pas besoin de renvoyer un build : le changement est côté serveur. Attendre quelques minutes,
   réessayer la connexion Google sur l'app du Play Store.

### 6.3 Rapport de pré-lancement

Après chaque envoi, Google lance l'app sur de vrais appareils et produit un rapport :
**Tester et publier → Tests → Rapport de pré-lancement**. Regarder les plantages et les alertes
d'accessibilité (les interrupteurs `NookToggle` invisibles pour TalkBack, notés dans
`docs/NEXT-STEPS.md`, y ressortiront sûrement).

### 6.4 Test fermé : 12 testeurs, 14 jours (compte personnel)

**Tester et publier → Tests → Tests fermés → Créer un canal** (ou utiliser « Alpha »).

1. Créer une release et y **promouvoir** la release du test interne (même AAB, pas de nouvel
   envoi).
2. Ajouter les testeurs par liste d'e-mails ou par Google Group.
3. Envoyer le lien d'inscription. Chaque testeur doit **accepter via le lien puis installer**
   l'app. Un e-mail ajouté à la liste sans inscription ne compte pas.
4. Soumettre la release pour relecture (celle-ci est relue par Google, quelques jours).
5. Garder **au moins 12 testeurs inscrits pendant 14 jours consécutifs**. Si quelqu'un se
   désinscrit en route, le compteur peut repartir : viser 15 à 20 testeurs pour avoir de la marge.
6. Pendant ces 14 jours, Google regarde l'engagement : demande à tes testeurs d'ouvrir l'app
   plusieurs fois et de faire des retours. Publier une mise à jour pendant la période est un bon
   signal.

Au bout des 14 jours : **Tableau de bord → Demander l'accès à la production**. Questionnaire
sur le déroulé du test (comment tu as recruté, quels retours, ce que tu as corrigé). Réponse sous
7 jours environ.

---

## 7. Publier en production

**Tester et publier → Production**

1. **Pays et régions** : ajouter les pays visés (France + francophonie, ou tous les pays puisque
   l'app est aussi en anglais).
2. **Créer une release** → promouvoir la release testée, ou en envoyer une nouvelle par le workflow
   (le versionCode suit la version, il est forcément supérieur).
3. Notes de version fr-FR et en-US.
4. **Déploiement progressif** : commencer à 20 %, surveiller les plantages dans Android vitals
   pendant 1 ou 2 jours, puis monter à 100 %.
5. **Envoyer pour examen**. Première relecture : de quelques jours à une semaine.

Option utile : **Vue d'ensemble de la publication → Publication gérée**. Une fois l'examen validé,
c'est toi qui cliques pour mettre en ligne, au lieu d'une mise en ligne automatique.

---

## 8. Après la mise en ligne

- **Chaque mise à jour** : notes de version dans `native/distribution/whatsnew/`, puis
  Actions → **Android release** → Run workflow. Détail dans `RELEASE.md`.
- **Android vitals** (Surveiller et améliorer) : taux de plantage et d'ANR. Google pénalise la
  visibilité au-delà de 1,09 % de plantages et 0,47 % d'ANR.
- **Avis** : y répondre depuis Play Console.
- **Exigence de targetSdk** : Google remonte le niveau minimum chaque année, en août. Surveiller
  les e-mails Play Console.
- **Nettoyage Capacitor** : une fois l'app native validée en production, supprimer `android/`,
  `ios/`, `capacitor.config.ts`, etc. (liste dans `docs/NEXT-STEPS.md` §5).

---

## Check-list express

- [ ] Compte Play Console créé, identité vérifiée
- [ ] 12 à 20 testeurs recrutés (compte personnel)
- [ ] Keystore sauvegardé hors du Mac
- [ ] `native/keystore.properties` et `native/composeApp/google-services.json` en place
- [ ] APK de release testé sur téléphone (Google, e-mail, notifications, liens, suppression)
- [x] Politique de confidentialité complétée (contact, FCM, TMDB, Google Books, suppression)
- [ ] Politique de confidentialité déployée sur `nookmind.paulbr.fr`
- [ ] Compte de démo e-mail créé et rempli
- [ ] Contenu de l'application : les 9 déclarations remplies
- [ ] Fiche fr-FR + en-US, icône 512, image 1024 × 500, 4 captures ou plus
- [ ] AAB en test interne, installé depuis le Play Store
- [ ] SHA-1 de la clé de signature Play ajouté à Google Cloud et Firebase, connexion Google OK
- [ ] Test fermé 14 jours, puis accès production demandé
- [ ] Production : pays, déploiement progressif, envoi pour examen
