#!/usr/bin/env sh
# Lance Vôlavo Hôte (crée un environnement virtuel local au premier lancement).
cd "$(dirname "$0")" || exit 1
if [ ! -d .venv ]; then
  python3 -m venv .venv || exit 1
fi
. .venv/bin/activate
pip install --disable-pip-version-check -q -r requirements.txt
exec python volavo_host.py "$@"
