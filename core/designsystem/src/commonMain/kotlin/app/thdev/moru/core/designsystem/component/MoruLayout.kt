package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.core.designsystem.theme.MoruTheme

@Composable
fun MoruScreen(
    modifier: Modifier = Modifier,
    padding: Dp? = null,
    verticalArrangement: Arrangement.Vertical? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedPadding = padding ?: MoruTheme.spacing.medium
    Column(
        modifier = modifier
            .fillMaxSize()
            .ckSurface(MoruSurfaceRole.Page)
            .padding(resolvedPadding),
        verticalArrangement = verticalArrangement ?: Arrangement.spacedBy(resolvedPadding),
        content = content,
    )
}

@Composable
fun MoruScreenHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    badges: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .ckSurface(MoruSurfaceRole.Card)
            .padding(horizontal = MoruTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MoruText(
                text = title,
                style = MoruTheme.typography.h1,
                maxLines = 1,
            )
            MoruText(
                text = description,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption,
                maxLines = 1,
            )
        }
        badges?.let { badgeContent ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                content = badgeContent,
            )
        }
        actions?.let { actionContent ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                content = actionContent,
            )
        }
    }
}

@Composable
fun MoruPane(
    title: String,
    detail: String,
    badgeText: String? = null,
    badgeType: MoruBadgeType = MoruBadgeType.NEUTRAL,
    modifier: Modifier = Modifier,
    role: MoruSurfaceRole = MoruSurfaceRole.Card,
    bodyPadding: Dp? = null,
    bodyWeight: Float? = null,
    bodyScrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.ckSurface(role),
    ) {
        MoruPaneHeader(
            title = title,
            detail = detail,
            badgeText = badgeText,
            badgeType = badgeType,
        )
        val resolvedPadding = bodyPadding ?: MoruTheme.spacing.medium
        var bodyModifier = Modifier
            .fillMaxWidth()
            .padding(resolvedPadding)
        if (bodyWeight != null) {
            bodyModifier = bodyModifier.weight(bodyWeight)
        }
        if (bodyScrollable) {
            bodyModifier = bodyModifier.verticalScroll(rememberScrollState())
        }
        Column(
            modifier = bodyModifier,
            verticalArrangement = Arrangement.spacedBy(resolvedPadding),
            content = content,
        )
    }
}

@Composable
fun MoruPaneHeader(
    title: String,
    detail: String,
    badgeText: String? = null,
    badgeType: MoruBadgeType = MoruBadgeType.NEUTRAL,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = MoruTheme.spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MoruText(
                text = title,
                style = MoruTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            MoruText(
                text = detail,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption,
                maxLines = 1,
            )
        }
        badgeText?.let { text ->
            MoruBadge(text = text, type = badgeType)
        }
    }
}

@Composable
fun MoruSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoruTheme.spacing.small),
    ) {
        MoruText(
            text = title.uppercase(),
            color = MoruTheme.colors.textSecondary,
            style = MoruTheme.typography.caption.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
        )
        content()
    }
}

@Composable
fun MoruMetricCard(
    label: String,
    value: String,
    detail: String,
    type: MoruBadgeType,
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
) {
    MoruSurfaceColumn(
        role = MoruSurfaceRole.Card,
        modifier = modifier
            .height(height),
        padding = MoruTheme.spacing.medium,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoruText(
                text = label,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            MoruStatusDot(type = type)
        }
        Column {
            MoruText(
                text = value,
                style = MoruTheme.typography.h1.copy(fontFamily = FontFamily.Monospace),
            )
            MoruText(
                text = detail,
                color = MoruTheme.colors.textSecondary,
                style = MoruTheme.typography.caption,
            )
        }
    }
}
