package app.thdev.moru.feature.settings

internal fun String.validSettingsNumber(range: IntRange): Int? =
    takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toIntOrNull()?.takeIf { it in range }
