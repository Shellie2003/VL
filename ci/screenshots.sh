#!/usr/bin/env bash
# Parcourt l'app sur l'émulateur et capture chaque écran principal (revue du design).
set -u
mkdir -p shots

tap_attr() { # $1 = attribut (text|content-desc), $2 = valeur
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml > /dev/null 2>&1
  local b
  b=$(grep -o "$1=\"$2\"[^>]*bounds=\"[^\"]*\"" ui.xml | head -1 \
      | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/')
  if [ -z "$b" ]; then echo "introuvable : $1=$2"; return 1; fi
  set -- $b
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

# Fenêtres système de l'émulateur (« … isn't responding ») : masquées / fermées.
adb shell settings put global hide_error_dialogs 1 > /dev/null 2>&1
dismiss_system_dialogs() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml > /dev/null 2>&1
  if grep -q "responding\|ne répond pas" ui.xml; then
    tap_attr text "Wait" || tap_attr text "Close app"
    sleep 1
  fi
}

shot() { sleep 2.5; dismiss_system_dialogs; adb exec-out screencap -p > "shots/$1.png"; echo "capture $1"; }

open_menu_item() { tap_attr content-desc "Menu" && sleep 1 && tap_attr text "$1"; }

sleep 8
dismiss_system_dialogs
shot 1-connexion
adb shell input keyevent KEYCODE_BACK
shot 2-trackpad
tap_attr content-desc "Menu"
shot 3-menu
tap_attr text "Clavier"
shot 4-clavier
tap_attr text "Mode paysage"
sleep 3
tap_attr text "Got it" || tap_attr text "OK"
sleep 1
shot 4b-clavier-paysage
tap_attr content-desc "Quitter le mode paysage"
sleep 3
open_menu_item "Molette Jog"
shot 5-jog
open_menu_item "Deck de montage"
shot 6-deck
open_menu_item "Réglages"
shot 7-reglages
ls -la shots
