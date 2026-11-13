package app.thdev.moru.feature.settings.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.feature.settings.i18n.onboardingStrings

/** The end: where things are, and a way to open the first Project; [onOpenProject] null hides it. */
@Composable
internal fun OnboardingDoneStep(onOpenProject: (() -> Unit)?) {
    val strings = onboardingStrings()
    OnboardingPageHeader(strings.doneTitle, strings.doneBody)
    onOpenProject?.let {
        MoruButton(strings.openProject, onClick = it, type = MoruButtonType.SECONDARY, modifier = Modifier.testTag("onboarding-open-project"))
    }
}
