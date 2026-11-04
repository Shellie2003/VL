package com.example.cutpilot.data

import com.example.cutpilot.model.ModifierKey
import com.example.cutpilot.model.ModifierKey.ALT
import com.example.cutpilot.model.ModifierKey.CMD
import com.example.cutpilot.model.ModifierKey.CTRL
import com.example.cutpilot.model.ModifierKey.SHIFT

/**
 * A ready-made shortcut. [key] is a semantic symbol understood by
 * HidConstants.symbolToHidCode ("" = modifiers only, e.g. the Windows key alone).
 */
data class LibraryShortcut(
    val label: String,
    val modifiers: Set<ModifierKey>,
    val key: String
) {
    val combo: String
        get() {
            val parts = modifiers.sortedBy { it.ordinal }.map {
                when (it) {
                    CTRL -> "Ctrl"
                    SHIFT -> "Maj"
                    ALT -> "Alt"
                    CMD -> "Win"
                }
            } + listOfNotNull(keyLabel(key))
            return parts.joinToString(" + ")
        }

    private fun keyLabel(k: String): String? = when (k.uppercase()) {
        "" -> null
        "GAUCHE" -> "←"
        "DROITE" -> "→"
        "HAUT" -> "↑"
        "BAS" -> "↓"
        "ESPACE" -> "Espace"
        "ENTRER" -> "Entrée"
        "DEL" -> "Suppr"
        "BACKSPACE" -> "⌫"
        "ESC" -> "Échap"
        "PRINTSCREEN" -> "Impr. écran"
        "PAGEUP" -> "Pg↑"
        "PAGEDOWN" -> "Pg↓"
        "HOME" -> "Début"
        "END" -> "Fin"
        else -> k
    }
}

data class ShortcutCategory(val name: String, val items: List<LibraryShortcut>)

object ShortcutLibrary {
    private fun s(label: String, key: String, vararg mods: ModifierKey) = LibraryShortcut(label, mods.toSet(), key)

