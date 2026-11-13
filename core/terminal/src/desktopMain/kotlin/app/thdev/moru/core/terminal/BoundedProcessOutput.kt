package app.thdev.moru.core.terminal

/** Captured output from a completed transient command. */
data class BoundedProcessOutput(val exitCode: Int, val bytes: ByteArray) {
    fun text(): String = bytes.toString(Charsets.UTF_8)
}
