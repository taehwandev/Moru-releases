package app.thdev.moru.core.designsystem.material

import java.awt.Window
import java.util.IdentityHashMap
import javax.swing.SwingUtilities

/** EDT-owned leases prevent docked hosts from multiplying effects or restoring shared opacity early. */
internal object MacBackdropRegistry {
    private class Entry(val controller: MacBackdropController, val role: Int) {
        var users = 0
    }

    private val windows = IdentityHashMap<Window, Entry>()

    fun acquire(window: Window, api: MacBackdropApi, role: Int, dark: Int): MacBackdropLease {
        check(SwingUtilities.isEventDispatchThread())
        val entry = windows.getOrPut(window) { Entry(MacBackdropController(window, api, role, dark), role) }
        check(entry.role == role) { "A native window has one material role" }
        entry.controller.updateAppearance(dark)
        entry.users++
        return MacBackdropLease(entry.controller) {
            check(SwingUtilities.isEventDispatchThread())
            entry.users--
            if (entry.users == 0) {
                windows.remove(window)
                entry.controller.close()
            }
        }
    }
}
