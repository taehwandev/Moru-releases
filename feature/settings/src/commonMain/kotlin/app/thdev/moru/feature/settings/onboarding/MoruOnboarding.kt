package app.thdev.moru.feature.settings.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.component.MoruButton
import app.thdev.moru.core.designsystem.component.MoruButtonType
import app.thdev.moru.core.designsystem.component.ckSurface
import app.thdev.moru.core.designsystem.theme.MoruSurfaceRole
import app.thdev.moru.feature.settings.SettingsEditorState
import app.thdev.moru.feature.settings.i18n.onboardingStrings

/**
 * The first-run welcome over the whole window, laid out like a setup assistant: a thin progress bar, one centred page
 * at a time sliding in from the side, and Skip, Back and Continue along the bottom. Each page makes one choice (the look), saved through [editor] as it is made so the app behind it changes at once.
 * Every page can be skipped; [onFinish] is called from the last page or from Skip setup.
 */
@Composable
fun MoruOnboarding(
    editor: SettingsEditorState,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenProject: (() -> Unit)? = null,
) {
    val strings = onboardingStrings()
    val steps = OnboardingStep.entries
    var index by remember { mutableStateOf(0) }
    val step = steps[index]
    Column(modifier = modifier.fillMaxSize().ckSurface(MoruSurfaceRole.Page).testTag("onboarding")) {
        OnboardingStepIndicator(steps, index, Modifier.align(Alignment.CenterHorizontally).padding(top = 28.dp))
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(260)) { w -> if (forward) w / 10 else -w / 10 } + fadeIn(tween(260)))
                    .togetherWith(slideOutHorizontally(tween(180)) { w -> if (forward) -w / 10 else w / 10 } + fadeOut(tween(180)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "onboarding-page",
        ) { at ->
            // Centred while it fits, scrolling when it does not.
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (steps[at]) {
                        OnboardingStep.Intro -> OnboardingIntroStep()
                        OnboardingStep.Theme -> OnboardingThemeStep(editor)
                        OnboardingStep.Done -> OnboardingDoneStep(onOpenProject?.let { open -> { onFinish(); open() } })
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step != OnboardingStep.Done) {
                MoruButton(strings.skipAll, onClick = onFinish, type = MoruButtonType.GHOST, modifier = Modifier.testTag("onboarding-skip"))
            }
            Spacer(Modifier.weight(1f))
            if (index > 0) {
                MoruButton(strings.back, onClick = { index-- }, type = MoruButtonType.GHOST, modifier = Modifier.testTag("onboarding-back"))
            }
            val last = index == steps.lastIndex
            MoruButton(
                text = when {
                    index == 0 -> strings.start
                    last -> strings.finish
                    else -> strings.next
                },
                onClick = { if (last) onFinish() else index++ },
                modifier = Modifier.testTag("onboarding-next"),
            )
        }
    }
}
