package app.thdev.moru.core.system

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

/**
 * Opens the macOS system sharing chooser for [file]. True means the chooser was launched, never that the file was
 * delivered. False means unavailable or launch failed, so the caller can offer Save Markdown. Call off the UI thread.
 * Every destination and any subsequent send remain the user's choices in the native service.
 */
fun shareResultFile(file: File): Boolean = launchResultFileShare(
    file = file,
    osName = System.getProperty("os.name"),
    helper = ::resultShareHelper,
    launch = { binary, result ->
        ProcessBuilder(binary.absolutePath, result.absolutePath).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    },
)

/** Content-addressed extraction keeps this purpose-specific helper. */
private fun resultShareHelper(): File? = runCatching {
    val bytes = object {}.javaClass.classLoader.getResourceAsStream("moru-result-share/moru-result-share")
        ?.use { it.readBytes() } ?: return@runCatching null
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes).take(8).joinToString("") { "%02x".format(it) }
    val directory = File(System.getProperty("user.home"), "Library/Caches/Moru/result-share-$digest")
    Files.createDirectories(directory.toPath())
    val binary = File(directory, "moru-result-share")
    if (!binary.isFile) {
        val pending = Files.createTempFile(directory.toPath(), "helper-", ".partial").toFile()
        try {
            pending.writeBytes(bytes)
            check(pending.setExecutable(true, true)) { "Cannot make sharing helper executable" }
            Files.move(pending.toPath(), binary.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            pending.delete()
        }
    }
    if (binary.canExecute() || binary.setExecutable(true, true)) binary else null
}.getOrNull()
