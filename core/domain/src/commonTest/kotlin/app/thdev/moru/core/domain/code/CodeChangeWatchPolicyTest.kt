package app.thdev.moru.core.domain.code

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeChangeWatchPolicyTest {
    @Test fun firstReadIsRequiredEvenWhenTheStampIsUnknownOrUnchanged() {
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(false, null, null))
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(false, "a", "a"))
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(false, "a", null))
    }

    @Test fun unreadableAndUnchangedStampsKeepAnExistingSnapshot() {
        assertFalse(CodeChangeWatchPolicy.shouldReadChanges(true, null, null))
        assertFalse(CodeChangeWatchPolicy.shouldReadChanges(true, "a", null))
        assertFalse(CodeChangeWatchPolicy.shouldReadChanges(true, "a", "a"))
    }

    @Test fun aMovedOrFirstKnownStampRereadsAnExistingSnapshot() {
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(true, "a", "b"))
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(true, null, "a"))
    }

    @Test fun stampsAreOpaqueIncludingAnEmptyKnownValue() {
        assertFalse(CodeChangeWatchPolicy.shouldReadChanges(true, "", ""))
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(true, null, ""))
        assertTrue(CodeChangeWatchPolicy.shouldReadChanges(true, "a", "a "))
    }
}
