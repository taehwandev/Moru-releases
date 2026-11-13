package app.thdev.moru.app

import app.thdev.moru.app.i18n.AppStrings

/**
 * What Moru says when it cannot start, given the [error] it failed with and the [log] holding its details. Data a newer
 * Moru saved (a database at a version this one has no way back from) gets its own explanation: nothing is wrong with
 * it, it only needs that newer Moru.
 */
internal fun startupFailureText(strings: AppStrings, error: Throwable, log: String): String {
    val messages = generateSequence(error) { it.cause }.mapNotNull { it.message }.toList()
    val newer = messages.any { message ->
        Downgrade.find(message)?.let { match -> match.groupValues[1].toInt() > match.groupValues[2].toInt() } == true
    }
    return if (newer) strings.startupNeedsNewerMoru(log)
    else strings.startupFailed(messages.firstOrNull() ?: error::class.simpleName.orEmpty(), log)
}

// Room: "A migration from 5 to 4 was required but not found."
private val Downgrade = Regex("""migration from (\d+) to (\d+)""", RegexOption.IGNORE_CASE)
