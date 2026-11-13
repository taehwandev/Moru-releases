package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.terminalFilePaths
import app.thdev.moru.core.terminal.terminalFileTarget
import com.jediterm.terminal.model.hyperlinks.HyperlinkFilter
import com.jediterm.terminal.model.hyperlinks.LinkInfo
import com.jediterm.terminal.model.hyperlinks.LinkResult
import com.jediterm.terminal.model.hyperlinks.LinkResultItem

/**
 * Makes each path in a terminal line that names a file on this computer a link, read from [workingDirectory] when it is
 * relative. A click hands [onOpen] the file's absolute path, with the `:line` the terminal printed.
 */
internal class TerminalFileLinkFilter(
    private val workingDirectory: String,
    private val onOpen: (String) -> Unit,
) : HyperlinkFilter {
    override fun apply(line: String): LinkResult? {
        val items = terminalFilePaths(line).mapNotNull { found ->
            val target = terminalFileTarget(found.path, workingDirectory) ?: return@mapNotNull null
            LinkResultItem(found.start, found.end, LinkInfo { onOpen(target) })
        }
        return items.takeIf { it.isNotEmpty() }?.let(::LinkResult)
    }
}
