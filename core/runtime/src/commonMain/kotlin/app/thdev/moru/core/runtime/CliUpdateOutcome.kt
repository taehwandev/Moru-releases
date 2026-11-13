package app.thdev.moru.core.runtime

/** What running a CLI's update did, judged by its `--version` before and after, not by the update's own words. */
sealed interface CliUpdateOutcome {
    data class Updated(val from: String?, val to: String) : CliUpdateOutcome

    /** The update ran but the version stayed: nothing newer on this install's channel yet. */
    data class Unchanged(val version: String?) : CliUpdateOutcome

    /** The update did not run or failed; [command] is the line the user can run themselves. */
    data class Failed(val command: String, val detail: String?) : CliUpdateOutcome
}
