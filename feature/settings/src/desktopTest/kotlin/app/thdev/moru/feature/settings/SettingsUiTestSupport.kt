package app.thdev.moru.feature.settings

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.runtime.RuntimeStatusRepository
import app.thdev.moru.core.runtime.StaticRuntimeStatusRepository
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

internal class UiSettingsRepository : SettingsRepository {
    @Volatile var stored = AppSettings()
    @Volatile var failWrites = false
    @Volatile var beforeWrite: (() -> Unit)? = null
    val saveCount = AtomicInteger()
    val attemptCount = AtomicInteger()
    override fun loadSettings() = stored
    override fun saveSettings(settings: AppSettings) {
        attemptCount.incrementAndGet()
        beforeWrite?.invoke()
        if (failWrites) error("Disk unavailable")
        stored = settings
        saveCount.incrementAndGet()
    }
}

internal val EmptyRuntimeStatus: RuntimeStatusRepository = StaticRuntimeStatusRepository

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitSettingsText(text: String) {
    waitUntil(timeoutMillis = 10_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    waitForIdle()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.captureSettings(name: String, tag: String? = null) {
    val target = File("build/ui-captures/$name.png")
    target.parentFile.mkdirs()
    val node = if (tag == null) onRoot() else onNodeWithTag(tag)
    ImageIO.write(node.captureToImage().toAwtImage(), "png", target)
}
