package app.thdev.moru.core.domain.markdown

/** Existing desktop image budgets; platform adapters retain resolution, transport and decode. */
object MarkdownImagePolicy {
    const val maximumEncodedBytes=8*1024*1024
    const val maximumSourceLength=4096
    const val maximumSourceDimension=8192
    const val maximumSourcePixels=24_000_000L
    const val maximumDecodedDimension=1600
    const val connectTimeoutMillis=3000
    const val readTimeoutMillis=5000
    const val maximumRequests=4

    /** Scheme admission only; platform adapters resolve URLs and local paths. */
    fun sourceKind(source: String): String? {
        if (source.isBlank() || source.length>maximumSourceLength) return null
        val scheme=Regex("^[A-Za-z][A-Za-z0-9+.-]*:").find(source)?.value?.dropLast(1)?.lowercase()
        return when (scheme) {
            "http","https" -> "Web"
            "file" -> "File"
            null -> "Path"
            else -> null
        }
    }
}
