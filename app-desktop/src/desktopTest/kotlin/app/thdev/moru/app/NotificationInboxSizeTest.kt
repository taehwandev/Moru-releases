package app.thdev.moru.app

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationInboxSizeTest {
    @Test
    fun aWindowSmallerThanTheOldMinimumStillBoundsTheInbox() {
        assertEquals(144.dp to 96.dp, notificationInboxSize(28.dp, 180.dp, 160.dp))
    }

    @Test
    fun exhaustedSpaceNeverProducesNegativeDimensions() {
        assertEquals(0.dp to 0.dp, notificationInboxSize(28.dp, 30.dp, 20.dp))
    }
}
