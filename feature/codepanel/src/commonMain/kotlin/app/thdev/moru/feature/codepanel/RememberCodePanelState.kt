package app.thdev.moru.feature.codepanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import app.thdev.moru.core.model.SavedCodePanel

/**
 * The Code panel's checkouts, started once from [saved] and reported to [onSavedChange] whenever a terminal's checkout
 * changes, so each Task's choice outlives the screen and the app.
 */
@Composable
fun rememberCodePanelState(
    saved: Map<String, SavedCodePanel>,
    onSavedChange: (Map<String, SavedCodePanel>) -> Unit,
): CodePanelState {
    val state = remember { CodePanelState(saved) }
    val savedChanged by rememberUpdatedState(onSavedChange)
    LaunchedEffect(state) { snapshotFlow { state.savedPanels }.collect { savedChanged(it) } }
    return state
}
