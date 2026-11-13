package app.thdev.moru.app

import java.io.File

internal fun File.readableCanonicalDirectory(): File? =
    runCatching { takeIf { isDirectory && canRead() }?.canonicalFile }.getOrNull()
