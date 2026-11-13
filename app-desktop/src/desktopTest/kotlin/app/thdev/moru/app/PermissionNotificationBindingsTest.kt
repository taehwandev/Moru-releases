package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PermissionNotificationBindingsTest {
    @Test
    fun receiptCanResolveOnlyItsExactLiveSessionRequest() {
        val bindings = NotificationPermissionBindings()
        bindings.capture("receipt", "session", "request-1")
        val live = mapOf("session" to "request-1", "other-session" to "request-1")
        assertEquals(NotificationPermissionBindings.Binding("session", "request-1"), bindings.resolve("receipt", true, live::get))
        assertNull(bindings.resolve("receipt", false, live::get))
        assertNull(bindings.resolve("receipt", true) { "request-2" })
        assertNull(bindings.resolve("receipt", true, emptyMap<String, String>()::get))
    }

    @Test
    fun storedHistoryAndNewProcessHaveNoPermissionAuthority() {
        val previousRun = NotificationPermissionBindings()
        previousRun.capture("receipt", "session", "request")
        val restarted = NotificationPermissionBindings()
        assertNull(restarted.resolve("receipt", true) { "request" })
        assertNull(previousRun.resolve("unbound-old-history", true) { "request" })
        previousRun.capture("receipt", null, null)
        assertNull(previousRun.resolve("receipt", true) { "request" })
    }

    @Test
    fun respondingDeletingAndClearingDiscardBindings() {
        val bindings = NotificationPermissionBindings()
        bindings.capture("one", "session", "request")
        bindings.capture("two", "session", "request")
        bindings.remove("one")
        assertNull(bindings.resolve("one", true) { "request" })
        assertNotNull(bindings.resolve("two", true) { "request" })
        bindings.clear()
        assertNull(bindings.resolve("two", true) { "request" })
    }

    @Test
    fun retainedBindingsAreBoundedAndNewestReceiptRemainsAvailable() {
        val bindings = NotificationPermissionBindings()
        repeat(501) { bindings.capture("receipt-$it", "session", "request") }
        assertNull(bindings.resolve("receipt-0", true) { "request" })
        assertNotNull(bindings.resolve("receipt-1", true) { "request" })
        assertNotNull(bindings.resolve("receipt-500", true) { "request" })
    }
}
