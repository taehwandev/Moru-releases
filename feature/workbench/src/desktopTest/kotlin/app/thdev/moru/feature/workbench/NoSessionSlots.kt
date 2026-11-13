package app.thdev.moru.feature.workbench

import app.thdev.moru.core.model.session.SessionActivity
import app.thdev.moru.feature.workbench.slots.WorkbenchSessionSlots

/** A stand-in for the session views: tests of the sidebar's own controls draw no sessions. */
internal val NoSessionSlots = WorkbenchSessionSlots(
    popover = {},
    row = { _, _ -> },
    unattached = {},
    activityOf = { _, _, _ -> SessionActivity.Idle },
)
