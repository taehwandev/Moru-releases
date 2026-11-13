package app.thdev.moru.core.system

import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Separates platform and process boundaries so checks never open a real share chooser or send a file. */
internal fun launchResultFileShare(
    file: File,
    osName: String,
    helper: () -> File?,
    launch: (File, File) -> Process,
): Boolean {
    if (!osName.lowercase().contains("mac") || !file.isFile || !file.canRead()) return false
    val binary = runCatching(helper).getOrNull() ?: return false
    val process = runCatching { launch(binary, file) }.getOrNull() ?: return false
    val shown = CompletableFuture<Boolean>()
    thread(isDaemon = true, name = "moru-result-share-ready") {
        val ready = runCatching { process.inputStream.bufferedReader().use { it.readLine() == "shown" } }.getOrDefault(false)
        shown.complete(ready)
    }
    runCatching { process.outputStream.close() }
    val ready = runCatching { shown.get(5, TimeUnit.SECONDS) }.getOrDefault(false)
    if (!ready) process.destroy()
    return ready
}
