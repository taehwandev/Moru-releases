package app.thdev.moru.core.domain.code

/** Root invalidation and polling belong to callers; an unreadable stamp alone never invalidates a snapshot. */
object CodeChangeWatchPolicy {
    fun shouldReadChanges(hasSnapshot: Boolean, previousStamp: String?, nextStamp: String?): Boolean =
        !hasSnapshot || (nextStamp != null && nextStamp != previousStamp)
}
