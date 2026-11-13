package app.thdev.moru.feature.git.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruColors
import app.thdev.moru.core.designsystem.theme.darkCkColors
import app.thdev.moru.core.designsystem.theme.lightCkColors
import app.thdev.moru.feature.git.history.CommitDiffPane
import app.thdev.moru.core.model.git.history.GitCommit
import app.thdev.moru.core.model.git.diff.CommitDiffResult
import app.thdev.moru.core.data.git.workbench.parseGitDiff
import kotlin.test.Test

/** Pins the commit diff pane: a real-looking diff in both themes, plus loading and failure. */
class CommitDiffScreenshotTest {
    private val comparator = GoldenImageComparator()

    private val commit = GitCommit(
        hash = "9f3c2a71d4e85b06a1c7e2f4b8d09a35c6e1f720",
        parents = listOf("1a2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d"),
        authorName = "Test Author",
        authorEmail = "test@example.com",
        epochSeconds = 0,
        refs = emptyList(),
        subject = "fix(login): show the error message when sign-in fails",
    )

    private val loaded = CommitDiffResult.Loaded(files = parseGitDiff(SAMPLE_PATCH), truncated = false)

    @Test
    fun diffInLightTheme() = assertPane("commit_diff_light", lightCkColors, loaded)

    @Test
    fun diffInDarkTheme() = assertPane("commit_diff_dark", darkCkColors, loaded)

    @Test
    fun whileLoading() = assertPane("commit_diff_loading_light", lightCkColors, null)

    @Test
    fun whenGitFails() = assertPane(
        "commit_diff_unavailable_light",
        lightCkColors,
        CommitDiffResult.Unavailable("fatal: bad object deadbeef"),
    )

    private fun assertPane(name: String, colors: MoruColors, diff: CommitDiffResult?) =
        assertThemedScreenshot(comparator, name, colors, windowWidth = 940, windowHeight = 500) {
            CommitDiffPane(commit = commit, diff = diff, modifier = Modifier.width(900.dp).height(460.dp))
        }
}

internal val SAMPLE_PATCH = """
diff --git a/feature/login/LoginViewModel.kt b/feature/login/LoginViewModel.kt
index 1111111..2222222 100644
--- a/feature/login/LoginViewModel.kt
+++ b/feature/login/LoginViewModel.kt
@@ -12,7 +12,11 @@ class LoginViewModel(
     fun signIn(email: String, password: String) {
         viewModelScope.launch {
-            val result = repository.signIn(email, password)
-            _state.value = LoginState.Done
+            when (val result = repository.signIn(email, password)) {
+                is SignInResult.Success -> _state.value = LoginState.Done
+                is SignInResult.Failure -> _state.value = LoginState.Error(result.message)
+            }
         }
     }
@@ -40,3 +44,3 @@ class LoginViewModel(
     private fun validate(email: String): Boolean {
-        return email.isNotBlank()
+        return email.isNotBlank() && '@' in email
     }
diff --git a/feature/login/LoginScreen.kt b/feature/login/LoginScreen.kt
new file mode 100644
index 0000000..3333333
--- /dev/null
+++ b/feature/login/LoginScreen.kt
@@ -0,0 +1,3 @@
+@Composable
+fun LoginScreen(state: LoginState) {
+}
diff --git a/docs/old-login.md b/docs/login.md
similarity index 100%
rename from docs/old-login.md
rename to docs/login.md
diff --git a/assets/logo.png b/assets/logo.png
index 5555555..6666666 100644
Binary files a/assets/logo.png and b/assets/logo.png differ
""".trimStart()
