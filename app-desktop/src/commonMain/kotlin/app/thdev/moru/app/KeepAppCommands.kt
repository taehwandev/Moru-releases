package app.thdev.moru.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import app.thdev.moru.core.model.KeyboardSettings

/** Keep native menu actions and their shortcuts aligned with the selected Project, app destination and settings. */
@Composable
internal fun KeepAppCommands(
    commands: MoruCommands,
    navigation: WorkbenchNavigation,
    keyboard: KeyboardSettings,
    onSelect: (MoruDestination) -> Unit,
) {
    val hasProject = navigation.selectedProject != null
    val canMoveBetweenTasks = (navigation.selectedProject?.tasks?.size ?: 0) > 1
    val projects = navigation.sidebar.projects
    SideEffect {
        commands.keyboard = keyboard
        commands.showSettings = { onSelect(MoruDestination.Settings) }
        commands.showKnowledge = { onSelect(MoruDestination.Knowledge) }
        // A Project opens the way a click on its sidebar row does.
        commands.projects = projects.map { project ->
            ProjectCommand(project.displayName) { onSelect(MoruDestination.Workbench); navigation.selectProject(project.id) }
        }
        commands.nextProject = { onSelect(MoruDestination.Workbench); navigation.selectAdjacentProject(1) }
            .takeIf { projects.size > 1 }
        commands.previousProject = { onSelect(MoruDestination.Workbench); navigation.selectAdjacentProject(-1) }
            .takeIf { projects.size > 1 }
        // Moving between Tasks keeps the open view; only Settings or another screen gives way to the workbench.
        commands.nextTask = { showProjectTab(navigation, navigation.tab) { onSelect(MoruDestination.Workbench) }; navigation.selectAdjacentTask(1) }
            .takeIf { canMoveBetweenTasks }
        commands.previousTask = { showProjectTab(navigation, navigation.tab) { onSelect(MoruDestination.Workbench) }; navigation.selectAdjacentTask(-1) }
            .takeIf { canMoveBetweenTasks }
        commands.showWork = { showProjectTab(navigation, ProjectTab.Work) { onSelect(MoruDestination.Workbench) } }
            .takeIf { hasProject }
        commands.showCode = { showProjectTab(navigation, ProjectTab.Code) { onSelect(MoruDestination.Workbench) } }
            .takeIf { hasProject }
        commands.showGit = { showProjectTab(navigation, ProjectTab.Git) { onSelect(MoruDestination.Workbench) } }
            .takeIf { hasProject }
    }
}
