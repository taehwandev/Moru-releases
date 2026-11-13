package app.thdev.moru.feature.settings.i18n

import app.thdev.moru.core.model.AppLanguage
import app.thdev.moru.core.model.AppShortcut
import app.thdev.moru.core.model.AppIcon
import app.thdev.moru.core.model.AppTheme
import app.thdev.moru.core.model.TaskSortOrder
import app.thdev.moru.core.model.terminal.TerminalColorSlot
import app.thdev.moru.core.model.terminal.TerminalCursorStyle
import app.thdev.moru.core.model.terminal.TerminalImportItem

/** Text of the Settings screen. */
internal interface SettingsStrings {
    val title: String
    val subtitle: String
    val automaticSaveHint: String
    val savingMessage: String
    val savedMessage: String
    fun saveFailed(reason: String): String
    val unknownError: String
    val retryButton: String
    val invalidNumbersHint: String
    fun numberRange(min: Int, max: Int): String

    val updatesTitle: String
    val appUpdatesTitle: String
    val labSessionsTitle: String
    val labSessionsDescription: String
    val tasksBesideTitle: String
    val tasksBesideDescription: String
    val devicesAmongTerminalsTitle: String
    val devicesAmongTerminalsDescription: String
    val projectWindowsTitle: String
    val projectWindowsDescription: String
    val updatesDescription: String
    fun currentVersion(version: String): String
    val versionUnavailable: String
    val automaticUpdates: String
    val updatesUnavailable: String
    val checkForUpdates: String
    val restartToUpdate: String

    val generalTitle: String
    val generalDescription: String
    val rememberStateTitle: String
    val rememberStateDescription: String
    val autoCollapseSidebarTitle: String
    val autoCollapseSidebarDescription: String
    val taskSortOrderTitle: String
    val taskSortOrderDescription: String
    fun taskSortOrderLabel(order: TaskSortOrder): String
    val attentionTitle: String
    val attentionDescription: String
    val desktopNotificationsTitle: String
    val desktopNotificationsDescription: String

    val appearanceTitle: String
    val appearanceDescription: String
    val themeTitle: String
    val themeDescription: String
    val appleStyleTitle: String
    val appleStyleDescription: String
    val appIconTitle: String
    val onboardingTitle: String
    val onboardingDescription: String
    val onboardingOpen: String
    val appIconDescription: String
    fun appIconLabel(icon: AppIcon): String
    val languageTitle: String
    val languageDescription: String
    val fontTitle: String
    val fontSizeTitle: String
    val fontDescription: String
    val fontPlaceholder: String
    val uiFontTitle: String
    val uiFontSizeTitle: String
    val uiFontDescription: String
    val searchFonts: String
    val noFontsFound: String
    val fontPreview: String
    val resetFont: String
    fun decreaseFontSize(label: String): String
    fun increaseFontSize(label: String): String
    fun themeLabel(theme: AppTheme): String
    fun languageLabel(language: AppLanguage): String

    val terminalTitle: String
    val terminalDescription: String
    val shellTitle: String
    val shellDescription: String
    val scrollbackTitle: String
    val scrollbackDescription: String
    val optionKeyTitle: String
    val optionKeyDescription: String
    val terminalImportTitle: String
    val terminalImportDescription: String
    val terminalImportAction: String
    val terminalImporting: String
    val terminalLookTitle: String
    val terminalLookDescription: String
    val terminalSchemeTitle: String
    val terminalSchemeDescription: String
    val terminalSchemeLight: String
    val terminalSchemeDark: String
    val terminalSchemeMoru: String
    val terminalSchemeCustom: String
    val terminalColorsTitle: String
    val terminalColorsDescription: String
    val terminalColorUseMoru: String
    fun terminalColorSlot(slot: TerminalColorSlot): String
    val terminalCursorTitle: String
    val terminalCursorDescription: String
    fun terminalCursorStyle(style: TerminalCursorStyle): String
    val terminalCursorBlinkTitle: String
    val terminalCursorBlinkDescription: String
    val terminalLineSpacingTitle: String
    val terminalLineSpacingDescription: String
    val terminalLookResetTitle: String
    val terminalLookResetDescription: String
    val terminalLookResetAction: String
    fun terminalImportUsing(source: String): String
    val terminalImportNotFound: String
    fun terminalImportFailed(reason: String): String
    fun terminalImportApplied(items: String): String
    fun terminalImportSkipped(items: String): String
    fun terminalImportItem(item: TerminalImportItem): String

    val agentClisTitle: String
    val agentClisDescription: String
    val agentClisLooking: String
    val noAgentCliTitle: String
    val noAgentCliDescription: String
    val cliUpdatedOutsideMoru: String
    val cliUpdate: String
    val cliUpdating: String
    fun cliUpdated(version: String): String
    fun cliAlreadyLatest(version: String): String
    fun cliUpdateFailed(detail: String?, command: String): String

    val shortcutsTitle: String
    val shortcutsDescription: String
    fun shortcutLabel(shortcut: AppShortcut): String
    val shortcutNone: String
    val shortcutWaiting: String
    fun shortcutReserved(chord: String): String
    val shortcutReset: String
    fun shortcutMoved(from: String, to: String): String
}
