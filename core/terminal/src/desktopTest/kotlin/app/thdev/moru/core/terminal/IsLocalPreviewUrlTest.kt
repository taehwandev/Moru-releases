package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsLocalPreviewUrlTest {
    @Test
    fun acceptsLoopbackWebAddresses() {
        assertTrue(isLocalPreviewUrl("http://localhost:5173/"))
        assertTrue(isLocalPreviewUrl("https://127.0.0.1:8443/app"))
        assertTrue(isLocalPreviewUrl("http://[::1]:3000"))
    }

    @Test
    fun refusesAnythingThatLeavesThisMachineOrIsNotWeb() {
        assertFalse(isLocalPreviewUrl("http://192.168.0.5:3000"))
        assertFalse(isLocalPreviewUrl("http://localhost.evil.com:3000"))
        assertFalse(isLocalPreviewUrl("http://user@evil.com/@localhost"))
        assertFalse(isLocalPreviewUrl("file:///etc/passwd"))
        assertFalse(isLocalPreviewUrl("javascript:alert(1)"))
        assertFalse(isLocalPreviewUrl("not a url"))
    }
}
