package app.thdev.moru.core.domain.markdown

/** Existing project-document naming and lexical navigation rules; IO stays native. */
object MarkdownDocumentPolicy {
    fun isMarkdownPath(path: String): Boolean =
        path.endsWith(".md", ignoreCase = true) || path.endsWith(".markdown", ignoreCase = true)

    private val UriScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private const val MaxLinkLength = 8192

    /** Pure lexical resolution; the project file adapter remains the filesystem boundary. */
    fun resolveDocumentLink(fromPath: String, target: String, knownPaths: List<String>): String? {
        if (target.length > MaxLinkLength || target.isBlank()) return null
        val raw = target.substringBefore('#').substringBefore('?')
        if (raw.isBlank()) return null
        val decoded = decodeLinkPath(raw) ?: return null
        if (decoded.startsWith('/') || decoded.startsWith('\\') || '\\' in decoded || UriScheme.containsMatchIn(decoded)) return null
        if (decoded.any { it.code < 32 }) return null
        val parent = fromPath.substringBeforeLast('/', "")
        val relative = normalizeProjectPath(if (parent.isEmpty()) decoded else "$parent/$decoded") ?: return null
        val known = knownPaths.take(10_000).mapNotNull(::normalizeProjectPath).toSet()
        if (relative in known) return relative
        val hasExtension = '.' in relative.substringAfterLast('/')
        if (!hasExtension && "$relative.md" in known) return "$relative.md"
        if ('/' !in decoded && !hasExtension) {
            val matches = known.filter { isMarkdownPath(it) && it.substringAfterLast('/').substringBeforeLast('.') == decoded }
            if (matches.size > 1) return null
            if (matches.size == 1) return matches.single()
        }
        return if (hasExtension) relative else "$relative.md"
    }

    private fun normalizeProjectPath(path: String): String? {
        if (path.length > MaxLinkLength || path.startsWith('/') || '\\' in path || UriScheme.containsMatchIn(path)) return null
        val parts = mutableListOf<String>()
        for (part in path.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isEmpty()) return null else parts.removeAt(parts.lastIndex)
                else -> parts += part
            }
        }
        return parts.joinToString("/").takeIf(String::isNotEmpty)
    }

    private fun decodeLinkPath(path: String): String? {
        val result = StringBuilder()
        var index = 0
        while (index < path.length) {
            if (path[index] != '%') { result.append(path[index++]); continue }
            val bytes = mutableListOf<Byte>()
            while (index < path.length && path[index] == '%') {
                if (index + 2 >= path.length) return null
                val value = path.substring(index + 1, index + 3).toIntOrNull(16) ?: return null
                bytes += value.toByte()
                index += 3
            }
            try { result.append(bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)) } catch (_: Exception) { return null }
        }
        return result.toString()
    }
}
