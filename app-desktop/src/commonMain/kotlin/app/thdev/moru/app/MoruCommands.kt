package app.thdev.moru.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.thdev.moru.core.model.KeyboardSettings

/**
 * What the menu bar can do right now. The screens that own the state register a command here while it applies and
 * clear it when it does not, so the menu, which lives outside them, enables an item exactly when it would work.
 */
class MoruCommands {
    var openProject by mutableStateOf<(() -> Unit)?>(null)
    var newTask by mutableStateOf<(() -> Unit)?>(null)
    var closeProject by mutableStateOf<(() -> Unit)?>(null)
    var newSession by mutableStateOf<(() -> Unit)?>(null)
    /** Opens a shell beside (right) or under (down) the selected terminal and makes every terminal the same size. */
    var splitRight by mutableStateOf<(() -> Unit)?>(null)
    var splitDown by mutableStateOf<(() -> Unit)?>(null)
    var closeTerminal by mutableStateOf<(() -> Unit)?>(null)
    var reopenClosedTerminal by mutableStateOf<(() -> Unit)?>(null)
    var showWork by mutableStateOf<(() -> Unit)?>(null)
    var showCode by mutableStateOf<(() -> Unit)?>(null)
    var showGit by mutableStateOf<(() -> Unit)?>(null)
    var showSettings by mutableStateOf<(() -> Unit)?>(null)
    var nextTask by mutableStateOf<(() -> Unit)?>(null)
    var previousTask by mutableStateOf<(() -> Unit)?>(null)
    var nextSession by mutableStateOf<(() -> Unit)?>(null)
    var previousSession by mutableStateOf<(() -> Unit)?>(null)
    var nextProject by mutableStateOf<(() -> Unit)?>(null)
    var previousProject by mutableStateOf<(() -> Unit)?>(null)
    var showKnowledge by mutableStateOf<(() -> Unit)?>(null)

    /** The open Projects in sidebar order, each with the command that opens it; the first nine get Command-1 to 9. */
    var projects by mutableStateOf<List<ProjectCommand>>(emptyList())

    /** The shortcuts as the user set them in Settings. */
    var keyboard by mutableStateOf(KeyboardSettings())

    /** Settings is waiting for a new shortcut, so every key goes there and no menu item runs. */
    var recordingShortcut by mutableStateOf(false)

    /** Checks for a newer Moru now; registered only by a packaged app that can update itself. */
    var checkForUpdates by mutableStateOf<(() -> Unit)?>(null)

    /** Quits and ends every terminal; a plain quit leaves them running in the terminal host. */
    var quitEndingTerminals by mutableStateOf<(() -> Unit)?>(null)
}
