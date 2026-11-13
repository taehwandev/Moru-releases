package app.thdev.moru.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The notification panel's width and height in a window of [windowWidth] by [windowHeight]: as wide as the history
 * reads well, never wider than the window leaves room for, and as tall as the room under the title bar allows, so it
 * always fits inside the window with [InboxEdgeGap] to spare.
 */
internal fun notificationInboxSize(titleRow: Dp, windowWidth: Dp, windowHeight: Dp): Pair<Dp, Dp> {
    val width = minOf(PreferredWidth, windowWidth - InboxEdgeGap * 2).coerceAtLeast(0.dp)
    val height = minOf(MaxHeight, windowHeight - titleRow - InboxEdgeGap * 2).coerceAtLeast(0.dp)
    return width to height
}

/** The space kept between the panel and the window's right and bottom edges. */
internal val InboxEdgeGap = 18.dp

private val PreferredWidth = 380.dp
private val MaxHeight = 640.dp
