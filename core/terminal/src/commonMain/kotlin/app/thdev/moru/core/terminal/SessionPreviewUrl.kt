package app.thdev.moru.core.terminal

import java.net.URI

/**
 * The page a session's row offers to open. An address the program printed keeps its path (`/ko/account/`), but only
 * while a process under that terminal serves its port ([servedPorts]): agents print addresses of servers they only
 * talked about, such as another project's dev server, and those are not this session's. Otherwise it is the server
 * found under the terminal ([served]), or none.
 */
fun sessionPreviewUrl(printed: String?, servedPorts: Set<Int>, served: String?): String? =
    printed?.takeIf { url -> portOf(url)?.let { port -> port in servedPorts } == true } ?: served

private fun portOf(url: String): Int? =
    runCatching {
        val uri = URI(url)
        when {
            uri.port != -1 -> uri.port
            uri.scheme.equals("https", ignoreCase = true) -> 443
            uri.scheme.equals("http", ignoreCase = true) -> 80
            else -> null
        }
    }.getOrNull()
