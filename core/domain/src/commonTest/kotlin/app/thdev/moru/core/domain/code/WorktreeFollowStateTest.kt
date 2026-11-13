package app.thdev.moru.core.domain.code

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class WorktreeFollowStateTest {
    private val main = "/repo"
    private val a = "/repo/worktrees/a"
    private val b = "/repo/worktrees/b"
    private val all = listOf(main, a, b)

    @Test fun aTerminalOnlyInTheProjectFolderShowsMain() {
        assertEquals(WorktreeFollowState(main, null), WorktreeFollowState().observe(listOf(main), all, main))
    }

    @Test fun aSeenNonMainCheckoutStaysAfterItsCommandExits() {
        val followed = WorktreeFollowState().observe(listOf(main, a), all, main)
        assertEquals(WorktreeFollowState(a, a), followed)
        assertEquals(followed, followed.observe(listOf(main), all, main))
        assertEquals(followed, followed.observe(emptyList(), all, main))
    }

    @Test fun aManualPickHoldsUntilANewNonMainCheckoutIsObserved() {
        val picked = WorktreeFollowState().observe(listOf(a), all, main).pick(main)
        assertEquals(WorktreeFollowState(main, a), picked)
        assertEquals(picked, picked.observe(listOf(main, a), all, main))
        assertEquals(picked, picked.observe(listOf(main), all, main))
        assertEquals(WorktreeFollowState(b, b), picked.observe(listOf(main, b), all, main))
    }

    @Test fun aRemovedCheckoutFallsBackAndClearsTheRemovedSeenRoot() {
        val followed = WorktreeFollowState(a, a)
        assertEquals(WorktreeFollowState(main, null), followed.observe(listOf(a), listOf(main, b), main))
    }

    @Test fun unknownProcessFoldersAreIgnored() {
        assertEquals(WorktreeFollowState(main, null), WorktreeFollowState().observe(listOf("/elsewhere"), all, main))
    }

    @Test fun emptyInventoryRetainsBothFieldsEvenWhenRootsAppearToMove() {
        val picked = WorktreeFollowState(main, a)
        assertSame(picked, picked.observe(listOf(b), emptyList(), null))
    }

    @Test fun aSeenCheckoutWinsOverRootReorderingWhileNewStateUsesTheFirstKnownRoot() {
        assertEquals(WorktreeFollowState(b, b), WorktreeFollowState().observe(listOf("/outside", b, a), all, main))
        assertEquals(WorktreeFollowState(main, b), WorktreeFollowState(main, b).observe(listOf(a, main, b), all, main))
    }

    @Test fun missingMainKeepsAValidChoiceOrUsesTheCallersNullableFallback() {
        assertEquals(WorktreeFollowState(), WorktreeFollowState().observe(emptyList(), listOf(a, b), null))
        assertEquals(WorktreeFollowState(a, a), WorktreeFollowState(a, a).observe(emptyList(), listOf(a, b), null))
        assertEquals(WorktreeFollowState("/caller-main", null),
            WorktreeFollowState().observe(emptyList(), listOf(a), "/caller-main"))
    }

    @Test fun picksRetainSeenAndAreRevalidatedOnlyAgainstANonemptyInventory() {
        val picked = WorktreeFollowState(a, a).pick("/manual")
        assertEquals(WorktreeFollowState("/manual", a), picked)
        assertSame(picked, picked.observe(emptyList(), emptyList(), main))
        assertEquals(WorktreeFollowState(main, a), picked.observe(emptyList(), all, main))
    }

    @Test fun duplicateAndUnicodeRootsRemainOpaqueExactStrings() {
        val path = "/repo/worktrees/😀 space\nname"
        val paths = listOf(main, path, path)
        assertEquals(WorktreeFollowState(path, path), WorktreeFollowState().observe(listOf(path, path), paths, main))
        assertEquals(WorktreeFollowState(main, null), WorktreeFollowState().observe(listOf(path.uppercase()), paths, main))
    }
}
