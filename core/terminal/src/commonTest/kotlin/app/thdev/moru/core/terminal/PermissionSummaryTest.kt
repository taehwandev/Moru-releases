package app.thdev.moru.core.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionSummaryTest {
    @Test fun borderedHookRequestKeepsOnlyTheCommandInItsTargetAndBriefsTheEffect() {
        val command = "MORU_ALLOW_MAIN_CHECKOUT_EDIT=1 git -C /repo restore src/MoruMenuRow.kt src/NewSessionPopover.kt"
        val details = "Bash command\nRun shell command\n────────\n$command\n────────\nHook PreToolUse:Bash requires confirmation for this command:\nThe command carries the documented main-checkout exception\nsettings.json to update hooks"
        val summary = permissionSummary("Do you want to proceed?", details)
        assertEquals(command, summary.target)
        assertEquals("Run git restore\nMoruMenuRow.kt, NewSessionPopover.kt\nMain checkout exception requires confirmation.", permissionBrief(summary, details))
        assertEquals(161, permissionBrief(PermissionSummary(PermissionKind.Command, "x".repeat(400)), "").length)
    }
    @Test fun eachAgentsRequestIsItsKindAndTheOneThingItActsOn() {
        assertEquals(PermissionSummary(PermissionKind.Command, "npm run build && npm test"), permissionSummary(
            "Do you want to proceed?", "⏺ Let me build it.\nBash command\nnpm run build && npm test\nRun build and tests"))
        assertEquals(PermissionSummary(PermissionKind.Command, "rm -rf build"),
            permissionSummary("Would you like to run the following command?", "Reason: clean the output\n$ rm -rf build"))
        assertEquals(PermissionSummary(PermissionKind.Command, "npm install"), permissionSummary("Allow execution of: 'npm install'?", ""))
        assertEquals(PermissionSummary(PermissionKind.Edit, "Main.kt"), permissionSummary("Do you want to make this edit? ", "Update Main.kt"))
        assertEquals(PermissionSummary(PermissionKind.Edit, "src/ProfileLine.kt"), permissionSummary(
            "Do you want to make this edit to ProfileLine.kt?", "Edit file\nsrc/ProfileLine.kt\n12 - old\n12 + new"))
        assertEquals(PermissionSummary(PermissionKind.Create, "Notes.md"), permissionSummary("Do you want to create Notes.md?", ""))
        assertEquals(PermissionSummary(PermissionKind.Web, "https://example.com"),
            permissionSummary("Do you want to allow Claude to fetch this content?", "Fetch\nhttps://example.com\nClaude wants to fetch content"))
        assertEquals(PermissionSummary(PermissionKind.Command, "ls -la"), permissionSummary("Allow Bash?", "ls -la"))
    }

    @Test fun aLongCommandIsKeptWholeOnEveryLine() {
        val long = "git -C /Users/me/git/Moru log --oneline --graph --decorate --all --since=2026-01-01 --author=me"
        assertEquals("$long &&\ngit status --short", permissionSummary(
            "Do you want to proceed?", "Bash command\n$long &&\ngit status --short\nShow history and status").target)
        assertEquals("cat <<EOF > notes.md\nhello\nEOF", permissionSummary(
            "Would you like to run the following command?", "Reason: write notes\n$ cat <<EOF > notes.md\nhello\nEOF").target)
        assertEquals("rm -rf build\nnpm test", permissionSummary("Allow Bash?", "rm -rf build\nnpm test").target)
    }

    @Test fun aRequestMoruCannotReadHasNoTarget() {
        assertEquals(PermissionSummary(PermissionKind.Other, null), permissionSummary("Overwrite config.json? (y/N)", ""))
        assertEquals(null, permissionSummary("Do you want to proceed?", "Something unusual").target)
    }

    @Test fun choicesAreNamedByWhatTheyDo() {
        assertEquals(PermissionChoiceKind.Allow, permissionChoiceKind("Yes"))
        assertEquals(PermissionChoiceKind.Allow, permissionChoiceKind("Yes, proceed (y)"))
        assertEquals(PermissionChoiceKind.Allow, permissionChoiceKind("Allow once"))
        assertEquals(PermissionChoiceKind.AllowAlways, permissionChoiceKind("Yes, and don't ask again for npm run build commands in /repo"))
        assertEquals(PermissionChoiceKind.AllowAlways, permissionChoiceKind("Yes, allow all edits during this session (shift+tab)"))
        assertEquals(PermissionChoiceKind.AllowAlways, permissionChoiceKind("Allow for this session"))
        assertEquals(PermissionChoiceKind.Deny, permissionChoiceKind("No, and tell Claude what to do differently (esc)"))
        assertEquals(PermissionChoiceKind.Other, permissionChoiceKind("Maybe later"))
    }
}
