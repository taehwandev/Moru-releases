package app.thdev.moru.core.model

/** Persisted lifecycle: `Hibernated` implies bounded descriptors, not live resources. */
enum class WorkContextPersistedLifecycle {
    NotCreated,
    Hibernated,
    Interrupted,
    RestoreFailed,
}

val WorkContextPersistedLifecycle.wireValue: String
    get() = when (this) {
        WorkContextPersistedLifecycle.NotCreated -> "not_created"
        WorkContextPersistedLifecycle.Hibernated -> "hibernated"
        WorkContextPersistedLifecycle.Interrupted -> "interrupted"
        WorkContextPersistedLifecycle.RestoreFailed -> "restore_failed"
    }

fun workContextPersistedLifecycleFromWireValue(value: String): WorkContextPersistedLifecycle =
    when (value) {
        "not_created" -> WorkContextPersistedLifecycle.NotCreated
        "hibernated" -> WorkContextPersistedLifecycle.Hibernated
        "interrupted" -> WorkContextPersistedLifecycle.Interrupted
        "restore_failed" -> WorkContextPersistedLifecycle.RestoreFailed
        else -> WorkContextPersistedLifecycle.NotCreated
    }
