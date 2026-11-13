package app.thdev.moru.core.ui.shortcut

import app.thdev.moru.core.model.ShortcutChord

/** How [this] chord reads in a menu: `⌥⇧⌘K` on macOS, `Ctrl+Alt+Shift+K` elsewhere. */
fun ShortcutChord.label(macOs: Boolean = ShortcutKeys.macOs): String =
    if (macOs) {
        (if (alt) "⌥" else "") + (if (shift) "⇧" else "") + "⌘" + key
    } else {
        "Ctrl+" + (if (alt) "Alt+" else "") + (if (shift) "Shift+" else "") + key
    }
