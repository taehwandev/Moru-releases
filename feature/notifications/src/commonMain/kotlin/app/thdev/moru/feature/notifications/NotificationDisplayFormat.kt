package app.thdev.moru.feature.notifications

import androidx.compose.runtime.Composable
import app.thdev.moru.core.designsystem.theme.LocalUiLanguage

/** Counts follow the app's selected language, including thousands grouping. */
@Composable
fun formatNotificationCount(count: Int): String = notificationCountLabel(count, LocalUiLanguage.current)
