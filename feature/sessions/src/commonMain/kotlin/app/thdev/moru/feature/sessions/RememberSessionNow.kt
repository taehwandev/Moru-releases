package app.thdev.moru.feature.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import app.thdev.moru.core.terminal.SESSION_CLOCK_TICK_MILLIS
import app.thdev.moru.core.terminal.SessionClock
/** The current time by [SessionClock], advancing every [SESSION_CLOCK_TICK_MILLIS] while composed. */
@Composable
fun rememberSessionNow(): Long {
    val now by SessionClock.nowMillis.collectAsState()
    return now
}
