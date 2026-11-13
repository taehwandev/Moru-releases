package app.thdev.moru.core.domain.markdown

/** Column-space distribution shared by the existing and native readers. */
object MarkdownTableLayout {
    data class Column(val narrowest: Int,val widest: Int)

    fun widths(columns: List<Column>, available: Int, floor: Int = 0): List<Int> {
        if (columns.isEmpty()) return emptyList()
        val widest = columns.map { it.widest }
        if (widest.sum() <= available) return widest
        val narrowest = columns.map { it.narrowest }
        if (narrowest.sum() < available) return shared(narrowest, widest, available)
        val floors = narrowest.map { minOf(it, floor) }
        if (floors.sum() >= available) return floors
        return shared(floors, widest, available)
    }

    /** Each column [from] its start, with the room left shared by how far each still is from [toward]. */
    private fun shared(from: List<Int>, toward: List<Int>, available: Int): List<Int> {
        val room = available - from.sum()
        val wants = from.indices.sumOf { (toward[it] - from[it]).toLong() }.coerceAtLeast(1)
        var given = 0
        return from.indices.map { index ->
            val share = if (index == from.lastIndex) room - given else ((toward[index] - from[index]) * room.toLong() / wants).toInt()
            given += share
            from[index] + share
        }
    }

}
