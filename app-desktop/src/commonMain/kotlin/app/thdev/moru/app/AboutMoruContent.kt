package app.thdev.moru.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Local fixtures can render the entire About surface without app services. */
@Composable
internal fun AboutMoruContent(versionLabel: String, onClose: () -> Unit) {
    val colors = MoruTheme.colors
    val shape = RoundedCornerShape(MoruTheme.radius.xLarge)
    Column(
        Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(max = 560.dp)
            .testTag("about-moru").clip(shape).background(colors.background)
            .border(1.dp, colors.border, shape).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = .12f), colors.background)))
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MoruText("YOUR LOCAL WORKBENCH", style = MoruTheme.typography.caption.copy(letterSpacing = 0.16.em), color = colors.textSecondary)
            MoruText("Moru", style = MoruTheme.typography.display)
            MoruText("A little calm for big ideas.", style = MoruTheme.typography.body.copy(textAlign = TextAlign.Center), color = colors.textSecondary)
            MoruText(versionLabel, modifier = Modifier.testTag("about-version").clip(CircleShape)
                .background(colors.primary.copy(alpha = .10f)).padding(horizontal = 12.dp, vertical = 6.dp),
                style = MoruTheme.typography.caption.copy(textAlign = TextAlign.Center))
        }
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MoruText("Open a Project. Describe a Task.\nMake something worth keeping.",
                color = colors.textSecondary, style = MoruTheme.typography.body.copy(textAlign = TextAlign.Center))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp).heightIn(min = 1.dp).background(colors.border))
        MoruText("Made for curious humans.", style = MoruTheme.typography.caption.copy(textAlign = TextAlign.Center),
            color = colors.textSecondary)
        MoruButton("Close", onClose, Modifier.padding(top = 16.dp, bottom = 22.dp).testTag("about-close"), type = MoruButtonType.SECONDARY)
    }
}
