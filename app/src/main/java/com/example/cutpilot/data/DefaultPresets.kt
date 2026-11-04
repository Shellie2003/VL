package com.example.cutpilot.data

import com.example.cutpilot.model.ModifierKey
import com.example.cutpilot.model.ShortcutKey
import com.example.cutpilot.model.SoftwareProfile

object DefaultPresets {

    val canvaStudio = SoftwareProfile(
        id = "canva_studio",
        name = "Canva Design & Vidéo",
        category = "Design & Montage",
        description = "Outils d'édition vidéo, scission de scènes, création de formes, calques et alignement Canva",
        shortcuts = listOf(
            ShortcutKey("canva_split", "Scinder Vidéo (S)", "Vidéo", "S", colorHex = 0xFFFF4D4D, description = "Scinde le clip ou la scène vidéo active sur Canva"),
            ShortcutKey("canva_play", "Lecture / Pause", "Vidéo", "ESPACE", colorHex = 0xFF00E676, description = "Démarre ou arrête la prévisualisation vidéo Canva"),
            ShortcutKey("canva_dup_scene", "Dupliquer Scène", "Scènes", "D", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF00E5FF, description = "Duplique la page ou la scène vidéo"),
            ShortcutKey("canva_add_scene", "Ajouter Scène", "Scènes", "ENTRER", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF00E5FF, description = "Insère une nouvelle page / scène vierge"),
            ShortcutKey("canva_text", "Ajouter Texte (T)", "Éléments", "T", colorHex = 0xFFFFB300, description = "Crée instantanément une zone de texte"),
            ShortcutKey("canva_rect", "Rectangle (R)", "Éléments", "R", colorHex = 0xFFFFB300, description = "Insère un rectangle sur le canevas"),
            ShortcutKey("canva_circle", "Cercle (C)", "Éléments", "C", colorHex = 0xFFFFB300, description = "Insère un cercle"),
            ShortcutKey("canva_line", "Ligne / Trait (L)", "Éléments", "L", colorHex = 0xFFFFB300, description = "Insère une ligne vectorielle"),
            ShortcutKey("canva_group", "Grouper", "Calques", "G", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFA855F7, description = "Groupe les éléments sélectionnés"),
            ShortcutKey("canva_ungroup", "Dissocier", "Calques", "G", modifiers = setOf(ModifierKey.CTRL, ModifierKey.SHIFT), colorHex = 0xFFA855F7, description = "Dissocie le groupe"),
            ShortcutKey("canva_forward", "Avancer calque", "Calques", "]", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF3B82F6, description = "Monte d'un niveau dans les calques"),
            ShortcutKey("canva_backward", "Reculer calque", "Calques", "[", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF3B82F6, description = "Descend d'un niveau dans les calques"),
            ShortcutKey("canva_front", "Premier Plan", "Calques", "]", modifiers = setOf(ModifierKey.CTRL, ModifierKey.SHIFT), colorHex = 0xFF3B82F6, description = "Passe tout au premier plan"),
            ShortcutKey("canva_back", "Arrière Plan", "Calques", "[", modifiers = setOf(ModifierKey.CTRL, ModifierKey.SHIFT), colorHex = 0xFF3B82F6, description = "Passe tout en arrière-plan"),
            ShortcutKey("canva_lock", "Verrouiller", "Outils", "L", modifiers = setOf(ModifierKey.ALT, ModifierKey.SHIFT), colorHex = 0xFF78909C, description = "Verrouille la position de l'élément"),
            ShortcutKey("canva_fit", "Ajuster 100%", "Zoom", "0", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFFEB3B, description = "Ajuste le zoom à 100% de la fenêtre"),
            ShortcutKey("canva_zoom_in", "Zoom +", "Zoom", "+", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFFEB3B, description = "Zoom avant"),
            ShortcutKey("canva_zoom_out", "Zoom -", "Zoom", "-", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFFEB3B, description = "Zoom arrière"),
            ShortcutKey("canva_present", "Plein Écran", "Affichage", "P", modifiers = setOf(ModifierKey.CTRL, ModifierKey.ALT), colorHex = 0xFF9C27B0, description = "Mode Présentation plein écran"),
            ShortcutKey("canva_export", "Télécharger", "Projet", "E", modifiers = setOf(ModifierKey.CTRL, ModifierKey.SHIFT), colorHex = 0xFFE91E63, description = "Ouvre le menu de téléchargement / partage"),
            ShortcutKey("canva_del", "Supprimer", "Édition", "DEL", colorHex = 0xFFFF4D4D, description = "Supprime l'élément ou la scène"),
            ShortcutKey("canva_undo", "Annuler", "Édition", "Z", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF78909C, description = "Annule la dernière modification")
        )
    )

