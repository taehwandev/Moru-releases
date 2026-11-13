package app.thdev.moru.feature.workbench.resume

/** Unknown process readings cannot establish that it is safe to offer Resume. */
internal fun agentRunningState(readings: List<Boolean?>): Boolean? = when {
    readings.any { it == true } -> true
    readings.any { it == null } -> null
    else -> false
}
