package app.thdev.moru.core.data.git

internal data class GitProcessResult(
    val exitCode: Int,
    val stdout: ByteArray,
    val stderr: String,
    val uncertain: Boolean = false,
) {
    fun text(): String = stdout.toString(Charsets.UTF_8).removeSuffix("\n")
    fun requireSuccess(): ByteArray {
        check(!uncertain && exitCode == 0) { stderr.ifBlank { "Git could not read the repository." } }
        return stdout
    }
}
