package app.thdev.moru.app

/** Switches the open Project to [tab], leaving any new-Task composer, and shows the workbench. */
internal fun showProjectTab(navigation: WorkbenchNavigation, tab: ProjectTab, showWorkbench: () -> Unit) {
    showWorkbench()
    navigation.tab = tab
    navigation.cancelComposition()
}
