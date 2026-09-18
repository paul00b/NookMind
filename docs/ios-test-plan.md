# iOS, phase A : installation et plan de test

Build sans compte Apple Developer payant. Ce que ça implique, une fois pour toutes :

- Simulateur : tout marche, sans signature.
- iPhone : l'app s'installe avec ton Apple ID gratuit (« Personal Team »), expire au bout de 7 jours,
  se réinstalle par Cmd+R.
- Absents de ce build, par construction : Sign in with Apple, connexion Google (bouton visible,
  échec au tap), notifications. Ce n'est pas un bug, c'est la phase B.

## 1. Installation, une fois

1. **Xcode 26.4 ou plus récent** (App Store). L'ouvrir une fois, accepter la licence, laisser
   installer les composants. Puis Xcode → Settings → Components : installer un runtime iOS 26.
   ```bash
   xcodebuild -version
   ```
2. **XcodeGen**
   ```bash
   brew install xcodegen
   ```
3. **Un JDK 17 ou plus récent.** Celui d'Android Studio suffit. Vérifier :
   ```bash
   /usr/libexec/java_home -V
   ```
   Si rien : `brew install --cask temurin@17`.
4. **Les fichiers hors git**, déjà là si Android build sur ce Mac :
   - `native/secrets.properties` (six clés)
   - `native/local.properties` avec `sdk.dir=…` (le plugin Android est appliqué même pour iOS)
5. **La branche**
   ```bash
   git fetch origin
   git checkout claude/optimistic-albattani-rj2h54
   git pull
   ```
6. **Le projet Xcode**
   ```bash
   cd native/iosApp
   xcodegen generate
   ```
7. **Ta team.** Xcode → Settings → Accounts → ton Apple ID → « Personal Team », copier l'identifiant
   à 10 caractères. Créer `native/iosApp/NookMind/Local.xcconfig` :
   ```
   DEVELOPMENT_TEAM = XXXXXXXXXX
   ```
   Ne rien régler dans l'onglet Signing de Xcode : perdu au prochain `xcodegen generate`.
8. **Premier build**
   ```bash
   open NookMind.xcodeproj
   ```
   Destination : un iPhone simulé (iPhone 17 Pro, par exemple). Cmd+R. Le premier build lance
   Gradle, qui télécharge Kotlin/Native (1 Go) : 10 à 20 minutes. Les suivants : 1 à 2 minutes.
9. **L'iPhone**, pour la partie 3 :
   - sur le téléphone : Réglages → Confidentialité et sécurité → Mode développeur → activer,
     redémarrer ;
   - brancher en USB, le choisir comme destination dans Xcode, Cmd+R ;
   - au premier lancement iOS refuse : Réglages → Général → VPN et gestion de l'appareil → faire
     confiance au développeur, relancer.

## 2. Simulateur, dans l'ordre

Chaque ligne : ce que tu fais, puis ce que tu dois voir.

**Démarrage**
- [ ] Lancement à froid : onboarding, trois slides, swipe, points qui suivent. « Skip » et « Get
      started » mènent à l'écran de connexion.
- [ ] Connexion e-mail + mot de passe avec ton compte : la bibliothèque arrive, avec tes données.
- [ ] Tuer l'app (Cmd+Shift+H deux fois, swipe up), relancer : session restaurée, pas d'écran de
      connexion.

**Thème et langue**
- [ ] Réglages → thème clair, sombre, système : chaque changement s'applique immédiatement.
- [ ] En « système », basculer l'apparence du simulateur (Features → Toggle Appearance) : l'app
      suit.
- [ ] Simulateur en français (Settings → General → Language & Region → French) : tout est traduit,
      dates au format `15 avr. 2026`.

**Livres**
- [ ] Recherche : résultats avec couvertures. Un titre rare aussi, pour vérifier que ce n'est pas
      un cache.
- [ ] Fiche → ajouter → la carte apparaît dans la bibliothèque.
- [ ] Note (étoiles, demi-étoiles), avancement, dates, notes texte : chaque modification survit à un
      kill de l'app et apparaît dans la web app.
- [ ] Supprimer.

**Films**
- [ ] Recherche TMDB → fiche : note IMDb et plateformes de streaming affichées (c'est l'API
      Vercel).
- [ ] Trailer : lecture **en ligne dans la fiche**, avec le son, sans passer plein écran tout
      seul. Fermer la fiche : le son s'arrête.
- [ ] Lien vers une plateforme : Safari s'ouvre sur la bonne page.
- [ ] Ajouter, noter, marquer vu, supprimer.

**Séries**
- [ ] Recherche → fiche → saisons et épisodes. Cocher des épisodes : compteurs et « prochain
      épisode » se mettent à jour.
- [ ] Onglet À suivre : la série y figure avec le bon épisode.
- [ ] Une série terminée, une en attente de saison : les deux états s'affichent.

**Collections**
- [ ] Créer, y ranger des éléments des trois types, retirer, supprimer la collection.

**Réglages**
- [ ] Mode d'affichage (grille / liste), ordre des sections : appliqués et conservés après kill.
- [ ] Notifications : la section indique que ce n'est pas disponible. Attendu.
- [ ] Vibrations : l'interrupteur se change et se conserve (aucun effet sur simulateur).
- [ ] Revoir l'onboarding, pages Confidentialité et Conditions : ouverture et retour.
- [ ] Déconnexion : retour à l'onboarding (comme le web), puis reconnexion : bibliothèque intacte.

**Navigation**
- [ ] Geste retour (swipe depuis le bord gauche) sur chaque fiche et chaque feuille : ferme le bon
      niveau, jamais deux.
- [ ] Clavier : aucun champ masqué pendant la saisie, le clavier se ferme au tap ailleurs.
- [ ] Simulateur iPad, paysage : barre latérale à partir de 768 pt, rien de coupé. Facultatif.

## 3. iPhone

Refaire connexion, une recherche par onglet, un trailer, un lien externe. Puis ce que seul
l'appareil peut dire :

- [ ] **Haptiques, les cinq signaux**, à comparer avec Android :
      confirmation (ajout, sauvegarde), refus (une erreur, un champ invalide), tic (puces,
      segments, étoiles), interrupteur on, interrupteur off. Noter ceux qui sont muets ou
      identiques entre eux.
- [ ] Fluidité du scroll dans une bibliothèque bien remplie.
- [ ] Zones sûres : rien sous la Dynamic Island, rien sous la barre d'accueil.
- [ ] Clavier réel, saisie dans les champs de connexion et de recherche.
- [ ] Passer en arrière-plan, revenir : même écran, même état. Tuer, relancer : session restaurée.
- [ ] Recevoir un appel ou une notification système pendant un trailer : la vidéo se met en pause,
      reprend.

## 4. Attendu, à ne pas remonter

- « Continue with Google » visible, tap → « Google sign-in is not available on this platform ».
- Pas de bouton Apple.
- Pas de remplissage automatique des mots de passe (demande une capacité payante).
- Sur iPhone, l'app cesse de s'ouvrir après 7 jours : Cmd+R depuis Xcode.

## 5. Remonter un problème

Pour chaque écart : l'écran, ce que tu as fait, ce que tu attendais, ce que tu as eu, et une
capture si c'est visuel. Je corrige d'ici, la CI vérifie, tu retires.
