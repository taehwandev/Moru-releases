package app.thdev.moru.core.terminal

/**
 * A permission request said in a few words: what kind of thing the agent wants and the one thing it acts on (the
 * command, the file, the address). [target] is null when the screen does not say it in a form this recognizes; the
 * full question and details are then the only honest way to show the request.
 */
data class PermissionSummary(val kind: PermissionKind, val target: String?)