    val premierePro = SoftwareProfile(
        id = "premiere_pro",
        name = "Adobe Premiere Pro",
        category = "Montage Pro",
        description = "Raccourcis indispensables de la timeline et outils de découpe",
        shortcuts = listOf(
            ShortcutKey("pr_cut", "Cutter (Blade)", "Découpe", "C", colorHex = 0xFFFF4D4D, description = "Sélectionne l'outil Cutter"),
            ShortcutKey("pr_split", "Scinder tête", "Découpe", "K", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFF4D4D, description = "Scinde au curseur de lecture"),
            ShortcutKey("pr_select", "Sélection (V)", "Outils", "V", colorHex = 0xFF00E5FF, description = "Outil flèche standard"),
            ShortcutKey("pr_ripple_del", "Suppr. Raccord", "Édition", "DEL", modifiers = setOf(ModifierKey.SHIFT), colorHex = 0xFFFF9800, description = "Supprime et comble le vide"),
            ShortcutKey("pr_ripple_edit", "Raccord (B)", "Outils", "B", colorHex = 0xFF00E5FF, description = "Ripple Edit Tool"),
            ShortcutKey("pr_slip", "Glissement (Y)", "Outils", "Y", colorHex = 0xFF00E5FF, description = "Slip Tool"),
            ShortcutKey("pr_mark_in", "Point Entrée", "Marqueurs", "I", colorHex = 0xFF4CAF50, description = "Place le point IN"),
            ShortcutKey("pr_mark_out", "Point Sortie", "Marqueurs", "O", colorHex = 0xFF4CAF50, description = "Place le point OUT"),
            ShortcutKey("pr_marker", "Ajouter Repère", "Marqueurs", "M", colorHex = 0xFF2196F3, description = "Pose un marqueur temporel"),
            ShortcutKey("pr_play", "Lecture/Pause", "Navigation", "ESPACE", colorHex = 0xFF00E676, description = "Démarrer ou stopper la lecture"),
            ShortcutKey("pr_snap", "Magnétisme (S)", "Édition", "S", colorHex = 0xFF9C27B0, description = "Active / désactive le magnétisme"),
            ShortcutKey("pr_zoom_in", "Zoom + Timeline", "Zoom", "+", colorHex = 0xFFFFEB3B, description = "Agrandir l'échelle temporelle"),
            ShortcutKey("pr_zoom_out", "Zoom - Timeline", "Zoom", "-", colorHex = 0xFFFFEB3B, description = "Réduire l'échelle temporelle"),
            ShortcutKey("pr_fullscreen", "Plein Écran", "Affichage", "~", colorHex = 0xFF607D8B, description = "Agrandit le panneau sous la souris"),
            ShortcutKey("pr_export", "Exporter Média", "Projet", "M", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFE91E63, description = "Ouvre la boîte d'exportation"),
            ShortcutKey("pr_undo", "Annuler", "Édition", "Z", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF78909C, description = "Annule la dernière action")
        )
    )

    val davinciResolve = SoftwareProfile(
        id = "davinci_resolve",
        name = "DaVinci Resolve",
        category = "Montage & Étalonnage",
        description = "Contrôles ultra-rapides pour Cut page, Edit page et Color",
        shortcuts = listOf(
            ShortcutKey("dv_blade", "Lame de rasoir", "Découpe", "B", colorHex = 0xFFFF4D4D, description = "Active l'outil Blade"),
            ShortcutKey("dv_split", "Scinder plan", "Découpe", "\\", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFF4D4D, description = "Coupe le clip actif"),
            ShortcutKey("dv_arrow", "Sélection (A)", "Outils", "A", colorHex = 0xFF00E5FF, description = "Mode sélection standard"),
            ShortcutKey("dv_trim", "Mode Raccord", "Outils", "T", colorHex = 0xFF00E5FF, description = "Trim Edit Mode"),
            ShortcutKey("dv_ripple_cut", "Ripple Cut", "Édition", "DEL", colorHex = 0xFFFF9800, description = "Supprime avec raccord"),
            ShortcutKey("dv_mark_in", "Point IN", "Marqueurs", "I", colorHex = 0xFF4CAF50, description = "Point d'entrée"),
            ShortcutKey("dv_mark_out", "Point OUT", "Marqueurs", "O", colorHex = 0xFF4CAF50, description = "Point de sortie"),
            ShortcutKey("dv_prev_edit", "Cut Précédent", "Navigation", "HAUT", colorHex = 0xFF2196F3, description = "Aller au point de montage précédent"),
            ShortcutKey("dv_next_edit", "Cut Suivant", "Navigation", "BAS", colorHex = 0xFF2196F3, description = "Aller au point de montage suivant"),
            ShortcutKey("dv_play", "Play/Pause", "Navigation", "ESPACE", colorHex = 0xFF00E676, description = "Lecture"),
            ShortcutKey("dv_cinema", "Cinéma Mode", "Affichage", "F", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF9C27B0, description = "Plein écran cinéma"),
            ShortcutKey("dv_bypass_color", "Bypass Étalon.", "Couleur", "D", modifiers = setOf(ModifierKey.SHIFT), colorHex = 0xFFE91E63, description = "Active/désactive les nœuds couleur")
        )
    )

