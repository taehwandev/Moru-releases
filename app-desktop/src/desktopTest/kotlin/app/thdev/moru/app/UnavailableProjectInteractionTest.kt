package app.thdev.moru.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/** A Project whose folder was renamed or moved offers to find that folder, so its Tasks are kept. */
@OptIn(ExperimentalTestApi::class)
class UnavailableProjectInteractionTest {
    @Test
    fun findFolderAsksWhereTheFolderIsNow() = runDesktopComposeUiTest(width = 900, height = 500) {
        var found = 0
        var opened = 0
        setContent {
            MoruTheme {
                CompositionLocalProvider(LocalUiLanguage provides UiLanguage.Korean) {
                    WorkbenchUnavailableProject("alpha", onFindFolder = { found += 1 }, onOpenProject = { opened += 1 })
                }
            }
        }

        onNodeWithText("폴더 찾기").performClick()

        assertEquals(1, found)
        assertEquals(0, opened)
    }
}
