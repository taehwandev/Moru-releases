package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Local receipt identities are never reconstructed from durable notification history. */
internal class NotificationPermissionBindings {
    data class Binding(val sessionId: String, val requestId: String)

    private var bindings by mutableStateOf<Map<String, Binding>>(emptyMap())

    fun capture(receiptId: String, sessionId: String?, requestId: String?) {
        val updated = bindings - receiptId
        bindings = if (sessionId == null || requestId == null) updated else
            (updated + (receiptId to Binding(sessionId, requestId))).entries
                .toList().takeLast(MAX_BINDINGS).associate { it.key to it.value }
    }

    fun resolve(receiptId: String, taskAvailable: Boolean, currentRequestId: (String) -> String?): Binding? {
        if (!taskAvailable) return null
        return bindings[receiptId]?.takeIf { currentRequestId(it.sessionId) == it.requestId }
    }

    fun remove(receiptId: String) { bindings = bindings - receiptId }
    fun clear() { bindings = emptyMap() }

    companion object {
        private const val MAX_BINDINGS = 500
        val Current = NotificationPermissionBindings()
    }
}
