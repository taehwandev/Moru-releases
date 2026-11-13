package app.thdev.moru.feature.settings

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

/** Shows [page] by clicking its entry in the page list, as the user would. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.openSettingsPage(page: SettingsPage) {
    onNodeWithTag("settings-page-${page.name}").performClick()
    waitForIdle()
}
