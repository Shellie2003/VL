# Vôlavo Hôte

Petit logiciel compagnon pour l'ordinateur. Il affiche un **QR code** : scannez-le avec
l'application Vôlavo et le téléphone devient instantanément une souris / console de montage.

## Lancer

| Système | Commande |
|---|---|
| Windows | double-clic sur `start_windows.bat` |
| macOS / Linux | `./start_mac_linux.sh` |
| Manuel | `pip install -r requirements.txt` puis `python volavo_host.py` |

Prérequis : Python 3.8+. Le premier lancement installe les dépendances (`qrcode`, et `pynput` / `evdev` hors Windows).

Options utiles :

```
--headless     QR code affiché dans le terminal (serveur, SSH…)
--new-key      génère une nouvelle clé (les téléphones déjà appairés doivent rescanner)
--port N       port UDP (défaut 50505)
--no-bt        ne pas inclure l'adresse Bluetooth du PC dans le QR code
--dry-run      affiche les événements reçus sans bouger la souris (diagnostic)
```

## Appairage

1. Le PC et le téléphone sont sur le même réseau Wi-Fi.
2. Lancez Vôlavo Hôte → une fenêtre affiche le QR code.
3. Dans Vôlavo : onglet **Connexion** → **Scanner le QR code du PC** (avec la plupart des appareils photo,
   le lien `volavo://` ouvre aussi directement l'application).
4. C'est connecté. Aux lancements suivants, la reconnexion est automatique (la clé est conservée dans
   `~/.volavo_host.json`).

Sans caméra : saisissez l'adresse IP et le **code manuel** affichés dans la fenêtre (onglet *Logiciel PC*).

Le QR code contient aussi l'adresse Bluetooth du PC : si le mode **Bluetooth HID** est sélectionné dans
l'application, le téléphone se connecte directement au Bluetooth de l'ordinateur (acceptez l'appairage).

## Sécurité

* Clé aléatoire de 80 bits par ordinateur, transmise uniquement par le QR code.
* Seul le téléphone authentifié peut injecter des événements ; les autres adresses reçoivent un refus.
* « Nouvelle clé » révoque immédiatement tous les téléphones.
* Boutons et touches maintenus sont relâchés automatiquement si le téléphone disparaît (4 s).

## Performances

* Protocole UDP binaire (5 octets par mouvement, contre ~80 en JSON auparavant).
* Windows : injection native `SendInput` (aucune dépendance), structures pré-allouées.
* Linux : `/dev/uinput` (X11 et Wayland), défilement haute résolution.
  Si l'accès est refusé : `sudo usermod -aG input $USER` puis reconnectez-vous, sinon repli automatique sur `pynput`.
* macOS : autorisez le Terminal / Python dans *Réglages › Confidentialité › Accessibilité*.

## Pare-feu

Au premier lancement Windows demande d'autoriser Python sur le réseau : acceptez pour les **réseaux privés**.
Ailleurs, ouvrez le port UDP 50505 si nécessaire.

## Tests

```
python -m unittest discover -s tests -v
```

Voir `build_exe.md` pour produire un exécutable autonome (PyInstaller).
