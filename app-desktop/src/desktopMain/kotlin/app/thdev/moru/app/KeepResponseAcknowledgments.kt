package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thdev.moru.core.terminal.SessionResponseAttention
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Saves the seen responses whenever they change: seen in Moru, a new turn, or a closed session. */
@Composable
internal fun KeepResponseAcknowledgments(store: ResponseAcknowledgments) {
    LaunchedEffect(store) {
        SessionResponseAttention.revision.collect {
            val seen = SessionResponseAttention.acknowledgedSessions()
            withContext(Dispatchers.IO) { store.write(seen) }
        }
    }
}
