# Vôlavo

Transforme le téléphone Android en **souris sans fil haute précision** et en console de montage
(trackpad, jog/shuttle, raccourcis Premiere / DaVinci / Final Cut / Canva…), en **Bluetooth** ou en **Wi-Fi**.

```
app/    Application Android (Kotlin, Jetpack Compose)
host/   Vôlavo Hôte — logiciel compagnon pour le PC (Python, Windows / macOS / Linux)
```

## Connexion en un scan

1. Sur l'ordinateur : lancez **Vôlavo Hôte** (`host/start_windows.bat` ou `host/start_mac_linux.sh`).
2. Sur le téléphone : **Connexion › Scanner le QR code du PC**.

Le QR code contient l'adresse du PC, son port, une clé d'appairage et son adresse Bluetooth.
Le téléphone se reconnecte ensuite automatiquement. Détails : [`host/README.md`](host/README.md).

Le mode **Bluetooth HID** fonctionne aussi sans aucun logiciel : le téléphone est vu comme une vraie
souris + clavier Bluetooth.

## Interface

* Navigation par le **menu ☰ en haut à droite** (plus de barre du bas : toute la hauteur sert au trackpad).
* Barre du haut : titre de l'écran + état de la connexion (touchez-le pour gérer la connexion),
  bouton **mode précision** sur le trackpad et la molette.
* Bandeau « Aucun ordinateur connecté » avec un bouton **Connecter** quand rien n'est relié.
* Au premier lancement, l'app s'ouvre directement sur l'écran de connexion.
* Palette sombre « minuit » (violet / turquoise) conforme WCAG AA, nouvelle icône, cibles tactiles ≥ 48 dp.
* Trackpad : plus de bouton « clic gauche » (toucher = clic, double-toucher = double-clic,
  toucher puis glisser = sélectionner / déplacer). Restent « Maintenir le clic » (glisser sur plusieurs
  mouvements) et « Clic droit ».

## Onglet Clavier

* Clavier complet **AZERTY ou QWERTY** (au choix, selon le clavier de l'ordinateur ; le choix est mémorisé) :
  touches F1–F12, Échap, navigation, flèches, Ctrl / Maj / Alt / AltGr / Win « collants »
  (1 appui = touche suivante, 2 appuis = verrouillé).
* Envoi de texte : en Wi-Fi tout caractère (accents, émojis) ; en Bluetooth conversion automatique AZERTY/QWERTY.
* Touches multimédia (volume, lecture, piste suivante…).
* Bibliothèque de plus de 150 raccourcis classés (Édition, Texte, Windows, Explorateur, Navigateur,
  Bureautique, Présentation, macOS, touches seules) avec recherche.

> Bluetooth : le descripteur HID inclut désormais les touches multimédia. Si le PC était déjà appairé,
> supprimez « Vôlavo Mouse & Deck » des appareils Bluetooth du PC puis appairez-le à nouveau.

## Optimisations

| Domaine | Avant | Maintenant |
|---|---|---|
| Protocole Wi-Fi | JSON (~80 o) + une coroutine lancée par paquet | binaire (5 o), thread d'envoi dédié haute priorité, buffers pré-alloués |
| File d'attente | chaque événement mis en file → retard qui s'accumule | mouvements **fusionnés** : jamais de retard accumulé |
| Wi-Fi | économie d'énergie active | verrou Wi-Fi *low latency* + DSCP EF (file « voix » WMM) |
| Latence affichée | valeur aléatoire 2–5 ms | **RTT réel** mesuré par PING/PONG |
| Précision | `toInt()` tronquait les mouvements lents (< 1 px perdu) | accumulation **sub-pixel** + courbe d'accélération progressive |
| Bluetooth | `sendReport` sur le thread UI, deltas > 127 écrêtés | thread dédié, fusion, découpage des grands deltas, défilement horizontal (AC Pan) |
| Interface | recomposition de toute l'app à chaque mouvement du doigt | seul le calque du réticule est redessiné |
| Gestes | 1 doigt | 1 doigt curseur, tap / tap‑glisser, 2 doigts défilement + clic droit, 3 doigts clic milieu, boutons maintenus |
| Raccourcis | touche inconnue → ESPACE (lecture intempestive) | ignorée ; option AZERTY pour le Bluetooth |
| Hôte | `pyautogui` (lent, PAUSE, pas de maintien) | `SendInput` natif / uinput / pynput, maintien des boutons, sécurité par clé |
| Reconnexion | manuelle (saisie d'IP) | automatique, repli sur plusieurs IP, ré-authentification si le PC redémarre |
