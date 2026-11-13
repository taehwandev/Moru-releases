package app.thdev.moru.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import app.thdev.moru.core.domain.SettingsRepository
import app.thdev.moru.core.model.WindowSettings
import kotlin.math.roundToInt

internal object MoruWindowStatePolicy {
    fun initialPlacement(
        window: WindowSettings,
        rememberLastState: Boolean,
    ): WindowPlacement {
        return if (rememberLastState && window.maximized) {
            WindowPlacement.Maximized
        } else {
            WindowPlacement.Floating
        }
    }

    fun initialPosition(
        window: WindowSettings,
        rememberLastState: Boolean,
    ): WindowPosition {
        val x = window.xDp
        val y = window.yDp
        return if (rememberLastState && x != null && y != null) {
            WindowPosition(x.dp, y.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        }
    }

    fun initialSize(
        window: WindowSettings,
        rememberLastState: Boolean,
    ): DpSize {
        val source = if (rememberLastState) window else WindowSettings()
        return DpSize(
            width = source.widthDp.coerceIn(MinWindowWidthDp, MaxWindowWidthDp).dp,
            height = source.heightDp.coerceIn(MinWindowHeightDp, MaxWindowHeightDp).dp,
        )
    }

    fun persist(
        settingsRepository: SettingsRepository,
        windowState: WindowState,
    ) {
        runCatching {
            settingsRepository.updateSettings { latestSettings ->
                if (latestSettings.general.rememberLastState) {
                    latestSettings.copy(window = latestSettings.window.updatedFrom(windowState))
                } else latestSettings
            }
        }.onFailure { error ->
            System.err.println("Failed to save Moru window state: ${error.message}")
        }
    }

    private fun WindowSettings.updatedFrom(windowState: WindowState): WindowSettings {
        val position = windowState.position
        val size = windowState.size
        return copy(
            widthDp = if (size.isSpecified) {
                size.width.value.roundToInt().coerceIn(MinWindowWidthDp, MaxWindowWidthDp)
            } else {
                widthDp
            },
            heightDp = if (size.isSpecified) {
                size.height.value.roundToInt().coerceIn(MinWindowHeightDp, MaxWindowHeightDp)
            } else {
                heightDp
            },
            xDp = if (position.isSpecified) {
                position.x.value.roundToInt()
            } else {
                xDp
            },
            yDp = if (position.isSpecified) {
                position.y.value.roundToInt()
            } else {
                yDp
            },
            maximized = windowState.placement == WindowPlacement.Maximized,
        )
    }

    private const val MinWindowWidthDp = 960
    private const val MinWindowHeightDp = 640
    private const val MaxWindowWidthDp = 4096
    private const val MaxWindowHeightDp = 2160
}
