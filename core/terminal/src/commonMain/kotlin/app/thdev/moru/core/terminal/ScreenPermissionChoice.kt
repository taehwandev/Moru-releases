package app.thdev.moru.core.terminal

/** The kind of a choice by its first words: "Yes", "Allow once", "Yes, and don't ask again…", "No, and tell…". */
fun permissionChoiceKind(label: String): PermissionChoiceKind = when {
    Deny.containsMatchIn(label) -> PermissionChoiceKind.Deny
    Allow.containsMatchIn(label) && Always.containsMatchIn(label) -> PermissionChoiceKind.AllowAlways
    Allow.containsMatchIn(label) -> PermissionChoiceKind.Allow
    else -> PermissionChoiceKind.Other
}

private val Allow = Regex("(?i)^(yes|allow|approve|proceed)\\b")
private val Always = Regex("(?i)don't ask again|do not ask again|\\balways\\b|for this session|during this session|for the session")
private val Deny = Regex("(?i)^(no|deny|reject|cancel)\\b")