    val finalCutPro = SoftwareProfile(
        id = "final_cut_pro",
        name = "Final Cut Pro",
        category = "Montage Mac",
        description = "Raccourcis magnétiques et outils de précision FCP",
        shortcuts = listOf(
            ShortcutKey("fcp_blade", "Lame (Blade)", "Découpe", "B", modifiers = setOf(ModifierKey.CMD), colorHex = 0xFFFF4D4D, description = "Coupe à la position du survol"),
            ShortcutKey("fcp_select", "Sélection (A)", "Outils", "A", colorHex = 0xFF00E5FF, description = "Flèche de sélection"),
            ShortcutKey("fcp_trim", "Ajustement (T)", "Outils", "T", colorHex = 0xFF00E5FF, description = "Outil trim"),
            ShortcutKey("fcp_range", "Intervalle (R)", "Outils", "R", colorHex = 0xFF00E5FF, description = "Sélection de plage"),
            ShortcutKey("fcp_ripple_del", "Suppr. Raccord", "Édition", "DEL", colorHex = 0xFFFF9800, description = "Supprime en resserrant"),
            ShortcutKey("fcp_in", "Marquer IN", "Marqueurs", "I", colorHex = 0xFF4CAF50, description = "Point d'entrée"),
            ShortcutKey("fcp_out", "Marquer OUT", "Marqueurs", "O", colorHex = 0xFF4CAF50, description = "Point de sortie"),
            ShortcutKey("fcp_fit", "Ajuster zoom", "Zoom", "Z", modifiers = setOf(ModifierKey.SHIFT), colorHex = 0xFFFFEB3B, description = "Ajuste la timeline à l'écran"),
            ShortcutKey("fcp_marker", "Repère (M)", "Marqueurs", "M", colorHex = 0xFF2196F3, description = "Ajoute un repère"),
            ShortcutKey("fcp_snapping", "Magnétisme (N)", "Édition", "N", colorHex = 0xFF9C27B0, description = "Active ou coupe le magnétisme")
        )
    )

    val capcutDesktop = SoftwareProfile(
        id = "capcut",
        name = "CapCut Desktop",
        category = "Réseaux & Shorts",
        description = "Optimisé pour le montage rapide de Reels, TikToks et Shorts",
        shortcuts = listOf(
            ShortcutKey("cc_split", "Diviser (Cut)", "Découpe", "B", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFF4D4D, description = "Divise le clip sélectionné"),
            ShortcutKey("cc_del", "Supprimer", "Édition", "DEL", colorHex = 0xFFFF9800, description = "Efface l'élément actif"),
            ShortcutKey("cc_play", "Lecture/Pause", "Navigation", "ESPACE", colorHex = 0xFF00E676, description = "Play / Pause"),
            ShortcutKey("cc_zoom_in", "Zoom +", "Zoom", "=", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFFEB3B, description = "Zoom avant timeline"),
            ShortcutKey("cc_zoom_out", "Zoom -", "Zoom", "-", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFFFFEB3B, description = "Zoom arrière timeline"),
            ShortcutKey("cc_export", "Exporter", "Projet", "E", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF00E5FF, description = "Générer la vidéo"),
            ShortcutKey("cc_undo", "Annuler", "Édition", "Z", modifiers = setOf(ModifierKey.CTRL), colorHex = 0xFF78909C, description = "Ctrl + Z")
        )
    )

    val afterEffects = SoftwareProfile(
        id = "after_effects",
        name = "Adobe After Effects",
        category = "Motion Design & FX",
        description = "Contrôles d'animation d'images clés et gestion de calques",
        shortcuts = listOf(
            ShortcutKey("ae_split", "Scinder calque", "Calques", "D", modifiers = setOf(ModifierKey.CTRL, ModifierKey.SHIFT), colorHex = 0xFFFF4D4D, description = "Scinde le calque sélectionné"),
            ShortcutKey("ae_preview", "Prévisualisation", "Lecture", "ESPACE", colorHex = 0xFF00E676, description = "RAM Preview"),
            ShortcutKey("ae_key_prev", "Image clé préc.", "Animation", "J", colorHex = 0xFF2196F3, description = "Va à l'image clé précédente"),
            ShortcutKey("ae_key_next", "Image clé suiv.", "Animation", "K", colorHex = 0xFF2196F3, description = "Va à l'image clé suivante"),
            ShortcutKey("ae_in_pt", "Aligner Début", "Calques", "[", colorHex = 0xFF4CAF50, description = "Aligne le début du calque au curseur"),
            ShortcutKey("ae_out_pt", "Aligner Fin", "Calques", "]", colorHex = 0xFF4CAF50, description = "Aligne la fin du calque au curseur"),
            ShortcutKey("ae_ease", "Lissage (F9)", "Animation", "F9", colorHex = 0xFFFFEB3B, description = "Applique l'amorti Easy Ease"),
            ShortcutKey("ae_pan", "Pan Behind (Y)", "Outils", "Y", colorHex = 0xFF00E5FF, description = "Déplace le point d'ancrage")
        )
    )

    val allProfiles = listOf(canvaStudio, premierePro, davinciResolve, finalCutPro, capcutDesktop, afterEffects)
}
