package app.thdev.moru.core.domain.git

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GitLogRecordsTest {
    private val hash = "a".repeat(40)
    private fun record(id: String = hash, parent: String = "", date: String = "1700000000",
                       subject: String = "Commit"): String =
        listOf(id, parent, "Ada", "ada@example.invalid", date, "", subject).joinToString("\u001f") + "\u001e"

    @Test fun emptyAndCompleteHistoryRemainDistinctFromIncompleteInput() {
        assertEquals(emptyList(), GitLogRecords.parse(""))
        assertEquals(hash, GitLogRecords.parse(record() + "\n").single().hash)
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record().dropLast(1)) }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse("\n") }
    }

    @Test fun rejectsMalformedRecordsInsteadOfPublishingAValidPrefix() {
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record() + "\nbroken\u001e") }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record(subject = "bad\u001fsubject")) }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record(subject = "bad\u001esubject")) }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record() + record()) }
    }

    @Test fun validatesFullCommitParentsAndTimestampWithoutFallbacks() {
        assertEquals(listOf("b".repeat(64)), GitLogRecords.parse(record(parent = "b".repeat(64))).single().parents)
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record(id = "--all")) }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record(parent = "short")) }
        assertFailsWith<IllegalArgumentException> { GitLogRecords.parse(record(date = "soon")) }
    }
}
