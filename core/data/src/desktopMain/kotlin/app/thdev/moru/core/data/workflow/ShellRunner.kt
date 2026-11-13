package app.thdev.moru.core.data.workflow

/** Runs one shell command and passes each line it prints to [onLine]; the exit code, or null when it never ended. */
fun interface ShellRunner {
    fun run(command: String, timeoutMillis: Long, onLine: (String) -> Unit): Int?
}
