package app.thdev.moru.app

import app.thdev.moru.core.model.AppIcon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconChoiceTest {
    @Test
    fun everyChoiceDrawsAtDockSizeAndOneWithoutItsOwnPictureShowsTheDefault() {
        val default = MoruDesktopRuntimeProperties.applicationIcon()
        AppIcon.entries.forEach { choice ->
            val picture = MoruDesktopRuntimeProperties.applicationIcon(choice)
            assertEquals(512, picture.width)
            assertEquals(512, picture.height)
            assertEquals(default.getRGB(256, 256), picture.getRGB(256, 256), "this build ships one picture")
        }
    }

    @Test
    fun anIdThisBuildDoesNotKnowIsTheDefault() {
        assertEquals(AppIcon.Moru, AppIcon.fromId("rocket"))
        assertEquals(AppIcon.Moru, AppIcon.fromId(null))
        assertTrue(AppIcon.entries.all { AppIcon.fromId(it.id) == it })
    }
}
