package app.thdev.moru.app

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** The public repository this community edition's issues are filed in. */
internal const val IssuesRepository = "taehwandev/Moru-kmp"

/** The issue for a start that failed with [error]; see [moruIssueUrl]. */
internal fun startupIssueUrl(
    error: Throwable,
    version: String,
    system: String,
    home: String = System.getProperty("user.home").orEmpty(),
    repository: String = IssuesRepository,
): String = moruIssueUrl("Moru could not start", "Moru could not start.", error, version, system, home, repository)

/** The issue for [error] that nothing caught, on the thread named [thread]; see [moruIssueUrl]. */
internal fun crashIssueUrl(
    error: Throwable,
    thread: String,
    version: String,
    system: String,
    home: String = System.getProperty("user.home").orEmpty(),
    repository: String = IssuesRepository,
): String = moruIssueUrl("Moru crashed", "Moru ran into an error nothing handled, on the thread $thread.", error, version, system, home, repository)

/** The system Moru runs on, as an issue names it. */
internal fun moruSystemDescription(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}"

/**
 * A new GitHub issue on Moru's public repository, titled [title] and the reason, filled in with [heading], the
 * version, the system and the error with its stack. Nothing is sent; the page opens for the user to read, edit and
 * submit. The issue is public, so the home folder in any path is written as `~`, and the stack is cut so the address
 * stays short.
 */
internal fun moruIssueUrl(
    title: String,
    heading: String,
    error: Throwable,
    version: String,
    system: String,
    home: String,
    repository: String,
): String {
    val reason = generateSequence(error) { it.cause }.mapNotNull { it.message }.firstOrNull() ?: error::class.simpleName.orEmpty()
    val stack = error.stackTraceText().lineSequence().take(MaxStackLines).joinToString("\n")
    val body = """
        |$heading
        |
        |- Version: $version
        |- System: $system
        |
        |**What happened**
        |
        |<!-- What were you doing, and had you just updated or gone back to an older Moru? -->
        |
        |**Error**
        |
        |```
        |$stack
        |```
    """.trimMargin().redactHome(home).take(MaxBodyChars)
    val fullTitle = "$title: ${reason.redactHome(home)}".take(MaxTitleChars)
    return "https://github.com/$repository/issues/new?title=${fullTitle.encoded()}&body=${body.encoded()}&labels=bug"
}

private fun Throwable.stackTraceText(): String = java.io.StringWriter().also { printStackTrace(java.io.PrintWriter(it)) }.toString()

private fun String.redactHome(home: String): String = if (home.length > 1) replace(home, "~") else this

private fun String.encoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8).replace("+", "%20")

private const val MaxStackLines = 40
private const val MaxBodyChars = 6_000
private const val MaxTitleChars = 120
