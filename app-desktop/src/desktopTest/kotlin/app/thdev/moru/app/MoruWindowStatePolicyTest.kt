package app.thdev.moru.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.WindowSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoruWindowStatePolicyTest {
    @Test
    fun centersWindowWhenNoRememberedCoordinatesExist() {
        val position = MoruWindowStatePolicy.initialPosition(
            window = WindowSettings(xDp = null, yDp = null),
            rememberLastState = true,
        )

        assertTrue(position is WindowPosition.Aligned)
        assertEquals(Alignment.Center, position.alignment)
    }

    @Test
    fun restoresRememberedCoordinatesWhenBothExist() {
        val position = MoruWindowStatePolicy.initialPosition(
            window = WindowSettings(xDp = 120, yDp = 80),
            rememberLastState = true,
        )

        assertTrue(position.isSpecified)
        assertEquals(120, position.x.value.toInt())
        assertEquals(80, position.y.value.toInt())
    }

    @Test
    fun initialSizeUsesSafeMinimums() {
        val size = MoruWindowStatePolicy.initialSize(
            window = WindowSettings(widthDp = 120, heightDp = 80),
            rememberLastState = true,
        )

        assertEquals(960, size.width.value.toInt())
        assertEquals(640, size.height.value.toInt())
    }

    @Test
    fun windowPersistencePreservesTheLatestSavedPreferences() {
        var stored = AppSettings().let { it.copy(appearance = it.appearance.copy(theme = AppTheme.TokyoNight)) }
        val repository = object : SettingsRepository {
            override fun loadSettings() = stored
            override fun saveSettings(settings: AppSettings) { stored = settings }
        }
        MoruWindowStatePolicy.persist(repository, WindowState(size = DpSize(1800.dp, 1000.dp)))
        assertEquals(AppTheme.TokyoNight, stored.appearance.theme)
        assertEquals(1800, stored.window.widthDp)
    }

    @Test
    fun disabledWindowRetentionDoesNotWrite() {
        val settings = AppSettings().let { it.copy(general = it.general.copy(rememberLastState = false)) }
        var writes = 0
        val repository = object : SettingsRepository {
            override fun loadSettings() = settings
            override fun saveSettings(settings: AppSettings) { writes++ }
        }
        MoruWindowStatePolicy.persist(repository, WindowState())
        assertEquals(0, writes)
    }
}
