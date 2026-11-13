package app.thdev.moru.core.designsystem.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.Window

/** Bounded initialization; disposal cancels setup and removes the exact material this composition attached. */
@Composable
internal fun rememberMacBackdrop(window: Window?, role: Int, dark: Int = -1): Boolean {
    var native by remember(window, role, dark) { mutableStateOf(false) }
    val owner = remember(window, role, dark) { arrayOfNulls<MacBackdropLease>(1) }
    DisposableEffect(owner) {
        onDispose { owner[0]?.close(); owner[0] = null }
    }
    LaunchedEffect(window, role, dark) {
        if (window == null) return@LaunchedEffect
        val api = withContext(Dispatchers.IO) { MacBackdropBridge.api } ?: return@LaunchedEffect
        val lease = MacBackdropRegistry.acquire(window, api, role, dark)
        val controller = lease.controller
        owner[0] = lease
        repeat(90) {
            controller.attach()
            val kind = controller.kind()
            if (kind > 0) {
                native = true
                return@LaunchedEffect
            }
            if (kind < 0) {
                lease.close()
                return@LaunchedEffect
            }
            delay(16)
        }
        lease.close()
    }
    return native
}
