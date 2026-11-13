package app.thdev.moru.app

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Opens a web page in Moru's own Preview, beside the Task whose session [sessionId] it came from, rather than in the
 * system browser. Null outside a workbench that shows a Preview.
 */
internal val LocalOpenInPreview = staticCompositionLocalOf<((sessionId: String, url: String) -> Unit)?> { null }
