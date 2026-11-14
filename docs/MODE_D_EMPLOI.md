# ZuluFx — Mode d'emploi (v0.41)

## Démarrage
1. Ouvrir l'app (écran en paysage).
2. Le moteur audio démarre automatiquement. Si le message « AUDIO : demarrage impossible » apparaît, fermer les autres apps audio et relancer.
3. « TEMPLATE DE BASE CHARGE » s'affiche : l'app est dans un état neutre (aucun son qui traîne).

## Organisation de l'écran
- **Haut gauche** : pad XY tactile (cercles A et B = axes F et Z).
- **Barre haute** : boutons FX, OUTILS, PLAY, REC, MUTE, SHIFT, UNDO, VU-mètre, écran BPM.
- **Gauche** : faders ZU/LU, FLY, MIX, entrée ligne (LINE-IN).
- **Centre** : grille de 12 pads (banques), éditeur de pas, PUNCH, sélecteur FX.
- **Panneaux** (ouverture par bouton) : FX SELECT, GAMME, PROJET, ARPEGGIATEUR/LFO, SAMPLE, PANIQUE.

## Pad XY (cercles A/B)
- Toucher le pad = jouer le synthé / déclencher des hits selon le moteur actif.
- Le cercle **A** = axe **F** (jaune), le cercle **B** = axe **Z** (rose) ; chaque axe pilote les paramètres qui lui sont affectés.
- Boutons **F** / **Z** : ON/OFF de chaque axe.
  - Appui long sur F : bascule MOTION (enregistrement du mouvement de l'axe) et REC WET.
  - Appui long sur Z : idem pour l'axe Z.

## Boutons principaux
- **FX** : ouvre la page FX. Dans la page : TAP = ON/OFF · SHIFT+TAP = RÉGLAGES · SYNTH = jouer sur le pad. Appui long sur FX : route/retire l'effet du MASTER.
- **OUTILS** : ouvre l'ARPEGGIATEUR et le navigateur LFO (destinations LFO/ARP visibles et routables).
- **PLAY / STOP** : démarre/arrête la lecture.
- **REC** : protocole d'enregistrement (voir ci-dessous).
- **MUTE** : coupe la sortie (MUTE ● = actif).
- **UNDO** : annule le dernier enregistrement (UNDO REC).
- **SHIFT** : maintenu = modifie la fonction des autres commandes ; verrouillable (le bouton clignote).
- **PANIQUE** (dans le menu) : remise à zéro complète — « PANIQUE : TOUT EST CALME ».

## Enregistrement (REC)
1. Taper **REC** → « REC : ARMÉ ».
2. Taper un **pad (1 à 14)** = destination de la banque → « PAD n ARMÉ — REC = LANCER ».
3. Taper **REC** = démarrer la capture ; taper le pad armé ou REC = stop (« REC STOP »).
4. REC WET (capturer avec les effets) : appui long sur F (axe F) ou Z (axe Z).
5. MOTION : enregistrer le mouvement d'un axe (appui long F/Z), puis relancer sur un pad.

## Scènes ZU / LU
- **TAP** sur ZU ou LU = changer de côté (gauche/droite) seulement.
- **Maintenir ZU ou LU (>0,5 s)** en réglant puis relâcher = capturer le snapshot sur la banque active du côté (« SNAPSHOT ZU : CHOISIR PAD »).
- Fader **ZU/LU** : fondu/morphing entre les deux scènes.
- Rangement par défaut (mode 12 pads) : 6 scènes de gauche = ZU, 6 de droite = LU.

## Pads (grille de 12)
- Tap = jouer la banque.
- SHIFT + tap (mode longueur) : régler la longueur de boucle (pads 1–9 = chiffres).
- FX maintenu + tap : longueur de boucle rapide.
- SHIFT + tap (mode sample) : ouvrir la page SAMPLE du pad.
- Double-tap SHIFT + pad : effacer la motion du pad.

## Projets / sauvegarde
- Page **PROJET** : TAP = CHARGER · APPUI LONG = SAUVER · DOUBLE-TAP = NOMMER · SWIPE = PAGE (4 pages).
- **Sauvegarde globale** : armer la sauvegarde puis taper un pad 1–4 (« GLOBAL n SAUVÉ »).
- **Chargement global** : SHIFT + MUTE maintenu + pad 1–4 de la rangée du bas.

## Nouveautés moteur (v0.42)
- **Synthés** : KIK8 (kick 808 : TUNE/PUNCH/DECAY), VOY (voix formantique : MODE < 50 % = robotique, > 50 % = humain avec vibrato et souffle), MAL (percussion modale type marimba/bois), BELL (cloche FM inharmonique).
- **Effets** : TEMPO (delay synchro BPM : 1/16, 1/16T, 1/16., 1/8, 1/8T, 1/4, 1/4., 1/2), PING (ping-pong stéréo), PLATE (réverb plaque), SHIM (shimmer, queue à l'octave), GATE (réverb gated).
- Tous ont leurs tirettes (SHIFT+TAP), sont snapshotables et mappables sur les axes F/Z, les LFO et l'ARP.
- **Punch-ins** : chaque déclenchement pousse les réglages complets de l'effet (tirettes) — un LFO branché dessus rend chaque punch-in légèrement différent. SHIFT + zone punch = page de réglages.
- **Longueur de boucle** : pendant que REC est armé (destination choisie, avant le lancement), la rangée 1–9 règle la longueur. Optionnel.
- **Boucles quantisées** : à l'arrêt manuel, la longueur est arrondie au temps le plus proche pour boucler proprement au tempo. Débrayable dans le menu (ENREGISTREMENT → BOUCLES QUANTISÉES).

## BPM
- Glisser verticalement l'écran BPM = ajuster.
- Tapper dessus en rythme = tap tempo.

## Faders
- **ZU/LU** : blend/morph entre scènes.
- **FLY** : effet fly (voir fader dédié).
- **MIX** : niveau de mix (vert).
- **LINE-IN** : gain d'entrée micro/ligne.

## Dépannage
- **Grésillement / craquements** : réduire le nombre d'effets actifs, vérifier qu'aucune autre app n'utilise l'audio, débrancher/rebrancher le casque (le flux audio redémarre automatiquement).
- **Son figé** : PANIQUE (menu) puis relancer.
- **Pas de son** : vérifier MUTE, le fader MIX, et le volume média du téléphone.
