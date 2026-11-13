package app.thdev.moru.core.data.agent

import java.io.File

/** Codex's newest thread list in [home], whose name carries its schema version. */
fun codexStateDatabase(home: File): File? =
    File(home, ".codex").listFiles { file -> CodexState.matches(file.name) }
        ?.maxByOrNull { file -> CodexState.matchEntire(file.name)!!.groupValues[1].toInt() }

private val CodexState = Regex("""state_(\d+)\.sqlite""")
