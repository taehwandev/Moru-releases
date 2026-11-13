package app.thdev.moru.feature.notifications

import app.thdev.moru.core.designsystem.theme.UiLanguage

internal expect fun notificationTimestampLabel(epochMillis: Long, language: UiLanguage): String
