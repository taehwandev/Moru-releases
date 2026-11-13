package app.thdev.moru.core.data.agent

/**
 * The folders AGY's last command in [stepPayload] names: the folders its command line moves into with `cd`, then the
 * folder AGY ran it in. AGY writes each command as `{"CommandLine":…,"Cwd":…}` inside the step.
 */
fun agyStepFolders(stepPayload: String): List<String> {
    val cwd = AgyCwd.findAll(stepPayload).lastOrNull() ?: return emptyList()
    val folder = unescapeJson(cwd.groupValues[1])
    val command = AgyCommandLine.findAll(stepPayload.substring(0, cwd.range.first)).lastOrNull()
        ?.let { match -> unescapeJson(match.groupValues[1]) }
    return (command?.let { commandFolders(it, folder) }.orEmpty() + folder).distinct()
}

private val AgyCwd = Regex(""""Cwd":"$JsonString"""")
private val AgyCommandLine = Regex(""""CommandLine":"$JsonString"""")
