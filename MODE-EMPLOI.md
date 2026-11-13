# ZULU — MODE D'EMPLOI (mémo v0.32)

Aide-mémoire des gestes et fonctions principales. Tout se joue au doigt, pensé pour le LIVE.

## Le son démarre TOUT SEUL

Depuis la v0.27, le moteur audio se lance à l'ouverture de l'appli : le synthé joué
au pad XY s'entend immédiatement. **START/STOP (VU + BPM appuyés ensemble)** coupe ou
relance le moteur (un message MOTEUR ON/OFF s'affiche).

## L'écran

- **Pad XY (gauche)** : pièce maîtresse. Curseur jaune = axe **F**, curseur rose = axe **Z**.
  Fond vivant (MENU) : **KALÉIDOSCOPE** — tes doigts dessinent dans le sable, dupliqués
  en 8 miroirs, et ça s'efface lentement/normalement/rapidement (vagues) ; ou **MANDALA**
  qui respire avec le son et les mouvements (turquoise = doux, chaud/rouge = agressif) ;
  ou **AUCUN** (économie batterie).
- **Rangée du haut du pad** : 9 boutons banques FX/synth user presets + **PUNCH**
  (ouvre la grille punch-in FX).
- **Bord du pad** : **MUTE** (en bas) et **SHIFT** juste au-dessus.
- **Ligne du haut à droite** : **FX** (rond), **ARP** (rond), **F**, **Z**, **REC**, **UNDO**
  (à la file), **VU-mètre agrandi** + **cadran BPM** style Casio, **PLAY/STOP** (rond, à droite).
- **Dessous** : 12 pads scènes/banques (8 samples + 4 solo par paires de colonnes),
  **orange**, dernière rangée **rose magenta**. Dans la colonne de gauche (au-dessus
  de SHIFT), un **badge de mode** affiche où on en est : **SCÈNE** (ambre) / **MOTION**
  (turquoise) / **SAMPLES** (vert). **Tap sur le badge** = basculer SCÈNE → MOTION →
  SAMPLES.
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
RND (slices aléatoires BPM), SLICR (hachage SamplR : gate + ordre des slices),
CLOUD (nuage granulaire), FRZ (freeze granulaire) — ces moteurs jouent la banque
sélectionnée (tap sur un pad banque = banque du player).
- **Tap** = ON/OFF (le bouton passe ambre quand il est actif) ; pour un SYNTH, tap =
  le charger, puis **jouer sur le pad XY**.
- **SHIFT + tap** ou **EDIT** (bouton dans la fenêtre, puis tap sur l'effet) = page de
  réglages de l'effet (tirettes). **La page EDIT reste bloquée** jusqu'au re-tap sur FX
  (on retrouve le pad XY). En EDIT : **tap sur une banque 1-9 = 1er effet de la page,
  double-tap = 2e effet** (navigation directe).
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
| REC → (F/Z) → pad → REC | ENREGISTRER (protocole unifié, voir section REC) |
| Re-tap REC ou pad | couper avant la fin EN GARDANT ce qui est enregistré |
| Badge (colonne gauche) | tap = cycle SCÈNE → MOTION → SAMPLES |
| Potard IN (ligne du haut) | glisser verticalement = gain de l'entrée line-in |
| Slot FX (1-9) maintenu + pad banque (ou l'inverse) | assigner les effets actifs à cette
  banque de samples ; refaire la manip = retirer les FX (banque DRY). Quand la banque
  joue, ses effets se chargent tout seuls (façon SP-404) |
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
| MENU · STYLE | 84 (classique) ou RETRO (jeu vidéo d'époque : scanlines, pixels,
  pads carrés) |
| Pincer à 3 doigts sur le pad | fusion des 2 curseurs |
| Double-tap sur les curseurs fusionnés | défusion billard (rebonds aléatoires) |
| SHIFT + écran (pad) | BEND : 4 ambiances aux 4 coins (doux / blip / anarchie / tempête) |

## REC unifié (v0.32, façon SP-404)

**Tap sur REC** = armer, **re-tap REC ou sur le pad** = stopper EN GARDANT (sinon
coupure automatique quantisée à la longueur choisie — longueur max = coupe auto,
longueur courte = re-tap pour couper avant).

- **Sample audio** : REC → F et/ou Z (avec les effets ; rien = sec/dry) → pad de
  destination (il clignote) → REC pour lancer.
- **Motion sequence** (remplace le snapshot sur ce pad) : REC → F et/ou Z (les axes à
  enregistrer) → pad → REC. Les mouvements des curseurs sont rejoués en boucle
  (longueur = réglage de boucle en cours). Tap sur le pad actif = stop.
- **Snapshot** : REC → ZU ou LU (choisir le côté) → pad (1-6) → REC. Capture TOUS les
  réglages en cours, punch-ins compris (ils se réactivent au rappel).
- **Édition TRANSPARENTE** (MENU · SNAPSHOT, activée par défaut) : une fois le pad de
  destination choisi, les réglages que tu bouges (curseurs, MIX, filtre, tirettes,
  punch) ne s'entendent PAS — ils ne prennent effet qu'une fois la snapshot enclenchée
  (fader ou pad). Option **DIRECT** au menu pour retrouver l'ancien comportement.
  **Par effet** : dans la page EDIT d'un FX, le bouton **SNAP** choisit le
  comportement de CET effet pendant la capture — **AUTO** (suit le menu),
  **SNAP:MUTE** (silencieux, défaut) ou **SNAP:SON** (il continue de s'entendre
  pendant que tu règles les autres).

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

- REC : protocole unifié (section REC unifié). Longueur : FX + pad (1-14 temps),
  quantisée au BPM.
- REC longue durée auto par paliers (≤7 s = 48 kHz, puis 24/16/12 kHz, hauteur conservée).
- Saveurs lo-fi REC : OFF / SP / K7 / MÉTAL (MENU).
- Import WAV et projets Koala Sampler (.koala) : MENU.
- **UNDO** (haut droite) : restaure le contenu précédent de la dernière banque enregistrée.

## Punch 9 (MENU → PUNCH 9)

Le pad XY devient une grille 3×3 de punch-in effects façon Pocket Operator, avec
10 collections en tête : ZULU, APHX, HGTN, SKRX, DOOM, WAIT, RHED, BOC, DAFT, W/Y.
**SHIFT + tap** = sortie. Le bouton PUNCH se referme normalement (comme les autres pages).

- **v0.32** : les punch-ins sont **capturés dans les snapshots** et se réactivent au
  rappel de la scène / au fader. Effets **corsés** (boostés ~×1,5) + combo **REV**
  (zone 1 de la collection ZULU) = filtre passe-haut + lo-fi + reverb.
- **SHIFT + tap sur un punch** = page EDIT (tirettes) de cet effet.

## Scènes (v0.32)

- Tap **ZU** ou **LU** = choisir le côté joué (6 scènes chacun, rangées gauche/droite
  par défaut). Les boutons s'allument (ZU turquoise / LU violet).
- Tap sur un pad 1-6 = jouer la scène de ce côté (pad vide = silence, c'est voulu).
- Plus aucun saut de fader ni capture par appui maintenu : tout passe par le REC unifié.
- Morph ZU/LU via le grand fader. **FLY reste indépendant** : jamais dans les scènes ni
  les snapshots (seul BEND peut le toucher).

## Drums & synthé embarqués (façon Kaossilator)

- Générateurs pilotés au pad : **KICK / SNARE / HAT** (X = accordage/brillance,
  Y = decay) + basses/acid, sirènes, bruit, square, FM.
- Synthé : gamme (japonaises/penta + custom via clavier sur le pad), fondamentale,
  tone/attaque/decay, octave, pitch, tune.

## OUTILS (bouton ARP, v0.32)

- **Tap** = cycle des pages : **ARP** → **GRID** (drum pads) → rien.
- **SHIFT + tap** = ARP ON/OFF.
- **GRID** : 12 ou 16 pads (MENU · DRUM PADS) jouant la banque sélectionnée, à la place
  du pad XY. Modes en tête de grille : **12-LEVELS** (pitch façon MPC), **SLICE**
  (slices du sample), **RND** (slices + pitch aléatoires). **×** = sortir.

## Boucles MIDI (v0.32)

Par défaut, les **drums et synths** s'enregistrent en **boucles MIDI** (les événements
sont rejoués, les paramètres restent retouchables derrière) ; les slots LECTURE SAMPLE
restent en audio. **MENU · REC ENGINES** : MIDI ou AUDIO.

## Orientation (v0.32)

**MENU · ORIENTATION** : **HORIZ** (défaut) ou **VERT** (pad XY en haut, boutons en
dessous). Jamais de rotation automatique ; le changement redémarre l'appli.

## Entrée & sortie studio (v0.32)

- **Potard IN** (ligne du haut, après UNDO) : glisser verticalement = gain line-in.
- **MENU · ENTRÉE (LINE-IN)** — 4 profils avec compresseur/coupe-bas adapté :
  **TRANSP** (limiteur seul, défaut : ça ne sature plus si ça tape dans le rouge),
  **INSTRU** (kalimba, cordes), **VOIX** (chant), **BBOX** (beatbox : transients
  serrés + légère saturation chaude).
- **MENU · EQUALISEUR** : ON/OFF — EQ 3 bandes doux, appliqué en entrée ET en sortie.
- **MENU · BOOST** : IN x1/x2/x4 et OUT x1/x2/x3.
- **MENU · LOUDNESS** : ON/OFF — maximiseur de loudness en sortie (compresseur +
  makeup + limiteur doux).
- **Catégorie STUDIO dans la fenêtre FX** (retraitement subtil, idéal avant resampling) :
  **EQ3** (3 bandes réglables), **COMP**, **WARM** (saturation k7 douce), **CLEAN**
  (coupe-bas/coupe-haut + gate). Et dans TEXTURE : **FUZZ** (fuzz asymétrique qui
  "sag") et **DISTO** (grosse disto symétrique).

## Multi-assign des axes (v0.32)

Un axe (F ou Z) peut piloter PLUSIEURS banques en même temps (ex. F = 1+3,
Z = 5+7), et un même paramètre peut recevoir F et Z à la fois (croisement).

Protocole, fenêtre FX ouverte :
1. **Tap sur F ou Z** : le bouton clignote → mode assignation.
2. **Tap sur les banques 1-9** : ajoute/retire la banque des cibles de l'axe
   (max 8 cibles par axe). Seuls les params XY des FX actifs de la banque
   sont repris ; banque vide = « BANQUE VIDE », aucun param XY = « AUCUN
   PARAM XY ».
3. **SHIFT + banque** : courbe de la banque pour cet axe — **NORM** (direct),
   **INV** (inversé), **S&H** (échantillonné-bloqué), **BOUILLON** (jitter).
4. **Re-tap sur F/Z ou fermer la fenêtre FX** : sortie du mode.

Les **snapshots capturent aussi le routage** multi-assign (banques + courbes
F/Z) : rappeler un snapshot remet le routage qui était actif à la capture.

## Chaîne MASTER (v0.32)

Dans la page **EDIT** d'un FX (SHIFT + FX, ou EDIT dans la fenêtre FX), le
bouton **MASTER** en haut à droite route l'effet sur le master : c'est un
pont direct vers la sortie, même si l'effet n'est assigné à aucune banque.
Les masques d'effets de banques ne l'éteignent pas, et le routage est
mémorisé dans les snapshots. Rappel : un effet routé au master ne s'entend
que s'il est **activé** (tap sur l'effet) — pas d'activation, pas de son.
Re-appui sur MASTER = retirer du master. Max conseillé : quelques FX, la
chaîne master s'entend sur TOUT.

## Réseau : les FX qui se parlent (v0.32)

Quand plusieurs banques sont multi-assignées, **MENU · RESEAU** fait
communiquer les cibles entre elles (ce que fait un axe réagit sur l'autre).
7 profils : **MYCELIUM** (lent, organique), **HUMUS** (très lent, profond),
**NET VINTAGE** (rafales de machines + aliasing), **CHAOS** (tempête),
**OISEAUX** (échos qui se répondent), **INSECTES** (stridulation rythmique),
**MARÉE** (respiration très lente, lune). Réglages : **CHAOS**
(doux/moyen/fort), **MODULATION** (léger/moyen/fort), **ALIASING**
(off/léger/fort), **VITESSE** (lente → frénétique).

## Projets : 32 slots, noms et couleurs (v0.32b)

Le panneau **PROJET** a 4 pages de 8 slots (32 au total) : **swipe**
gauche/droite ou flèches ‹ › pour changer de page. **Tap = charger**,
**appui long = sauver**, **double-tap = nommer + choisir la couleur de
l'étiquette** (6 couleurs). Les noms et couleurs sont mémorisés.

## Template de base + panique (v0.32b)

- **MENU · TEMPLATE DE BASE** : **SAUVER L'ETAT ACTUEL** — cet état se
  recharge tout seul à chaque démarrage (niveau 0, là où tu te retrouves).
  **EFFACER** revient au défaut d'usine au démarrage.
- **MENU · PANIQUE** : **TOUT REMETRE A ZERO** — stoppe tout, éteint les FX,
  remet filtre/axes/punch/solo/mute à zéro. Les sauvegardes sont conservées.

## Logs et noms des samples (v0.32b)

- Une **ligne de log** sous la grille affiche le dernier sample touché :
  banque, nom, date/heure d'enregistrement, durée.
- **MENU · LOGS SAMPLES** : ON/OFF (ON par défaut) pour cacher la ligne.
- **MENU · NOMS DES SAMPLES** : nommer chaque banque B1-B8 (le « coin-coin »,
  le « magnifique violon… »). Les noms partent dans les sauvegardes de projet.

## Porte de sortie FX (v0.33)

Rappuyer sur le bouton **FX** ferme TOUT : page EDIT, fenêtre FX, punch-ins,
tout se referme d'un coup et on retrouve le pad XY. Plus de blocage dans les
pages.

## OUTILS : tap direct + GRID (v0.33)

- **Tap sur OUTILS** (ARP/LFO/sampleur) : ouvre direct le **programme**
  (sections LFO / arpégiateur / sampleur). Pas besoin de SHIFT.
- **SHIFT + tap sur OUTILS** : ouvre/ferme direct la **GRID** (drum pad sur
  les samples : 12 pads, pitch ±12 façon MPC, modes slicé et random).
- Sur la GRID : **SHIFT + tap sur OUTILS** = changer de mode de grille
  (**NOTES / SLICE / RND**). Fermer la grille : le **×** en haut à droite.
- **Mode NOTES** : grille façon Koala Sampler — **32 cubes = 2 octaves**
  chromatiques (grave en bas à gauche → aigu en haut à droite).
  **MENU · DRUM PADS** : 8 / 12 / 16 / **32** cubes — moins de cubes = cubes
  plus gros. SLICE = tranches du sample, RND = tranches aléatoires avec pitch.
- **Sélecteur d'octave** sous la barre de modes : **-2 / -1 / 0 / +1 / +2** —
  décale toute la grille d'octaves supplémentaires (jusqu'à 4 octaves au
  total), sans glide, just un sélecteur.

## Tirettes EDIT universelles (v0.33)

**TOUS** les effets (y compris punch-ins), **tous** les synthés (SAW, SQUARE,
FM, NOISE, 303) et **tous** les drum engines (KICK, SNARE, HAT, PERC) ont
leur page **EDIT** avec 4 tirettes verticales (k0-k3, la 4e = NIVEAU).
- Dans la fenêtre FX : **EDIT** puis tap sur un effet → ses tirettes.
- **SHIFT + tap** sur une zone punch-in → ses tirettes (1-2 réglages pour
  nuancer : plus dur, couleur différente…).
- Toutes les tirettes sont exposées aux **axes X/Y**, aux **LFO**, à
  l'**arpégiateur** et aux **snapshots** (voyants de couleur au-dessus).
- Le bloc « ASSIGNATION AXES » a été **supprimé** : l'assignation se fait
  **uniquement depuis les tirettes** de la page EDIT.

## Kaossilator sur le pad XY (v0.33)

Les slots synthé **SAW / SQUARE / FM / NOISE / 303** se jouent directement
sur le pad XY, comme le Kaossilator de Korg :
- **X = notes de la gamme** : graves à gauche → aigus à droite (2 octaves,
  quantifié sur la gamme choisie).
- **Y = filtre** : fermé en bas → ouvert en haut (120 Hz → 12 kHz).
- Le son ne joue que doigt posé (gate), pas de motif automatique — les
  motifs, c'est le boulot de l'arp/LFO (mappés sur les tirettes).
- Tirettes : **TUNE / ATTAQUE / RELEASE / NIVEAU**.
- Le **303** ajoute le slide (GLIDE) et un filtre résonant acide :
  **CUT / GLIDE / RESO / NIVEAU**.

## Drums percussifs : 9 zones (v0.33)

Les slots **KICK / SNARE / HAT / PERC** ne jouent **aucun motif tout seul** :
le pad devient 9 zones (3×3), chaque zone = un son one-shot, **glissés
possibles** (les hits se déclenchent en cascade en glissant le doigt).
- **Colonne = modèle**, **rangée du bas = le plus dur/sec** :
  - KICK : **808 / 909 / HARD**, TUNE / DECAY / CLICK / NIVEAU.
  - SNARE : 3 variantes sèches, TON / DECAY / GRAIN / NIVEAU.
  - HAT : fermé / mi / ouvert, TON / DECAY / BRIGHT / NIVEAU.
  - **PERC** : toms grave/medium/aigu, tabla bayan/dayan/tin, cowbell,
    bells grave/aigu — TON / DECAY / MORDANT / NIVEAU.
- Si tu veux des motifs : mappe l'**arpégiateur** ou un **LFO** sur les
  tirettes du slot.

## Stutter FLY réparé (v0.33)

Le stutter du fader FLY ne craque plus : sortie en **fondu** (crossfade
lissé) au lieu d'une coupure sèche.

## MODE ÉCO (v0.33)

**MENU · MODE ECO (BATTERIE)** : OFF/ON. Mode « économie de carburant » :
- drums et synthés en **bitrate réduit** (96 pas) compensé par un
  **loudness** (grain chaud) ;
- **voix granulaires réduites** (4 → 2) ;
- **fond animé coupé** (mandala/kaléidoscope) pour économiser la batterie.
Un peu dégradé, mais ça tourne et ça sonne.

## VU mètre : IN/OUT + swipes volume (v0.34)

Le VU mètre affiche maintenant **4 barres** : **OUT L/R** (sortie, vert) et
**IN L/R** (line-in, cyan).
- **Swipe vertical** sur le VU = **volume général** (0-200 %).
- **Swipe horizontal** sur le VU = **volume line-in** (0-200 %, synchronisé
  avec le petit potard IN).
- Un aperçu « VOL 80 % » / « IN 120 % » s'affiche pendant le réglage.
- **Appui long sur le VU** = menu. Plus de raccourcis miroir ni double-tap :
  MIROIR, GAMME et le nombre de PADS se règlent dans le **menu**.

## Fusion : pincer à 2 doigts (v0.34)

La fusion des deux points se fait maintenant en **pinçant à 2 doigts** et en
**tenant 1 seconde** (au lieu de 3 doigts). La séparation billard reste au
double-tap.

## REC long : jusqu'à 19 temps (v0.34)

1. **REC** → armer.
2. **Tap sur le pad destination** → il s'arme.
3. **Rester appuyé 2 secondes** sur ce pad → saisie de longueur.
4. **Pads 1-9 = chiffres** : tap simple = 1 à 9 temps ; **1 puis 4 = 14**,
   **1 puis 6 = 16**… **19 temps max**. (1 seul = attendre 2 s ou REC.)
- **MENU · TEMPS LOOP** : **÷2 / ×2** pour subdiviser/doubler la durée de
  boucle (1-19 temps), tap sur le chiffre = retour à 8.

## Resampling avec FX : SHIFT + REC (v0.34)

**SHIFT + REC** arme un enregistrement **avec les effets** (resampling à la
SP-404, pas besoin de F/Z = WET) : choisir le pad destination, même protocole
de longueur (pad tenu 2 s + chiffres).

## STRETCH samples : OFF / CLEAN / BEAT / RETRO (v0.34)

**MENU · STRETCH SAMPLES · B<n>** : qualité/mode de lecture de la banque
sélectionnée. Quand le stretch est actif, le pitch des steps (±12 façon MPC)
et les changements de vitesse deviennent du **time-stretch** : la vitesse
change, la hauteur reste.
- **OFF** : lecture normale (pitch = vitesse).
- **CLEAN** : stretch granulaire lisse (grains 2048, fenêtrage doux) — ralentis
  et accélérés propres.
- **BEAT** : stretch « splice » calé sur le temps de la boucle — coupures
  nettes à chaque temps, le groove reste carré (esprit SP-404 beat stretch).
- **RETRO** : grains courts + réduction de bits — jungle/lo-fi crade, ça
  granule et ça craque exprès.
Le réglage est mémorisé par banque dans les sauvegardes de projet.

## Éditer un sample : SHIFT + pad (v0.35)

En mode **SAMPLES** : tap sur un pad = toggle on/off (comme avant).
**SHIFT + pad** ouvre sa **page d'édition** (tirettes) :
- **PITCH** : ±12 demi-tons (la vitesse de lecture suit le pitch).
- **ATTAQUE** : fondu d'ouverture au (re)démarrage de la boucle.
- **DECAY** : fondu de fermeture ; à fond = sustain (pas de decay).
- **TONE** : filtre passe-bas du sample (aiguë → fermé → sourd).
- **DÉBUT / FIN** : fenêtre de lecture dans le sample (coupe le début ou la fin).
SHIFT + pad **sélectionne** aussi la banque : les FX sample dédiés se
routent ensuite comme d'habitude (pad tenu + slot FX), et chaque tirette
est assignable (**XY / LFO / ARP / SNAP** via Shift + zone, voyants colorés).
Réglages mémorisés dans les sauvegardes de projet.

## Effacer : SHIFT + double-tap (v0.35)

- Mode **SCÈNE** : **SHIFT + double-tap** sur un pad = efface **toutes** les
  données assignées sur ce pad (snapshot, motion, sélection de côté).
- Mode **MOTION** : **SHIFT + tap** sur une banque = **doubler la longueur**
  de la motion séquence (jusqu'à 19 temps, pour un overdub plus long) ;
  **SHIFT + double-tap** = effacer la **motion** (le snapshot reste).
- Le premier tap attend un instant (450 ms) avant d'agir : c'est normal.

## Reverse : pad tenu + double-tap SHIFT (v0.35)

**Appuie sur un pad (boucle audio) et garde-le appuyé, puis double-tap sur
SHIFT** : la boucle se lit **à l'envers** (mode reverse). Refais la même
manip pour revenir en lecture normale. Fonctionne dans tous les modes,
audio loop uniquement.

## Demarrage silencieux (v0.37)

L'app démarre **sans aucun son** : arpégiateur, générateur et boucles sont
coupés à l'allumage, même si le template de base en contenait. C'est toi qui
lances le son quand tu veux. Les **LFO** aussi sont coupés à l'allumage
(v0.39) : plus d'effets « rebondissants » tant que tu ne les rallumes pas.

## Enregistrer une scene : maintenir ZU / LU (v0.38)

1. **Tap sur ZU** (ou LU) → tu choisis le côté ; **tap sur une banque** → tu
   joues cette scène.
2. Pour enregistrer : **maintiens ZU (ou LU) appuyé** (le bouton affiche
   « ZU·CAP »), **fais ton réglage** pendant ce temps, **relâche** → le
   snapshot est capturé **sur la banque active** de ce côté. Refais la même
   chose sur le même pad pour le refaire.
- Un tap court ne capture pas (il ne fait que changer de côté).
- Le protocole **REC → ZU/LU → pad → REC** reste disponible pour choisir
  librement le pad de destination.

## Anti-Larsen (v0.36)

- **MIC ON / MIC OFF** dans le **MENU · MICRO / ANTI-LARSEN** : active la vraie
  entrée (micro téléphone ou carte son USB). La permission micro est demandée
  au premier ON.
- **Protection auto** : si le micro est actif et que le casque se débranche,
  Zulu **coupe le line-in tout seul** (toast « CASQUE DÉBRANCHÉ ») — pas de
  Larsen dans les oreilles.
- **Mute manuel** : **2 swipes horizontaux rapides sur le VU mètre** = line-in
  muet ; **2 swipes again** = le son revient au niveau d'avant.

## Destinations LFO / ARP visibles et routables (v0.40)

Dans la page ARP/LFO (bouton outils), une section **DESTINATION** montre où
va chaque modulateur : **A -> ...**, **B -> ...**, **ARP -> ...** (nom lisible
de la cible : effet + tirette, moteur, paramètre global ou tirette sample).

- **Tap sur A / B / ARP** (ou sur le bouton de destination) : un mini
  navigateur s'ouvre : **RIEN (DÉPATCHER)**, puis les **sections d'effets**
  (grille), puis les **tirettes** de la section choisie. Tap sur une tirette =
  le modulateur est **repatché** dessus, tout simplement.
- **MONTANT ARP** : profondeur de la modulation arp (25/50/75/100 %). L'arp
  module sa cible en escalier, un motif différent à chaque pas.
- Le routage depuis les tirettes (voyant vert LFO / rose ARP) marche toujours,
  la destination se met à jour toute seule dans la page.

## Nouveaux moteurs (v0.40)

Trois moteurs inspirés des modules Mutable Instruments, dans la section SYNTHÉ
du navigateur d'effets (SHIFT+tap = tirettes) :

- **VOX** : synthé formant (TUNE / FORMANT / AIR / NIVEAU) — voix, nappes.
- **PLUK** : corde pincée Karplus-Strong (TUNE / AMORTI / RICHE / NIVEAU).
- **TIDE** : oscillateur morphologique (TUNE / FORME / PLI / NIVEAU) — du
  sinus aux dents de scie, avec pliage d'onde.

## Punch-in retravaillés (v0.40)

Les 10 collections de punch-in sont **dé-doublonnées** et chaque case a
maintenant sa **personnalité** (un réglage de son effet est appliqué à
l'activation) : APHEX (acide clinique), SKREAM (dub profond), SKRILLEX
(brutal), DOOM (poussiéreux), WAITS (cabaret), GLITCH (haché), DOUCE (subtil
studio), MAGNETO (k7), CLUB, SABLIER (vinyl/stops). Certains sont discrets,
d'autres très corsés.

## Panique et démarrage : état neutre complet (v0.40)

- **PANIQUE** coupe maintenant **tout** : tous les effets (même ceux collés
  par les punch-in), le générateur (remis sur SAW, muet), arp, LFO, boucles,
  motion, punch, solo, mute. Plus de son qui traîne.
- **Chaque démarrage** est neutre : remise à zéro aussi des axes
  multi-assign, reverse de boucles, snapshots actifs et latches de punch.

## JUNGLE : granulaire choppe drum'n'bass (v0.41)

- JUNGLE (section GRANULAIRE) est refait de fond en comble : 6 grains
  simultanés, chacun pioché à un endroit différent du son, avec de gros
  sauts de pitch (octaves, quintes, +24) et un pan stéréo aléatoire par
  grain. Le mix passe en remplacement franc du son, plus de voile discret.
- **FOLIE** augmente la densité de répétitions stutter (le même bout de
  son rejoué à des hauteurs différentes, façon jungle) et la proba de
  lecture à l'envers.
- Tirettes : TAILLE (durée des grains), FOLIE (chaos/stutter), SAUT
  (distance de lecture dans le passé), NIVEAU.

## À venir (v0.34+)

- Vendoring réel de Plaits (Mutable Instruments, MIT) — cette passe utilise
  des engines « inspirés MI » écrits à la main.
- Drums/synthés enregistrés en événements (MIDI-like, paramètres
  retouchables, option dans le MENU complet) ; audio uniquement au
  resampling.
- 2 arpégiateurs routables, capture du son interne, MIDI inter-app, carte
  son USB, export/import Koala Sampler.
