#!/usr/bin/env bash
# Installe l'APK, lance l'activité principale et vérifie que l'app ne plante pas.
set -u
APK="$1"
PKG=com.volavo.mouse

echo "== Installation"
adb install -r -g "$APK" 2>&1 | tee install.log
if ! grep -q "Success" install.log; then
  echo "::error::Échec d'installation : $(cat install.log)"
  exit 1
fi

echo "== Lancement"
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 12

PID=$(adb shell pidof "$PKG" | tr -d '\r')
adb logcat -d -b crash > crash.log
adb logcat -d | grep -E "AndroidRuntime|FATAL|$PKG" | tail -80
if [ -s crash.log ] || [ -z "$PID" ]; then
  echo "::error::L'application a planté au démarrage"
  cat crash.log
  exit 1
fi
echo "OK : installée et en cours d'exécution (pid $PID)"
