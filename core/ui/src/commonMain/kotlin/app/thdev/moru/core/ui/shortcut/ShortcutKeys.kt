package app.thdev.moru.core.ui.shortcut

import androidx.compose.ui.input.key.Key
import app.thdev.moru.core.model.ShortcutChord

/** The keys a shortcut can use, by the name a [ShortcutChord] stores. */
object ShortcutKeys {
    /** Whether shortcuts use Command (macOS) rather than Control. */
    val macOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

    private val byName: Map<String, Key> = buildMap {
        listOf(
            Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
            Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
        ).forEachIndexed { index, key -> put(('A' + index).toString(), key) }
        listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
            .forEachIndexed { index, key -> put(index.toString(), key) }
        put(",", Key.Comma)
        put(".", Key.Period)
        put("/", Key.Slash)
        put(";", Key.Semicolon)
        put("'", Key.Apostrophe)
        put("[", Key.LeftBracket)
        put("]", Key.RightBracket)
        put("\\", Key.Backslash)
        put("-", Key.Minus)
        put("=", Key.Equals)
        put("`", Key.Grave)
        listOf(Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
            .forEachIndexed { index, key -> put("F${index + 1}", key) }
    }
    private val byKey: Map<Key, String> = byName.entries.associate { (name, key) -> key to name }

    /** The key [name] stands for, or null when no shortcut can use it. */
    fun key(name: String): Key? = byName[name]

    /** The name a shortcut stores for [key], or null for a key no shortcut can use, such as a modifier alone. */
    fun name(key: Key): String? = byKey[key]
}
