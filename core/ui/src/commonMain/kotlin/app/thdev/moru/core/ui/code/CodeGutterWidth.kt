package app.thdev.moru.core.ui.code

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The gutter's width for [lineCount] lines: room for the largest number. */
fun codeGutterWidth(lineCount: Int): Dp = (lineCount.toString().length * 9 + 4).dp
