package app.thdev.moru.core.model

import app.thdev.moru.core.model.terminal.TerminalProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Settings the app actually reads. Anything not listed here is not a setting: older
 * files may still carry removed keys, which the repository ignores on load and drops
 * on the next save.
 */
@Serializable
data class AppSettings(
    @SerialName("schema_version")
    val schemaVersion: Int = 1,
    val general: GeneralSettings = GeneralSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val terminal: TerminalSettings = TerminalSettings(),
    val window: WindowSettings = WindowSettings(),
    val attention: AttentionSettings = AttentionSettings(),
    val keyboard: KeyboardSettings = KeyboardSettings(),
    @SerialName("agent_workflows")
    val workflows: AgentWorkflowSettings = AgentWorkflowSettings(),
    val experimental: ExperimentalSettings = ExperimentalSettings(),
)

@Serializable
data class GeneralSettings(
    @SerialName("remember_last_state")
    val rememberLastState: Boolean = true,
    @SerialName("task_sort_order")
    val taskSortOrder: TaskSortOrder = TaskSortOrder.OldestFirst,
    /** Display ids only; the catalog remains authoritative for Project and Task ownership. */
    @SerialName("project_order")
    val projectOrder: List<String> = emptyList(),
    @SerialName("task_order")
    val taskOrder: Map<String, List<String>> = emptyMap(),
    /** Auto-collapse inactive Projects and idle Task sessions in the sidebar when switching focus. */
    @SerialName("auto_collapse_inactive_sidebar")
    val autoCollapseInactiveSidebar: Boolean = false,
    /** The first-run welcome was finished or skipped; it opens again only from Settings › General. */
    @SerialName("onboarding_done")
    val onboardingDone: Boolean = false,
)

@Serializable
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.System,
    @SerialName("apple_style")
    val appleStyle: Boolean = false,
    // Stored as `ui_language` so it never collides with the removed `general.language` key older files carry.
    @SerialName("ui_language")
    val language: AppLanguage = AppLanguage.System,
    // Separate from the removed, inactive `ui_font_*` keys in legacy settings files.
    @SerialName("ide_font_family")
    val uiFontFamily: String? = null,
    @SerialName("ide_font_size")
    val uiFontSize: Int = 14,
    @SerialName("terminal_font_family")
    val terminalFontFamily: String? = null,
    @SerialName("terminal_font_size")
    val terminalFontSize: Int = 13,
    /** The [AppIcon] shown in the Dock, by its id; an id this build does not know is the default icon. */
    @SerialName("app_icon")
    val appIcon: String = AppIcon.Moru.id,
)

@Serializable
enum class AppTheme {
    @SerialName("system")
    System,

    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,

    @SerialName("fresh_mint")
    FreshMint,

    @SerialName("github_light")
    GitHubLight,

    @SerialName("github_dark")
    GitHubDark,

    @SerialName("catppuccin_latte")
    CatppuccinLatte,

    @SerialName("catppuccin_mocha")
    CatppuccinMocha,

    @SerialName("tokyo_night")
    TokyoNight,
}

@Serializable
data class TerminalSettings(
    @SerialName("shell_command")
    val shellCommand: String? = null,
    @SerialName("scrollback_lines")
    val scrollbackLines: Int = 5000,
    /** The terminal look the user set or imported; null keeps Moru's. */
    @SerialName("profile")
    val profile: TerminalProfile? = null,
    /** Option (Alt) sends Escape before the key, as shells and agents expect for word moves; off types the character macOS composes. */
    @SerialName("option_sends_escape")
    val optionSendsEscape: Boolean = true,
)

@Serializable
data class TerminalWorkspaceSettings(
    val id: String = DefaultTerminalWorkspaceId,
    val name: String = DefaultTerminalWorkspaceName,
)

data class TerminalWorkspaceState(
    val workspaces: List<TerminalWorkspaceSettings> = listOf(TerminalWorkspaceSettings()),
    val selectedWorkspaceId: String = DefaultTerminalWorkspaceId,
)

const val DefaultTerminalWorkspaceId = "workspace-1"
const val DefaultTerminalWorkspaceName = "Workspace 1"

@Serializable
data class WindowSettings(
    @SerialName("width_dp")
    val widthDp: Int = 1440,
    @SerialName("height_dp")
    val heightDp: Int = 900,
    @SerialName("x_dp")
    val xDp: Int? = null,
    @SerialName("y_dp")
    val yDp: Int? = null,
    val maximized: Boolean = false,
)
