package app.thdev.moru.feature.git

import java.text.NumberFormat

internal actual fun formatGitCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)
