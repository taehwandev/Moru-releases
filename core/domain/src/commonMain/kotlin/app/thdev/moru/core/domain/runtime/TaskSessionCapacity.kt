package app.thdev.moru.core.domain.runtime

/** Admission only; restoring existing sessions never discards them. */
object TaskSessionCapacity {
    const val Maximum = 8
    const val LimitMessage = "A Task can have at most eight terminal sessions. Close a session before adding another."
    fun canAdd(count: Int): Boolean = count in 0 until Maximum
}
