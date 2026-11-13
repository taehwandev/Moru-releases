package app.thdev.moru.core.designsystem.theme

import java.awt.GraphicsEnvironment
import java.util.Locale

actual fun installedSystemFontFamilies(): List<String> = GraphicsEnvironment
    .getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)
    .distinct().sortedBy { it.lowercase(Locale.ROOT) }
