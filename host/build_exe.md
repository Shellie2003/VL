# Construire un exécutable autonome

Pour distribuer Vôlavo Hôte sans demander d'installer Python :

```bash
pip install pyinstaller -r requirements.txt
pyinstaller --onefile --windowed --name VolavoHote volavo_host.py
```

L'exécutable se trouve dans `dist/` (`VolavoHote.exe` sous Windows, `VolavoHote` sous macOS/Linux).
