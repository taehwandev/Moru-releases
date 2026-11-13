package app.thdev.moru.core.terminal

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionPermissionsTest {
    private val session = "permission-registry-test"
    private val prompt = SessionPermissionPrompt("Allow Bash?", "pwd", listOf(SessionPermissionChoice("allow", "Allow once"), SessionPermissionChoice("deny", "Deny")))
    @AfterTest fun clear() = SessionPermissions.forget(session)

    @Test fun allSurfacesConsumeOneRequestAndRedrawCannotReplayIt() {
        var sends = 0
        SessionPermissions.observe(session, prompt) { sends++; true }
        val shown = SessionPermissions.requests.value.getValue(session)
        SessionPermissions.observe(session, prompt) { sends++; true }
        assertEquals(shown.id, SessionPermissions.requests.value.getValue(session).id)
        assertFalse(SessionPermissions.respond(session, shown.id, "unknown"))
        assertTrue(SessionPermissions.respond(session, shown.id, "allow"))
        assertFalse(SessionPermissions.respond(session, shown.id, "deny"))
        SessionPermissions.observe(session, prompt) { sends++; true }
        assertNull(SessionPermissions.requests.value[session])
        assertEquals(1, sends)
    }

    @Test fun replacedClosedAndNewEpisodesRejectOldIdentity() {
        SessionPermissions.observe(session, prompt) { true }
        val old = SessionPermissions.requests.value.getValue(session)
        SessionPermissions.observe(session, prompt.copy(details = "rm build")) { true }
        assertFalse(SessionPermissions.respond(session, old.id, "allow"))
        SessionPermissions.forget(session)
        SessionPermissions.observe(session, prompt) { true }
        assertFalse(SessionPermissions.respond(session, old.id, "allow"))
        SessionPermissions.forget(session)
        assertNull(SessionPermissions.requests.value[session])
    }

    @Test fun failedSendingCanRetryButCannotRestoreAReplacedRequest() {
        var available = false
        SessionPermissions.observe(session, prompt) { available }
        val shown = SessionPermissions.requests.value.getValue(session)
        assertFalse(SessionPermissions.respond(session, shown.id, "allow"))
        assertNotNull(SessionPermissions.requests.value[session])
        available = true
        assertTrue(SessionPermissions.respond(session, shown.id, "allow"))
        SessionPermissions.forget(session)
        SessionPermissions.observe(session, prompt) {
            SessionPermissions.observe(session, prompt.copy(details = "new")) { true }
            false
        }
        val previous = SessionPermissions.requests.value.getValue(session)
        assertFalse(SessionPermissions.respond(session, previous.id, "allow"))
        assertEquals("new", SessionPermissions.requests.value.getValue(session).details)
        assertFalse(SessionPermissions.respond(session, previous.id, "allow"))
    }
}
