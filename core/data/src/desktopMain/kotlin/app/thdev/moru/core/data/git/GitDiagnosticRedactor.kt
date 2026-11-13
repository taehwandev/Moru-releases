package app.thdev.moru.core.data.git

internal object GitDiagnosticRedactor {
    private val url = Regex("[A-Za-z][A-Za-z0-9+.-]*://[^\\s<>'\\\"]+")
    fun redact(value: String): String = url.replace(value) { match ->
        val raw = match.value
        val schemeEnd = raw.indexOf("://") + 3
        val authorityEnd = raw.indexOf('/', schemeEnd).let { if (it < 0) raw.length else it }
        val at = raw.lastIndexOf('@', authorityEnd)
        val safe = if (at >= schemeEnd) raw.substring(0, schemeEnd) + "[redacted]@" + raw.substring(at + 1) else raw
        safe.substringBefore('?').substringBefore('#')
    }.replace(Regex("[\\p{Cc}&&[^\\n\\t]]"), "").take(4_096)
}
