package app.thdev.moru.feature.notifications

import app.thdev.moru.core.designsystem.theme.UiLanguage

internal expect fun notificationCountLabel(count: Int, language: UiLanguage): String
