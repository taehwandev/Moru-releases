package app.thdev.moru.core.designsystem.material

import com.sun.jna.Library

/** Fixed in-process AppKit bridge; handles originate only from live Skiko windows. */
internal interface MacBackdropApi : Library {
    fun moru_backdrop_install(window: Long, role: Int, radius: Double, dark: Int): Long
    fun moru_backdrop_remove(token: Long)
    fun moru_backdrop_kind(token: Long): Int
    fun moru_backdrop_count(): Int
    fun moru_backdrop_probe(window: Long, x: Double, y: Double): Int
}
