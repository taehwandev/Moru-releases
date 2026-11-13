package app.thdev.moru.app

import androidx.compose.runtime.staticCompositionLocalOf

/** The results the user marked seen in the Moru view, and how to mark another, kept with the workbench layout. */
internal data class SeenResults(val seen: Set<Int>, val markSeen: (Int) -> Unit)

internal val LocalSeenResults = staticCompositionLocalOf { SeenResults(emptySet()) {} }

/** Only the newest are kept: an old result is long gone from the view, and the layout file stays small. */
internal fun List<Int>.plusSeen(key: Int): List<Int> = (filter { it != key } + key).takeLast(MaxSeenResults)

private const val MaxSeenResults = 200
