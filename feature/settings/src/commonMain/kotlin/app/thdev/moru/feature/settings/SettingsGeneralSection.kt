package app.thdev.moru.feature.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.core.designsystem.component.MoruSettingOptionSelector
import app.thdev.moru.core.designsystem.component.MoruSettingRow
import app.thdev.moru.core.designsystem.component.MoruSettingSection
import app.thdev.moru.core.designsystem.component.MoruSettingSwitchRow
import app.thdev.moru.core.model.AppSettings
import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsGeneralSection(
    settings: AppSettings,
    onChange: (edit: (AppSettings) -> AppSettings) -> Unit,
    /** Opens the first-run welcome again (workflow, look, icon, companion); null leaves the row out. */
    onShowOnboarding: (() -> Unit)? = null,
) {
    val strings = settingsStrings()
    MoruSettingSection(
        title = strings.generalTitle,
        description = strings.generalDescription,
        icon = MoruIconName.Sliders,
    ) {
        MoruSettingSwitchRow(
            title = strings.rememberStateTitle,
            description = strings.rememberStateDescription,
            checked = settings.general.rememberLastState,
            onCheckedChange = { onChange { s -> s.copy(general = s.general.copy(rememberLastState = it)) } },
            modifier = Modifier.testTag("settings-remember-state"),
        )
        MoruSettingSwitchRow(
            title = strings.autoCollapseSidebarTitle,
            description = strings.autoCollapseSidebarDescription,
            checked = settings.general.autoCollapseInactiveSidebar,
            onCheckedChange = { onChange { s -> s.copy(general = s.general.copy(autoCollapseInactiveSidebar = it)) } },
            modifier = Modifier.testTag("settings-auto-collapse-sidebar"),
        )
        MoruSettingRow(
            title = strings.taskSortOrderTitle,
            description = strings.taskSortOrderDescription,
            showDivider = false,
        ) {
            MoruSettingOptionSelector(
                options = TaskSortOrder.entries.map { it.name },
                selectedOption = settings.general.taskSortOrder.name,
                onOptionSelected = { option ->
                    val order = TaskSortOrder.valueOf(option)
                    onChange { it.copy(general = it.general.copy(taskSortOrder = order)) }
                },
                optionLabel = { strings.taskSortOrderLabel(TaskSortOrder.valueOf(it)) },
                modifier = Modifier.widthIn(max = 360.dp).testTag("settings-task-sort-order").semantics {
                    contentDescription = strings.taskSortOrderTitle
                    stateDescription = strings.taskSortOrderLabel(settings.general.taskSortOrder)
                },
                wrapInFlow = true,
            )
        }
        onShowOnboarding?.let { show ->
            MoruSettingRow(title = strings.onboardingTitle, description = strings.onboardingDescription, showDivider = false) {
                MoruButton(strings.onboardingOpen, onClick = show, type = MoruButtonType.SECONDARY, modifier = Modifier.testTag("settings-show-onboarding"))
            }
        }
    }
}
