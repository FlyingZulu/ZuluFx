# ZULU — MODE D'EMPLOI (mémo v0.29)

Aide-mémoire des gestes et fonctions principales. Tout se joue au doigt, pensé pour le LIVE.

## Le son démarre TOUT SEUL

Depuis la v0.27, le moteur audio se lance à l'ouverture de l'appli : le synthé joué
au pad XY s'entend immédiatement. **START/STOP (VU + BPM appuyés ensemble)** coupe ou
relance le moteur (un message MOTEUR ON/OFF s'affiche).

## L'écran

- **Pad XY (gauche)** : pièce maîtresse. Curseur jaune = axe **F**, curseur rose = axe **Z**.
  Fond : mandala discret qui respire avec le son (turquoise = doux, chaud/rouge = agressif).
- **Rangée du haut du pad** : 9 boutons banques FX/synth user presets + **PUNCH**
  (ouvre la grille punch-in FX).
- **Bord du pad** : **MUTE** (en bas) et **SHIFT** juste au-dessus.
- **Ligne du haut à droite** : **FX** (rond), **ARP** (rond), **F**, **Z**, **REC**, **UNDO**
  (à la file), **VU-mètre agrandi** + **cadran BPM** style Casio, **PLAY/STOP** (rond, à droite).
- **Dessous** : 12 pads scènes/banques (8 samples + 4 solo par paires de colonnes),
  **orange**, dernière rangée **rose magenta**. Un badge en haut à droite de la grille
  affiche toujours le mode : **SCÈNE** (ambre) / **MOTION** (turquoise) / **SAMPLES**
  (vert, quand SHIFT est enfoncé).
  Boutons **ZU / LU**, grand fader **ZU/LU**, puis faders **FLY** et **MIX**.
- **Curseurs façon Kaoss Pad** : on touche n'importe où sur le pad → le curseur libre
  le plus proche vient sous le doigt et suit exactement ; au relâchement il **reste sur
  place** (pas de retour fantôme). F OFF = axe désactivé, F HOLD = axe figé
  (le doigt prend alors l'autre curseur).

## Fenêtre FX · SYNTH

Le bouton **FX** ouvre une fenêtre avec **toutes les catégories en grille de pads
colorés** : FILTRE (turquoise), MODULATION (violet), TEMPS (vert), TEXTURE (ambre),
RESONANCE (rose), GRANULAIRE (cream), TRANSITIONS (jaune), SYNTH (orange),
**LECTURE SAMPLE** (menthe) : SPLY (lecteur), GRAN (granulaire), JGL (jungle rétro),
RND (slices aléatoires BPM) — ces moteurs jouent la banque sélectionnée
(tap sur un pad banque = banque du player).
- **Tap** = ON/OFF (le bouton passe ambre quand il est actif) ; pour un SYNTH, tap =
  le charger, puis **jouer sur le pad XY**.
- **SHIFT + tap** ou **EDIT** (bouton dans la fenêtre, puis tap sur l'effet) = page de
  réglages de l'effet (tirettes).
- **Tap hors de la fenêtre** ou **×** = fermer.

## Gestes essentiels

| Geste | Action |
|---|---|
| VU + BPM appuyés ensemble | START/STOP |
| Appui long sur VU | MENU (sauvegarde globale en premier : grille de projets) |
| Double-tap VU | pads 8 → 10 → 12 → 14 |
| SHIFT + double-tap VU | GAMME |
| SHIFT + appui long VU | miroir du layout (gaucher/droitier) |
| Tap tempo | tapoter le cadran BPM |
| SHIFT + pad banque | REC banque (longueur = réglage en cours) |
| FX maintenu + pad banque | régler la longueur de boucle (1-14 temps) |
| FX maintenu (ou tapé) + banque 1-9 | sauvegarder le duo d'effets dans la banque
  (effets actifs + réglages + assignations d'axes) ; message PRESET X SAUVÉ |
| SHIFT + double-tap sur le pad | cycle d'INERTIE : ∞ (glisse sans fin) → 0,5 → 1 → 2 → 4 s
  (aussi dans le MENU, rangée BILLARD) |
| Appui long pad banque | éditeur de pas (pitch/vélocité par pas) |
| SHIFT + MUTE | resampling (réenregistre la sortie en banque) |
| SHIFT + MUTE + pad bas 1-4 | CHARGER une sauvegarde GLOBALE (tout bascule :
  BPM, effets, presets, banques audio — façon parts Octatrack) |
| SHIFT + MUTE double-tap, puis pad bas 1-4 | SAUVEGARDER la GLOBALE 1-4 (armé 1,5 s) |
| SHIFT + double-tap sur le pad | cycle d'INERTIE des curseurs (∞ / 0,5 / 1 / 2 / 4 s) |
| Banques basses (mode 12 pads) | SOLO par paires de colonnes (aucun solo = silence) |
| ZU / LU maintenu ≥ 400 ms | capture snapshot |
| Pincer à 3 doigts sur le pad | fusion des 2 curseurs |
| Double-tap sur les curseurs fusionnés | défusion billard (rebonds aléatoires) |
| SHIFT + écran (pad) | BEND : 4 ambiances aux 4 coins (doux / blip / anarchie / tempête) |

## FX send par sample (v0.26)

**SHIFT + pad sample**, puis :
- **F** = FX send ON (le sample passe dans le rack d'effets)
- **Z** = FX send OFF (sample sec, hors rack)
- **F et Z ensemble** = bascule ON/OFF

(Le REC déclenché par SHIFT+pad est annulé automatiquement.)

## Page de réglages des effets (v0.26)

Dans le panneau FX : **SHIFT + tap sur un bouton d'effet** (DUB, MEGA, TAPE, WARP, TALK,
RINGS) ouvre sa page de réglages **à la place du pad XY** : tirettes verticales façon
vieux synthé (3 à 8 réglages par effet).

- **Glisser une tirette** = régler la valeur.
- **Tap sur la zone des voyants** (haut) ou **SHIFT + tap sur une tirette** = cycle
  d'assignation du paramètre : rien → **XY** → **LFO A** → **LFO B**.
- **4 voyants au-dessus de chaque tirette** : turquoise = axe XY, vert = LFO,
  rose = ARP (v0.27), ambre = snapshot (v0.27).
- **Légende** en haut à gauche de la page.
- **SHIFT + tap en haut/bas de la page** = sortie.

## Arpégiateur

- Bouton **ARP** (10e bouton de la rangée du haut) : ON/OFF.
- **SHIFT + ARP** : panneau arpégiateur (modes dont XY SamplR, rate, octave, gate,
  sauts d'octave, nombre de slices). Le LFO custom se dessine dans la page LFO.

## Looper / sampler (façon KP3)

- REC banque : SHIFT + pad. Longueur : FX + pad (1-14 temps), quantisée au BPM.
- REC longue durée auto par paliers (≤7 s = 48 kHz, puis 24/16/12 kHz, hauteur conservée).
- Saveurs lo-fi REC : OFF / SP / K7 / MÉTAL (MENU).
- Import WAV et projets Koala Sampler (.koala) : MENU.
- **UNDO** (haut droite) : restaure le contenu précédent de la dernière banque enregistrée.

## Punch 9 (MENU → PUNCH 9)

Le pad XY devient une grille 3×3 de punch-in effects façon Pocket Operator, avec
10 collections en tête : ZULU, APHX, HGTN, SKRX, DOOM, WAIT, RHED, BOC, DAFT, W/Y.
**SHIFT + tap** = sortie.

## Scènes

- 12 pads : 6 gauche = ZU, 6 droite = LU (rangement par défaut).
- Snapshots façon Octatrack : ZU/LU maintenu, bouger les paramètres, relâcher.
- Morph ZU/LU via le grand fader.

## Drums & synthé embarqués (façon Kaossilator)

- Générateurs pilotés au pad : **KICK / SNARE / HAT** (X = accordage/brillance,
  Y = decay) + basses/acid, sirènes, bruit, square, FM.
- Synthé : gamme (japonaises/penta + custom via clavier sur le pad), fondamentale,
  tone/attaque/decay, octave, pitch, tune.

## À venir (v0.27+)

- Routage ARP et snapshots des tirettes (voyants rose/ambre).
- Drums/synthés enregistrés en événements (MIDI-like, paramètres retouchables,
  option dans le MENU complet) ; audio uniquement au resampling.
- Mode DRUM PAD (±12 pitch MPC, slices, random), 2 arpégiateurs routables,
  capture du son interne, MIDI inter-app, carte son USB, motion sequences.
