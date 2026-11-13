package app.thdev.moru.core.data.git

internal data class GitReadEvidence(val values: Map<String, ByteArray>) {
    fun bytes(name: String): ByteArray = values.getValue(name)
    fun text(name: String): String = GitStatusParser.decode(bytes(name)).removeSuffix("\n")
    fun sameAs(other: GitReadEvidence): Boolean = values.keys == other.values.keys &&
        values.all { (key, value) -> value.contentEquals(other.values.getValue(key)) }
}
