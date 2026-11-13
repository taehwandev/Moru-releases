package app.thdev.moru.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.MoruText
import app.thdev.moru.core.designsystem.theme.MoruTheme
import app.thdev.moru.core.designsystem.theme.systemFontFamily
import app.thdev.moru.feature.settings.i18n.settingsStrings

@Composable
internal fun SettingsFontControls(
    family: String?,
    families: List<String>,
    size: Int,
    sizeText: String,
    label: String,
    sizeLabel: String,
    tag: String,
    defaultFamily: FontFamily,
    onFamilyChange: (String?) -> Unit,
    onSizeChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    val strings = settingsStrings()
    val shape = RoundedCornerShape(MoruTheme.radius.small)
    Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsFontPicker(family, families, label, "$tag-family", onFamilyChange, Modifier.weight(1f))
            MoruButton(
                strings.resetFont, onReset, type = MoruButtonType.SECONDARY,
                modifier = Modifier.testTag("$tag-reset").semantics { contentDescription = "${strings.resetFont}: $label" },
            )
        }
        SettingsFontSizeControl(sizeText, sizeLabel, "$tag-size", onSizeChange)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MoruTheme.colors.background)
                .border(MoruTheme.stroke.thin, MoruTheme.colors.border, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            MoruText(
                text = strings.fontPreview,
                style = MoruTheme.typography.body.copy(
                    fontFamily = family?.let { systemFontFamily(it) } ?: defaultFamily,
                    fontSize = size.coerceIn(10..28).sp,
                    lineHeight = (size.coerceIn(10..28) * 1.4f).sp,
                ),
                modifier = Modifier.testTag("$tag-preview"),
            )
        }
    }
}
