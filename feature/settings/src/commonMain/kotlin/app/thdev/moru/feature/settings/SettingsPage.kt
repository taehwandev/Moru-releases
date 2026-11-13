package app.thdev.moru.feature.settings

import app.thdev.moru.core.designsystem.component.MoruIconName
import app.thdev.moru.feature.settings.i18n.SettingsStrings

/** One page of Settings, listed on the left in this order; its name is what the workbench layout remembers. */
enum class SettingsPage(internal val icon: MoruIconName) {
    General(MoruIconName.Sliders),
    Appearance(MoruIconName.Palette),
    Terminal(MoruIconName.Terminal),
    Attention(MoruIconName.Bell),
    Shortcuts(MoruIconName.Code),
    Updates(MoruIconName.Download),
    ;

    internal fun title(strings: SettingsStrings): String = when (this) {
        General -> strings.generalTitle
        Appearance -> strings.appearanceTitle
        Terminal -> strings.terminalTitle
        Attention -> strings.attentionTitle
        Shortcuts -> strings.shortcutsTitle
        Updates -> strings.updatesTitle
    }

    companion object {
        /** Restores remembered pages, including the former Agent CLIs page now inside Updates. */
        fun named(name: String?): SettingsPage = when (name) {
            "AgentClis" -> Updates
            else -> entries.firstOrNull { it.name == name } ?: General
        }
    }
}
