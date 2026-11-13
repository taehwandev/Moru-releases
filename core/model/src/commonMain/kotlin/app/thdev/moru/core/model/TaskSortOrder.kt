package app.thdev.moru.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Display order by Task creation time, independent of later activity. */
@Serializable
enum class TaskSortOrder {
    @SerialName("oldest_first")
    OldestFirst,

    @SerialName("newest_first")
    NewestFirst,

    @SerialName("manual")
    Manual,
}
