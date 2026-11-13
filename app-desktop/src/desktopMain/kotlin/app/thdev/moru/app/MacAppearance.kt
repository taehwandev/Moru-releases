package app.thdev.moru.app

import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer

/**
 * Sets the look of the whole macOS application, so the title bar, the menus that drop down and every popup follow
 * the app's own theme and not the computer's. Java starts every application pinned to the light look whatever the
 * computer is set to, which is why a dark app still got a light title bar. This talks to AppKit through the
 * Objective-C runtime and does nothing, returning false, anywhere else or if the runtime cannot be reached. The menu
 * strip along the top of the screen is drawn by macOS from the wallpaper and the system setting, and stays as it is.
 */
internal object MacAppearance {
    /** Applies [look] to the running application. Returns whether the request reached AppKit. */
    fun apply(look: MacAppearanceLook, osName: String = System.getProperty("os.name").orEmpty()): Boolean {
        if (!MoruWindowChrome.isMac(osName)) return false
        return runCatching {
            val runtime = ObjectiveC()
            val application = runtime.send(runtime.cls("NSApplication"), "sharedApplication") ?: return false
            val appearance = look.appKitName?.let { name ->
                runtime.send(runtime.cls("NSAppearance"), "appearanceNamed:", runtime.string(name))
            }
            // AppKit wants this on its main thread, and a call from any other thread aborts the process. Java drives
            // that thread's run loop in a mode of its own, so a request queued for the usual modes alone never runs.
            val modes = runtime.send(runtime.cls("NSMutableArray"), "array")
            RunLoopModes.forEach { mode -> runtime.send(modes, "addObject:", runtime.string(mode)) }
            runtime.send(
                application, "performSelectorOnMainThread:withObject:waitUntilDone:modes:",
                runtime.selector("setAppearance:"), appearance, 0, modes,
            )
            true
        }.getOrDefault(false)
    }

    /** The appearance AppKit is really using for the application, such as "NSAppearanceNameDarkAqua", or null. */
    fun effectiveName(osName: String = System.getProperty("os.name").orEmpty()): String? {
        if (!MoruWindowChrome.isMac(osName)) return null
        return runCatching {
            val runtime = ObjectiveC()
            val application = runtime.send(runtime.cls("NSApplication"), "sharedApplication") ?: return null
            runtime.nameOf(runtime.send(application, "effectiveAppearance"))
        }.getOrNull()
    }

    /** The appearance each open window is really drawn with, for checking that they follow the application. */
    fun windowEffectiveNames(osName: String = System.getProperty("os.name").orEmpty()): List<String?> {
        if (!MoruWindowChrome.isMac(osName)) return emptyList()
        return runCatching {
            val runtime = ObjectiveC()
            val application = runtime.send(runtime.cls("NSApplication"), "sharedApplication") ?: return emptyList()
            val windows = runtime.send(application, "windows")
            val count = runtime.send(windows, "count")?.let { Pointer.nativeValue(it).toInt() } ?: 0
            (0 until count).map { index ->
                runtime.nameOf(runtime.send(runtime.send(windows, "objectAtIndex:", index.toLong()), "effectiveAppearance"))
            }
        }.getOrDefault(emptyList())
    }

    private val RunLoopModes = listOf(
        "NSRunLoopCommonModes", "AWTRunLoopMode", "NSDefaultRunLoopMode", "NSEventTrackingRunLoopMode", "NSModalPanelRunLoopMode",
    )

    /** The few Objective-C runtime calls needed, with fixed argument lists (message sends are not variadic). */
    private class ObjectiveC {
        private val library = NativeLibrary.getInstance("objc")
        private val getClass: Function = library.getFunction("objc_getClass")
        private val register: Function = library.getFunction("sel_registerName")
        private val msgSend: Function = library.getFunction("objc_msgSend")

        fun cls(name: String): Pointer? = getClass.invokePointer(arrayOf(name))

        fun selector(name: String): Pointer? = register.invokePointer(arrayOf(name))

        fun send(receiver: Pointer?, selector: String, vararg arguments: Any?): Pointer? {
            if (receiver == null) return null
            return msgSend.invokePointer(arrayOf<Any?>(receiver, selector(selector), *arguments))
        }

        fun string(text: String): Pointer? = send(cls("NSString"), "stringWithUTF8String:", text)

        fun nameOf(appearance: Pointer?): String? = send(send(appearance, "name"), "UTF8String")?.getString(0)
    }
}
