package app.thdev.moru.core.domain.markdown

private val heading = Regex("""^ {0,3}(#{1,6})\s+(.*)$""")
private val listItem = Regex("""^(\s*)(?:([-*+])|(\d+)[.)])\s+(.*)$""")
private val task = Regex("""^\[([ xX])]\s+(.*)$""")
internal val fence = Regex("""^ {0,3}(`{3,}|~{3,})(.*)$""")
private val tableSeparator = Regex("""^:?-{3,}:?$""")
private val rule = Regex("""^ {0,3}(?:\*\s*){3,}$|^ {0,3}(?:-\s*){3,}$|^ {0,3}(?:_\s*){3,}$""")
private val callout = Regex("""^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION|AI|BOT|USER|STEP)](?:[ \t]+(.*))?$""", RegexOption.IGNORE_CASE)

/** A bounded source preview is parsed without executing HTML or embedded content. */
fun parseMarkdownBlocks(text: String): List<MarkdownBlock> = parseMarkdownBlocks(text, 0)

internal fun parseMarkdownBlocks(text: String, stepOffset: Int): List<MarkdownBlock> = parseBlocks(text, 0, intArrayOf(stepOffset))

private fun parseBlocks(text: String, quoteDepth: Int, steps: IntArray): List<MarkdownBlock> {
    val lines = text.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    fun flush() {
        if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
        paragraph.clear()
    }
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val codeFence = fence.matchEntire(line)
        val title = heading.matchEntire(line)
        val item = listItem.matchEntire(line)
        val columns = tableCells(line)
        val separators = lines.getOrNull(index + 1)?.let(::tableCells).orEmpty()
        when {
            codeFence != null -> {
                flush()
                val marker = codeFence.groupValues[1]
                val code = mutableListOf<String>()
                index++
                while (index < lines.size && !isClosingFence(lines[index], marker)) code += lines[index++]
                blocks += MarkdownBlock.CodeBlock(code.joinToString("\n"), codeFence.groupValues[2].trim())
            }
            line.isBlank() -> flush()
            title != null -> {
                flush()
                blocks += MarkdownBlock.Heading(title.groupValues[1].length, title.groupValues[2].trim().replace(Regex("""\s+#+\s*$"""), ""))
            }
            rule.matches(line) -> { flush(); blocks += MarkdownBlock.Rule }
            quoteDepth < 8 && line.trimStart().startsWith('>') -> {
                flush()
                val quoted = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith('>')) {
                    quoted += lines[index++].trimStart().removePrefix(">").removePrefix(" ")
                }
                val alert = callout.matchEntire(quoted.first().trim())
                if (alert == null) {
                    blocks += MarkdownBlock.Quote(parseBlocks(quoted.joinToString("\n"), quoteDepth + 1, steps))
                } else {
                    val kind = MarkdownCalloutKind.entries.first { it.marker.equals(alert.groupValues[1], ignoreCase = true) }
                    val stepNumber = if (kind == MarkdownCalloutKind.Step) ++steps[0] else null
                    quoted[0] = alert.groupValues[2]
                    blocks += MarkdownBlock.Callout(kind, parseBlocks(quoted.joinToString("\n"), quoteDepth + 1, steps), stepNumber)
                }
                index--
            }
            columns.size > 1 && separators.size == columns.size && separators.all(tableSeparator::matches) -> {
                flush()
                val rows = mutableListOf<List<String>>()
                index += 2
                while (index < lines.size && lines[index].isNotBlank() && lines[index].contains('|')) {
                    rows += tableCells(lines[index++]).take(columns.size).let { it + List((columns.size - it.size).coerceAtLeast(0)) { "" } }
                }
                blocks += MarkdownBlock.Table(columns, rows, separators.map {
                    when { it.startsWith(':') && it.endsWith(':') -> MarkdownTableAlignment.Center; it.endsWith(':') -> MarkdownTableAlignment.End; else -> MarkdownTableAlignment.Start }
                })
                index--
            }
            item != null -> {
                flush()
                val content = item.groupValues[4]
                val check = task.matchEntire(content)
                blocks += MarkdownBlock.ListItem(
                    text = check?.groupValues?.get(2) ?: content,
                    depth = item.groupValues[1].replace("\t", "    ").length / 2,
                    number = item.groupValues[3].takeIf(String::isNotEmpty),
                    checked = check?.groupValues?.get(1)?.let { it != " " },
                )
            }
            else -> paragraph += line.trim()
        }
        index++
    }
    flush()
    return blocks
}

internal fun isClosingFence(line: String, marker: String): Boolean {
    val trimmed = line.trim()
    return trimmed.length >= marker.length && trimmed.all { it == marker.first() }
}

internal fun tableCells(line: String): List<String> {
    // An escaped pipe and a pipe inside inline code are cell content.
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var escaped = false
    var inCode = false
    for (character in line.trim().removePrefix("|").removeSuffix("|")) {
        when {
            escaped -> { cell.append(character); escaped = false }
            character == '\\' -> escaped = true
            character == '`' -> { inCode = !inCode; cell.append(character) }
            character == '|' && !inCode -> { cells += cell.toString().trim(); cell.clear() }
            else -> cell.append(character)
        }
    }
    if (escaped) cell.append('\\')
    cells += cell.toString().trim()
    return cells
}
