package app.thdev.moru.core.terminal

import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** How often session ages ("working", "quiet", "2m ago") are re-read; output does not move the session list. */
const val SESSION_CLOCK_TICK_MILLIS: Long = 2_000L

/**
 * The one wall clock every session view ages its sessions by. The sidebar, the Task rows and the attention alerts
 * each ran their own timer; they now share this, which ticks only while something is watching it.
 */
object SessionClock {
    val nowMillis: StateFlow<Long> = flow {
        while (true) {
            SessionShellProcesses.refreshProgramObservations()
            emit(Clock.System.now().toEpochMilliseconds())
            delay(SESSION_CLOCK_TICK_MILLIS)
        }
    }.stateIn(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(),
        initialValue = Clock.System.now().toEpochMilliseconds(),
    )
}
