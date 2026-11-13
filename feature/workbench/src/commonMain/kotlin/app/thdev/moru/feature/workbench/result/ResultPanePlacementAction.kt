package app.thdev.moru.feature.workbench.result

/** The host supplies exactly the action appropriate to the Result's current window. */
sealed interface ResultPanePlacementAction {
    val onClick: () -> Unit

    data class Detach(override val onClick: () -> Unit) : ResultPanePlacementAction
    data class Dock(override val onClick: () -> Unit) : ResultPanePlacementAction
}
