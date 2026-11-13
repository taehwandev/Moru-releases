package app.thdev.moru.core.designsystem.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDesktopPopupHost
import app.thdev.moru.core.designsystem.component.MoruPopover
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.MoruMaterialStyle
import org.jetbrains.skiko.SkiaLayer
import org.junit.Assume.assumeTrue
import java.awt.Component
import java.awt.Container
import java.awt.GraphicsEnvironment
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MacBackdropLifecycleOsTest {
    @Test
    fun sharedHostsAndRepeatedConcurrentPopoversReleaseTheirMaterials() {
        assumeTrue(System.getProperty("os.name").startsWith("Mac") && !GraphicsEnvironment.isHeadless())
        val api = assertNotNull(MacBackdropBridge.api)
        val baseline = api.moru_backdrop_count()
        val firstHost = mutableStateOf(true)
        val secondHost = mutableStateOf(true)
        val firstPopup = mutableStateOf(false)
        val secondPopup = mutableStateOf(false)
        val appliedHosts = AtomicInteger()
        lateinit var window: ComposeWindow
        var created = false
        try {
            onEdt {
                window = ComposeWindow()
                prepareMoruNativeWindow(window)
                created = true
                window.apply {
                    setBounds(100, 100, 700, 420)
                    setContent {
                        MoruTheme(darkTheme = false, materialStyle = MoruMaterialStyle.Apple) {
                            SideEffect { appliedHosts.set((if (firstHost.value) 1 else 0) + (if (secondHost.value) 2 else 0)) }
                            Box(Modifier.fillMaxSize()) {
                                if (firstHost.value) MoruDesktopPopupHost(window) { MoruText("Additional docked host") }
                                if (secondHost.value) MoruDesktopPopupHost(window) {
                                    Box(Modifier.offset(40.dp, 60.dp)) {
                                        MoruPopover(firstPopup.value, { firstPopup.value = false }) {
                                            Box(Modifier.size(220.dp, 140.dp)) { MoruText("First native popup") }
                                        }
                                    }
                                    Box(Modifier.offset(360.dp, 60.dp)) {
                                        MoruPopover(secondPopup.value, { secondPopup.value = false }) {
                                            Box(Modifier.size(220.dp, 140.dp)) { MoruText("Second native popup") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    isVisible = true
                }
            }
            await("two hosts share one native material") { appliedHosts.get() == 3 && api.moru_backdrop_count() == baseline + 1 }
            val layer = onEdt { assertNotNull(findLayer(window)) }
            onEdt { firstHost.value = false }
            await("one host retains shared renderer transparency") {
                appliedHosts.get() == 2 && api.moru_backdrop_count() == baseline + 1 && onEdt { layer.transparency }
            }
            onEdt { secondHost.value = false }
            await("last host releases the material and renderer") {
                appliedHosts.get() == 0 && api.moru_backdrop_count() == baseline && onEdt { !layer.transparency }
            }
            onEdt { firstHost.value = true; secondHost.value = true }
            await("shared hosts reinstall") { appliedHosts.get() == 3 && api.moru_backdrop_count() == baseline + 1 }

            // Popovers paint an opaque theme surface instead of installing their own material (docs/DESIGN_SYSTEM.md),
            // so opening and closing them must leave exactly the hosts' shared material behind.
            repeat(5) { cyclePopup(api, baseline, window, firstPopup) }
            val before = memorySample()
            repeat(30) { cyclePopup(api, baseline, window, firstPopup) }
            val after = memorySample()
            File("build/native-backdrop/lifecycle.txt").apply { parentFile.mkdirs() }.writeText(
                "macOS=${System.getProperty("os.version")}; JVM=${System.getProperty("java.version")}; Compose=1.10.3; " +
                    "mode=desktopTest; warmup=5; measuredCycles=30; before=$before; after=$after; " +
                    "nativeMaterialsAfterEachClose=${baseline + 1}; no release performance comparison\n"
            )

            onEdt { firstPopup.value = true }
            await("first popup") { showingPopups(window) == 1 }
            onEdt { secondPopup.value = true }
            await("concurrent popups") { showingPopups(window) == 2 }
            assertEquals(baseline + 1, api.moru_backdrop_count(), "Concurrent popups must not add native materials")
            onEdt { secondPopup.value = false }
            await("first popup survives second dismissal") { showingPopups(window) == 1 }
            onEdt { firstPopup.value = false }
            await("both popups closed") { showingPopups(window) == 0 }
            assertEquals(baseline + 1, api.moru_backdrop_count(), "Closing popups must keep the hosts' shared material")

            // Cancellation must remove pending tokens too, which an active-view count cannot observe.
            val cancelled = onEdt { api.moru_backdrop_install(layer.windowHandle, 0, 0.0, -1) }
            assertTrue(cancelled > 0)
            api.moru_backdrop_remove(cancelled)
            await("cancelled native token erased") { api.moru_backdrop_kind(cancelled) == -1 }
            assertEquals(baseline + 1, api.moru_backdrop_count())
        } finally {
            onEdt { if (created) window.dispose() }
            await("all native views released after close") { api.moru_backdrop_count() == baseline }
        }
    }

    private fun cyclePopup(api: MacBackdropApi, baseline: Int, window: ComposeWindow, expanded: androidx.compose.runtime.MutableState<Boolean>) {
        onEdt { expanded.value = true }
        await("repeat popup opening") { showingPopups(window) == 1 }
        assertEquals(baseline + 1, api.moru_backdrop_count(), "An open popup must not add a native material")
        onEdt { expanded.value = false }
        await("repeat popup closing") { showingPopups(window) == 0 }
        assertEquals(baseline + 1, api.moru_backdrop_count(), "A closed popup must keep the hosts' shared material")
    }

    private fun showingPopups(window: ComposeWindow): Int = onEdt { window.ownedWindows.count { it.isShowing } }

    private fun memorySample(): String {
        System.gc()
        val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
        val process = ProcessBuilder("/bin/ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).start()
        val rssKiB = process.inputStream.bufferedReader().use { it.readText().trim() }
        assertEquals(0, process.waitFor())
        return "heapBytes=$heap,rssKiB=$rssKiB"
    }

    private fun findLayer(component: Component): SkiaLayer? =
        if (component is SkiaLayer) component else (component as? Container)?.components?.firstNotNullOfOrNull(::findLayer)

    private fun await(description: String, read: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (read()) return
            Thread.sleep(20) // Real AppKit/WindowServer integration cannot use coroutine virtual time.
        }
        error("Timed out waiting for $description")
    }

    private fun <T> onEdt(action: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return action()
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(action) }
        return checkNotNull(result).getOrThrow()
    }
}