    val categories: List<ShortcutCategory> = listOf(
        ShortcutCategory(
            "Édition", listOf(
                s("Copier", "C", CTRL), s("Coller", "V", CTRL), s("Couper", "X", CTRL),
                s("Annuler", "Z", CTRL), s("Rétablir", "Y", CTRL), s("Rétablir (alt.)", "Z", CTRL, SHIFT),
                s("Tout sélectionner", "A", CTRL), s("Enregistrer", "S", CTRL),
                s("Enregistrer sous", "S", CTRL, SHIFT), s("Rechercher", "F", CTRL),
                s("Remplacer", "H", CTRL), s("Imprimer", "P", CTRL), s("Nouveau", "N", CTRL),
                s("Ouvrir", "O", CTRL), s("Coller sans format", "V", CTRL, SHIFT),
                s("Supprimer mot précédent", "BACKSPACE", CTRL), s("Supprimer mot suivant", "DEL", CTRL),
                s("Mot précédent", "GAUCHE", CTRL), s("Mot suivant", "DROITE", CTRL),
                s("Sélection mot ←", "GAUCHE", CTRL, SHIFT), s("Sélection mot →", "DROITE", CTRL, SHIFT),
                s("Début de ligne", "HOME"), s("Fin de ligne", "END"),
                s("Sélection → début", "HOME", SHIFT), s("Sélection → fin", "END", SHIFT),
                s("Début du document", "HOME", CTRL), s("Fin du document", "END", CTRL)
            )
        ),
        ShortcutCategory(
            "Texte", listOf(
                s("Gras", "B", CTRL), s("Italique", "I", CTRL), s("Souligné", "U", CTRL),
                s("Insérer un lien", "K", CTRL), s("Aligner à gauche", "L", CTRL),
                s("Centrer", "E", CTRL), s("Aligner à droite", "R", CTRL), s("Justifier", "J", CTRL),
                s("Agrandir police", ".", CTRL, SHIFT), s("Réduire police", ",", CTRL, SHIFT),
                s("Majuscules / minuscules", "F3", SHIFT), s("Saut de page", "ENTRER", CTRL)
            )
        ),
        ShortcutCategory(
            "Windows", listOf(
                s("Menu Démarrer", "", CMD), s("Bureau", "D", CMD), s("Explorateur", "E", CMD),
                s("Verrouiller", "L", CMD), s("Paramètres", "I", CMD), s("Exécuter", "R", CMD),
                s("Rechercher", "S", CMD), s("Menu Win+X", "X", CMD),
                s("Capture zone", "S", CMD, SHIFT), s("Capture plein écran", "PRINTSCREEN", CMD),
                s("Impr. écran", "PRINTSCREEN"), s("Presse-papiers", "V", CMD),
                s("Émojis", ".", CMD), s("Gestionnaire des tâches", "ESC", CTRL, SHIFT),
                s("Changer de fenêtre", "TAB", ALT), s("Vue des tâches", "TAB", CMD),
                s("Fermer la fenêtre", "F4", ALT), s("Agrandir", "HAUT", CMD), s("Réduire", "BAS", CMD),
                s("Ancrer à gauche", "GAUCHE", CMD), s("Ancrer à droite", "DROITE", CMD),
                s("Fenêtre → autre écran", "GAUCHE", CMD, SHIFT), s("Tout réduire", "M", CMD),
                s("Nouveau bureau virtuel", "D", CMD, CTRL), s("Bureau virtuel ←", "GAUCHE", CMD, CTRL),
                s("Bureau virtuel →", "DROITE", CMD, CTRL), s("Fermer bureau virtuel", "F4", CMD, CTRL),
                s("Game Bar", "G", CMD), s("Enregistrer l'écran", "R", CMD, ALT),
                s("Projeter (écrans)", "P", CMD), s("Notifications", "N", CMD),
                s("Paramètres rapides", "A", CMD), s("Dictée vocale", "H", CMD),
                s("Loupe +", "+", CMD), s("Loupe −", "-", CMD), s("Propriétés système", "PAUSE", CMD),
                s("Menu contextuel", "F10", SHIFT)
            )
        ),
        ShortcutCategory(
            "Explorateur", listOf(
                s("Renommer", "F2"), s("Nouveau dossier", "N", CTRL, SHIFT), s("Propriétés", "ENTRER", ALT),
                s("Dossier parent", "HAUT", ALT), s("Précédent", "GAUCHE", ALT), s("Suivant", "DROITE", ALT),
                s("Barre d'adresse", "D", ALT), s("Supprimer définitivement", "DEL", SHIFT),
                s("Nouvelle fenêtre", "N", CTRL), s("Actualiser", "F5"), s("Volet d'aperçu", "P", ALT)
            )
        ),
        ShortcutCategory(
            "Navigateur", listOf(
                s("Nouvel onglet", "T", CTRL), s("Fermer l'onglet", "W", CTRL),
                s("Rouvrir l'onglet fermé", "T", CTRL, SHIFT), s("Onglet suivant", "TAB", CTRL),
                s("Onglet précédent", "TAB", CTRL, SHIFT), s("Barre d'adresse", "L", CTRL),
                s("Actualiser", "F5"), s("Actualiser sans cache", "F5", CTRL),
                s("Page précédente", "GAUCHE", ALT), s("Page suivante", "DROITE", ALT),
                s("Favori", "D", CTRL), s("Historique", "H", CTRL), s("Téléchargements", "J", CTRL),
                s("Navigation privée", "N", CTRL, SHIFT), s("Nouvelle fenêtre", "N", CTRL),
                s("Zoom +", "+", CTRL), s("Zoom −", "-", CTRL), s("Zoom 100 %", "0", CTRL),
                s("Plein écran", "F11"), s("Outils de développement", "F12"),
                s("Rechercher dans la page", "F", CTRL), s("Haut de page", "HOME"), s("Bas de page", "END"),
                s("Page suivante (défil.)", "ESPACE"), s("Page précédente (défil.)", "ESPACE", SHIFT)
            )
        ),
        ShortcutCategory(
            "Bureautique", listOf(
                s("Nouvelle diapo", "M", CTRL), s("Lancer diaporama", "F5"),
                s("Diaporama depuis ici", "F5", SHIFT), s("Grouper", "G", CTRL),
                s("Dissocier", "G", CTRL, SHIFT), s("Dupliquer", "D", CTRL),
                s("Excel : modifier cellule", "F2"), s("Excel : somme auto", "=", ALT),
                s("Excel : nouvelle feuille", "F11", SHIFT), s("Excel : date du jour", ";", CTRL),
                s("Excel : insérer ligne", "+", CTRL, SHIFT), s("Excel : filtre", "L", CTRL, SHIFT),
                s("Commentaire", "M", CTRL, ALT)
            )
        ),
        ShortcutCategory(
            "Présentation", listOf(
                s("Diapo suivante", "DROITE"), s("Diapo précédente", "GAUCHE"),
                s("Écran noir", "B"), s("Écran blanc", "W"), s("Quitter", "ESC"),
                s("Première diapo", "HOME"), s("Dernière diapo", "END"), s("Pointeur laser", "L", CTRL)
            )
        ),
        ShortcutCategory(
            "macOS", listOf(
                s("Spotlight", "ESPACE", CMD), s("Changer d'app", "TAB", CMD), s("Quitter l'app", "Q", CMD),
                s("Fermer fenêtre", "W", CMD), s("Copier", "C", CMD), s("Coller", "V", CMD),
                s("Couper", "X", CMD), s("Annuler", "Z", CMD), s("Rétablir", "Z", CMD, SHIFT),
                s("Tout sélectionner", "A", CMD), s("Enregistrer", "S", CMD),
                s("Capture écran", "3", CMD, SHIFT), s("Capture zone", "4", CMD, SHIFT),
                s("Outil capture", "5", CMD, SHIFT), s("Forcer à quitter", "ESC", CMD, ALT),
                s("Verrouiller", "Q", CMD, CTRL), s("Préférences", ",", CMD),
                s("Masquer l'app", "H", CMD), s("Nouvel onglet", "T", CMD), s("Plein écran", "F", CMD, CTRL)
            )
        ),
        ShortcutCategory(
            "Touches seules", listOf(
                s("Échap", "ESC"), s("Entrée", "ENTRER"), s("Tab", "TAB"), s("Espace", "ESPACE"),
                s("Suppr", "DEL"), s("Retour arrière", "BACKSPACE"), s("Verr. Maj", "CAPSLOCK"),
                s("Menu", "MENU"), s("Inser", "INSERT")
            ) + (1..12).map { s("F$it", "F$it") }
        )
    )

    val all: List<LibraryShortcut> by lazy { categories.flatMap { it.items } }
}
