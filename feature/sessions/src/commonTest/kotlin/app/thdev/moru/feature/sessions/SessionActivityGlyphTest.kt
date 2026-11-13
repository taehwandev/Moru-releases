package app.thdev.moru.feature.sessions

import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.model.session.SessionActivity
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionActivityGlyphTest {
    @Test
    fun stillShapesKeepRunningAttentionAndNeutralExitsDistinct() {
        assertEquals(MoruStatusGlyphKind.Working, SessionActivity.Starting.glyphKind())
        assertEquals(MoruStatusGlyphKind.Working, SessionActivity.Working.glyphKind())
        assertEquals(MoruStatusGlyphKind.NeedsYou, SessionActivity.Quiet.glyphKind())
        assertEquals(MoruStatusGlyphKind.Idle, SessionActivity.Idle.glyphKind())
        assertEquals(MoruStatusGlyphKind.Idle, SessionActivity.Exited.glyphKind())
        assertEquals(MoruStatusGlyphKind.Error, SessionActivity.Failed.glyphKind())
    }
}
