package app.thdev.moru.app

import kotlin.test.Test
import kotlin.test.assertEquals

class QuitPlanTest {
    @Test
    fun rememberingTheLastStateOnlyLetsTheTerminalsGoWhateverIsRunning() {
        assertEquals(QuitPlan.Detach, quitPlan(rememberLastState = true, runningTerminals = 0))
        assertEquals(QuitPlan.Detach, quitPlan(rememberLastState = true, runningTerminals = 3))
    }

    @Test
    fun withItOffRunningTerminalsAreAskedAboutAndNoneEndsAtOnce() {
        assertEquals(QuitPlan.Ask, quitPlan(rememberLastState = false, runningTerminals = 1))
        assertEquals(QuitPlan.EndAll, quitPlan(rememberLastState = false, runningTerminals = 0))
    }
}
