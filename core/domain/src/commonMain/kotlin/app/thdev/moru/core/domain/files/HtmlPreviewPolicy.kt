package app.thdev.moru.core.domain.files

/** Existing HTML classification and decoded project-relative asset policy; callers own IO and URL decoding. */
object HtmlPreviewPolicy {
    const val maximumHtmlBytes: Int = 1024 * 1024
    const val maximumAssetBytes: Int = 16 * 1024 * 1024

    /** Preserve the feature classifier, including bare root names `html` and `htm`. */
    fun isHtmlPath(path: String): Boolean = path.substringAfterLast('.').lowercase() in setOf("html", "htm")

    /** A lexical path check, independent of MIME support; colon and percent characters remain literal. */
    fun isAllowedPath(path: String): Boolean = path.isNotEmpty() && !path.startsWith('/') &&
        '\\' !in path && '\u0000' !in path && path.split('/').all { it.isNotEmpty() && !it.startsWith('.') }

    fun mimeType(path: String): String? = contentTypes[path.substringAfterLast('/').substringAfterLast('.', "").lowercase()]

    private val contentTypes = mapOf(
        "html" to "text/html; charset=utf-8", "htm" to "text/html; charset=utf-8",
        "css" to "text/css; charset=utf-8", "js" to "text/javascript; charset=utf-8",
        "mjs" to "text/javascript; charset=utf-8", "json" to "application/json; charset=utf-8",
        "txt" to "text/plain; charset=utf-8", "wasm" to "application/wasm",
        "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
        "gif" to "image/gif", "svg" to "image/svg+xml", "webp" to "image/webp",
        "avif" to "image/avif", "bmp" to "image/bmp", "ico" to "image/x-icon", "apng" to "image/apng",
        "woff" to "font/woff", "woff2" to "font/woff2", "ttf" to "font/ttf", "otf" to "font/otf",
        "eot" to "application/vnd.ms-fontobject", "mp3" to "audio/mpeg", "wav" to "audio/wav",
        "ogg" to "audio/ogg", "oga" to "audio/ogg", "m4a" to "audio/mp4", "aac" to "audio/aac",
        "flac" to "audio/flac", "mp4" to "video/mp4", "m4v" to "video/mp4", "webm" to "video/webm",
        "ogv" to "video/ogg", "mov" to "video/quicktime",
    )
}
