package app.thdev.moru.core.domain.git

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GitStatusRecordsTest {
    @Test fun literalWhitespaceAndRenamePairArePreserved() {
        val files = GitStatusRecords.parseNulTerminated(" M spaced name\u0000R  new\nname\u0000old name\u0000?? 한글.txt\u0000")
        assertEquals(listOf("spaced name", "new\nname", "한글.txt"), files.map { it.path })
        assertEquals("old name", files[1].originalPath)
        assertTrue(files[1].hasStaged)
        assertTrue(files[2].isUntracked)
    }

    @Test fun incompleteAndMalformedRecordsAreRejected() {
        for (raw in listOf(" M file", "R  new\u0000", "bad\u0000", "ZZ file\u0000", " M file\u0000\u0000")) {
            assertFailsWith<IllegalArgumentException> { GitStatusRecords.parseNulTerminated(raw) }
        }
    }

    @Test fun unsafePathsAreRejectedOnEitherRenameSide() {
        for (raw in listOf(" M ../outside\u0000", "?? /absolute\u0000", " M x//y\u0000",
            "R  new\u0000old/../outside\u0000", " M ./file\u0000")) {
            assertFailsWith<IllegalArgumentException> { GitStatusRecords.parseNulTerminated(raw) }
        }
    }

    @Test fun emptySnapshotIsClean() = assertEquals(emptyList(), GitStatusRecords.parseNulTerminated(""))
}
