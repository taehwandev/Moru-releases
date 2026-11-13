package app.thdev.moru.feature.workbench

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruDropdownMenu
import app.thdev.moru.core.designsystem.component.MoruIcon
import app.thdev.moru.core.designsystem.component.MoruIconButton
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruMenuItem
import app.thdev.moru.core.designsystem.component.MoruStatusGlyph
import app.thdev.moru.core.designsystem.component.MoruStatusGlyphKind
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.component.scroll.stableScrollAnchor
import app.thdev.moru.feature.workbench.identity.ProjectIdentityBadge
import app.thdev.moru.feature.workbench.i18n.workbenchStrings
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummary
import app.thdev.moru.feature.workbench.sidebar.SidebarSessionSummaryView
import app.thdev.moru.feature.workbench.sidebar.SidebarStateBadge

/**
 * One Project header: click selects, the disclosure toggles expansion.
 * The sessions still at work in a collapsed Project are listed below the header by [ProjectGroup], not here.
 */
@Composable
internal fun ProjectRowView(
    project: ProjectRow,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit = {},
    runningSessions: Int = 0,
    sessionSummary: SidebarSessionSummary = SidebarSessionSummary(),
    taskAttentionCount: Int = project.attentionCount,
    onRename: (String) -> Unit = {},
    onRelocate: () -> Unit = {},
    reorderHandle: (@Composable () -> Unit)? = null,
    /** Opens this Project in a window of its own; null hides the entry. */
    onOpenInWindow: (() -> Unit)? = null,
) {
    var confirmingClose by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val strings = workbenchStrings()
    val toggleDescription = if (project.expanded) strings.collapseProject(project.displayName) else strings.expandProject(project.displayName)
    val projectDescription = strings.projectDescription(project.displayName)
    val attentionDescription = strings.tasksNeedYou(taskAttentionCount, project.displayName)
    val badgeSize = 20.dp
    val rowAlpha by animateFloatAsState(
        targetValue = if (project.selected || sessionSummary.hasActivity) 1f else 0.84f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
    )
    val chevronRotation = remember { Animatable(0f) }
    var lastExpanded by remember { mutableStateOf(project.expanded) }
    LaunchedEffect(project.expanded) {
        if (lastExpanded != project.expanded) {
            lastExpanded = project.expanded
            chevronRotation.snapTo(if (project.expanded) -90f else 90f)
            chevronRotation.animateTo(0f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
        }
    }

    val requestClose = {
        if (runningSessions > 0) confirmingClose = true else onClose()
    }

    Column(Modifier.stableScrollAnchor("project:${project.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(rowAlpha)
                .pointerInput(project.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                menuOpen = true
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruIconButton(onClick = onToggle, description = toggleDescription, size = 18.dp,
                modifier = Modifier.alignBy { it.measuredHeight / 2 }) {
                MoruIcon(
                    name = if (project.expanded) MoruIconName.ChevronDown else MoruIconName.ChevronRight,
                    size = 12.dp,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation.value },
                )
            }
            ProjectIdentityBadge(
                name = project.displayName,
                modifier = Modifier.alignBy { it.measuredHeight / 2 },
                size = badgeSize,
                active = project.selected,
            )
            val rowInteractive = !renaming && !confirmingClose
            Column(
                Modifier.weight(1f).alignBy(ProjectTitleCenter)
                    .then(
                        if (rowInteractive) {
                            Modifier
                                .clickable(role = Role.Button, onClick = onSelect)
                                .semantics(mergeDescendants = true) {
                                    selected = project.selected
                                    contentDescription = projectDescription
                                }
                        } else {
                            Modifier.semantics {
                                selected = project.selected
                                contentDescription = projectDescription
                            }
                        }
                    )
                    .padding(vertical = 1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Box(Modifier.layout { measurable, constraints ->
                    val title = measurable.measure(constraints)
                    layout(title.width, title.height, mapOf(ProjectTitleCenter to title.height / 2)) { title.placeRelative(0, 0) }
                }) {
                    if (renaming) {
                        InlineNameEditor(
                            initial = project.displayName,
                            tag = "project-name",
                            onSave = { name -> renaming = false; onRename(name) },
                            onCancel = { renaming = false },
                        )
                    } else {
                        val titleStyle = if (project.selected) {
                            MoruTheme.typography.title.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
                        }
                        MoruText(
                            text = project.displayName,
                            style = titleStyle,
                            color = if (project.isAvailable) MoruTheme.colors.textPrimary else MoruTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!project.isAvailable || project.rootPath == null) {
                    MoruText(
                        text = if (!project.isAvailable) strings.folderUnavailable else strings.noFolderLinked,
                        style = MoruTheme.typography.caption,
                        color = MoruTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (confirmingClose) {
                    ProjectCloseConfirm(
                        runningSessions = runningSessions,
                        onConfirm = { confirmingClose = false; onClose() },
                        onKeepOpen = { confirmingClose = false },
                    )
                }
            }
            if (taskAttentionCount > 0) {
                Row(
                    modifier = Modifier
                        .alignBy { it.measuredHeight / 2 }
                        .semantics(mergeDescendants = true) {
                            contentDescription = attentionDescription
                        },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MoruStatusGlyph(kind = MoruStatusGlyphKind.NeedsYou, size = 10.dp)
                    MoruText(
                        text = taskAttentionCount.toString(),
                        style = MoruTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = MoruTheme.colors.warning,
                        maxLines = 1,
                    )
                }
            }
            if (!renaming) {
                if (reorderHandle != null) {
                    Box(Modifier.alignBy { it.measuredHeight / 2 }) { reorderHandle() }
                } else {
                    Box(Modifier.alignBy { it.measuredHeight / 2 }) {
                        MoruIconButton(
                            onClick = { menuOpen = true },
                            description = strings.projectMenu(project.displayName),
                            size = 20.dp,
                        ) {
                            MoruIcon(name = MoruIconName.Ellipsis, size = 12.dp)
                        }
                        MoruDropdownMenu(
                            expanded = menuOpen,
                            onDismiss = { menuOpen = false },
                            items = listOfNotNull(
                                onOpenInWindow?.let { open -> MoruMenuItem(strings.openInNewWindow) { open() } },
                                MoruMenuItem(strings.renameTask) { renaming = true },
                                MoruMenuItem(strings.changeProjectFolder) { onRelocate() },
                                MoruMenuItem(strings.closeProject(project.displayName)) { requestClose() },
                            ),
                        )
                    }
                }
            }
        }
        // Under the name, so a long Project name keeps its width.
        if (sessionSummary.awaitingReply > 0 || sessionSummary.responsesReady > 0) {
            Row(Modifier.padding(start = 28.dp, top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sessionSummary.awaitingReply > 0) {
                    SidebarStateBadge(strings.awaitingReply(sessionSummary.awaitingReply), MoruStatusGlyphKind.NeedsYou,
                        MoruTheme.colors.warning, Modifier.testTag("project-awaiting-reply:${project.id}"))
                }
                if (sessionSummary.responsesReady > 0) {
                    SidebarStateBadge(strings.finished(sessionSummary.responsesReady), MoruStatusGlyphKind.Done,
                        MoruTheme.colors.success, Modifier.testTag("project-finished:${project.id}"))
                }
            }
        }
        if (sessionSummary.total > 0) SidebarSessionSummaryView(
            sessionSummary,
            Modifier.fillMaxWidth().padding(start = 28.dp, end = 6.dp, top = 4.dp)
                .clickable(enabled = !renaming && !confirmingClose, role = Role.Button, onClick = onSelect)
                .testTag("project-session-summary:${project.id}"),
        )
    }
}

/** Secondary usage and warning lines never move the primary title's alignment anchor. */
private val ProjectTitleCenter = HorizontalAlignmentLine(::minOf)
