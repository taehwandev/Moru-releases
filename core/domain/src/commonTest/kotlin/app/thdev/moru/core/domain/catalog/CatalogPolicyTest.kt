package app.thdev.moru.core.domain.catalog

import app.thdev.moru.core.model.ProjectAvailability
import app.thdev.moru.core.model.TaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CatalogPolicyTest {
    private val policy = CatalogPolicy()
    private val empty = CatalogSnapshot(0, emptyList(), emptyList(), null, null)

    @Test
    fun closingAndReopeningPreserveStableMetadataAndRestoreTheLastTask() {
        val opened = open()
        val created = policy.createTask(opened, "p", "t", "v", "Fix login", 20).snapshot
        val composing = policy.composeTask(created, "p").snapshot
        assertNull(composing.selectedTaskId)
        assertEquals("t", composing.projects.single().lastTaskId)
        val restored = policy.selectProject(composing, "p").snapshot
        assertEquals("t", restored.selectedTaskId)
        val closed = policy.closeProject(restored, "p", 30).snapshot
        assertNull(closed.selectedProjectId)
        assertNull(closed.selectedTaskId)
        assertTrue(closed.projects.single().closed)
        assertEquals(created.tasks, closed.tasks)
        assertRejected(closed, "project_closed") { policy.createTask(it, "p", "new", "revision", "Task", 31) }
        val reopened = policy.openProject(closed, "ignored", "ignored-root", "Different name", "/project", 40).snapshot
        assertEquals("p", reopened.selectedProjectId)
        assertEquals("t", reopened.selectedTaskId)
        assertEquals("r", reopened.projects.single().rootId)
        assertEquals("Moru", reopened.projects.single().name)
        assertEquals(created.tasks, reopened.tasks)
        assertFalse(reopened.projects.single().closed)
        assertEquals(closed.generation + 1, reopened.generation)
    }

    @Test
    fun descriptionTitlesAndNumberedTitlesShareTheExistingRule() {
        var snapshot = open()
        snapshot = policy.createTask(snapshot, "p", "t1", "v1", " \nFirst line\nSecond line  ", 20).snapshot
        assertEquals("First line", snapshot.tasks.single().title)
        assertEquals("First line\nSecond line", snapshot.tasks.single().desiredOutcome)
        snapshot = policy.createTask(snapshot, "p", "t2", "v2", " ", 21).snapshot
        assertEquals("Moru task 1", snapshot.tasks.last().title)
        snapshot = policy.createTask(snapshot, "p", "t3", "v3", "", 22).snapshot
        assertEquals("Moru task 2", snapshot.tasks.last().title)
        val long = policy.createTask(snapshot, "p", "t4", "v4", "x".repeat(100), 23).snapshot
        assertEquals(80, long.tasks.last().title.length)
        assertEquals("Moru task 5", nextNumberedTaskTitle("Moru", listOf("Other task 9", "Moru task 4", "Moru task x")))
    }

    @Test
    fun renameNormalizesLineBreaksAndNoOpDoesNotAdvanceGeneration() {
        val initial = withTask()
        val project = policy.renameProject(initial, "p", "  A \r\n B  ", 30).snapshot
        assertEquals("A B", project.projects.single().name)
        assertEquals(initial.generation + 1, project.generation)
        val task = policy.renameTask(project, "t", "  Hello\nworld  ", 31).snapshot
        assertEquals("Hello world", task.tasks.single().title)
        assertEquals(project.generation + 1, task.generation)
        assertSame(task, policy.renameTask(task, "t", " Hello world ", 99).snapshot)
        assertEquals(80, policy.renameProject(task, "p", "p".repeat(100), 32).snapshot.projects.single().name.length)
        assertEquals(120, policy.renameTask(task, "t", "t".repeat(150), 32).snapshot.tasks.single().title.length)
        assertRejected(task, "invalid_name") { policy.renameTask(it, "t", "\n ", 33) }
    }

    @Test
    fun doneIsAnExplicitDecisionAndReopenDoesNotInventRuntimeProgress() {
        val working = withTask().let { it.copy(tasks = it.tasks.map { task -> task.copy(status = TaskStatus.Working) }) }
        assertSame(working, policy.setTaskDone(working, "t", false, 30).snapshot)
        val done = policy.setTaskDone(working, "t", true, 30).snapshot
        assertEquals(TaskStatus.Done, done.tasks.single().status)
        assertSame(done, policy.setTaskDone(done, "t", true, 31).snapshot)
        assertEquals(TaskStatus.Understanding, policy.setTaskDone(done, "t", false, 32).snapshot.tasks.single().status)
    }

    @Test
    fun selectionCannotCrossProjectOwnershipOrReferToMissingMetadata() {
        val first = withTask()
        val second = policy.openProject(first, "p2", "r2", "Other", "/other", 30).snapshot
        assertRejected(second, "invalid_owner") { policy.selectTask(it, "p2", "t") }
        assertRejected(second, "not_found") { policy.selectProject(it, "unknown") }
        assertEquals("invalid_selection", policy.validate(first.copy(selectedProjectId = null)))
        assertEquals("invalid_selection", policy.validate(first.copy(projects = first.projects.map { it.copy(lastTaskId = "missing") })))
        assertEquals("invalid_owner", policy.validate(first.copy(tasks = first.tasks.map { it.copy(projectId = "missing") })))
        assertEquals("invalid_identity", policy.validate(first.copy(tasks = first.tasks.map { it.copy(revisionId = "") })))
    }

    @Test
    fun duplicateIdentitiesAndRootPathsAreRejectedIncludingClosedProjects() {
        val initial = withTask()
        assertEquals("duplicate_identity", policy.validate(initial.copy(tasks = initial.tasks.map { it.copy(revisionId = "r") })))
        assertRejected(initial, "duplicate_identity") { policy.createTask(it, "p", "t", "other-v", "Other", 30) }
        val duplicate = initial.projects.single().copy(id = "p2", rootId = "r2", closed = true, lastTaskId = null)
        assertEquals("duplicate_path", policy.validate(initial.copy(projects = initial.projects + duplicate)))
        assertEquals("duplicate_identity", policy.validate(initial.copy(projects = initial.projects + duplicate.copy(path = "/other", rootId = "r"))))
    }

    @Test
    fun canonicalPathsAreLexicallyAbsoluteWithoutDotOrEmptySegments() {
        for (path in listOf("", "relative", "/a/../b", "/a/./b", "/a//b", "/a/", "/nul\u0000path")) {
            assertRejected(empty, "invalid_path") { policy.openProject(it, "p", "r", "Moru", path, 10) }
        }
        assertTrue(policy.openProject(empty, "p", "r", "", "/", 10).accepted)
        assertTrue(policy.openProject(empty, "p", "r", "", "/한글 folder", 10).accepted)
    }

    @Test
    fun generationOverflowAndStaleOrNegativeTimesNeverReplaceTheOriginal() {
        val initial = withTask().copy(generation = Long.MAX_VALUE)
        assertSame(initial, policy.selectProject(initial, "p").snapshot)
        assertRejected(initial, "generation_overflow") { policy.composeTask(it, "p") }
        assertRejected(initial, "invalid_timestamp") { policy.renameTask(it, "t", "New", -1) }
        val renamed = policy.renameTask(withTask(), "t", "New", 30).snapshot
        assertRejected(renamed, "stale_timestamp") { policy.renameTask(it, "t", "Older", 25) }
        assertEquals("invalid_generation", policy.validate(empty.copy(generation = -1)))
        assertEquals("invalid_timestamp", policy.validate(renamed.copy(tasks = renamed.tasks.map { it.copy(updatedAtMillis = 0) })))
    }

    @Test
    fun descriptionsAreBoundedByUtf8BytesAndAggregateTextHasItsOwnLimit() {
        val initial = open()
        assertTrue(policy.createTask(initial, "p", "t", "v", "한".repeat(21_845), 20).accepted)
        assertRejected(initial, "description_limit") { policy.createTask(it, "p", "t", "v", "한".repeat(21_846), 20) }
        val text = "x".repeat(65_536)
        fun snapshot(count: Int) = initial.copy(tasks = List(count) { index -> task("t$index", "v$index", text) })
        assertNull(policy.validate(snapshot(63)))
        assertEquals("text_limit", policy.validate(snapshot(64)))
    }

    @Test
    fun catalogCountBoundsAndAvailabilityAreValidatedWithoutIo() {
        val initial = open()
        val hundred = initial.copy(projects = List(100) { index ->
            initial.projects.single().copy(id = "p$index", rootId = "r$index", path = "/project$index", availability = ProjectAvailability.Missing)
        }, selectedProjectId = null)
        assertNull(policy.validate(hundred))
        assertEquals("catalog_limit", policy.validate(hundred.copy(projects = hundred.projects + hundred.projects.first())))
        val tasks = initial.copy(tasks = List(2_000) { task("t$it", "v$it", "") })
        assertNull(policy.validate(tasks))
        assertEquals("catalog_limit", policy.validate(tasks.copy(tasks = tasks.tasks + task("extra", "extra-v", ""))))
        assertNull(policy.validate(initial.copy(projects = initial.projects.map { it.copy(availability = ProjectAvailability.PermissionDenied) })))
    }

    @Test
    fun numberedTitleOverflowRejectsInsteadOfCreatingANegativeNumber() {
        val initial = withTask().let { it.copy(tasks = it.tasks.map { task -> task.copy(title = "Moru task ${Int.MAX_VALUE}") }) }
        assertRejected(initial, "title_overflow") { policy.createTask(it, "p", "t2", "v2", "", 30) }
    }

    @Test
    fun truncationPreservesWholeUnicodeScalarsWithinExistingCodeUnitLimits() {
        val emoji = "\uD83D\uDE00"
        val initial = withTask()
        val splitBoundary = "a".repeat(79) + emoji
        val created = policy.createTask(initial, "p", "t2", "v2", splitBoundary, 30)
        assertTrue(created.accepted)
        assertEquals("a".repeat(79), created.snapshot.tasks.last().title)
        assertEquals(splitBoundary, created.snapshot.tasks.last().desiredOutcome)
        assertEquals("a".repeat(79), policy.renameProject(initial, "p", splitBoundary, 30).snapshot.projects.single().name)
        val exactBoundary = "a".repeat(78) + emoji
        assertEquals(exactBoundary, policy.renameProject(initial, "p", exactBoundary + "x", 30).snapshot.projects.single().name)
        assertEquals("a".repeat(119), policy.renameTask(initial, "t", "a".repeat(119) + emoji, 30).snapshot.tasks.single().title)
        val exactTaskBoundary = "a".repeat(118) + emoji
        assertEquals(exactTaskBoundary, policy.renameTask(initial, "t", exactTaskBoundary + "x", 30).snapshot.tasks.single().title)
    }

    @Test
    fun truncationRemovesWhitespaceAtTheCutoffInsteadOfRejectingOrdinaryText() {
        val initial = withTask()
        val title = policy.createTask(initial, "p", "t2", "v2", "a".repeat(79) + " rest", 30)
        assertTrue(title.accepted)
        assertEquals("a".repeat(79), title.snapshot.tasks.last().title)
        val project = policy.renameProject(initial, "p", "p".repeat(79) + " rest", 30)
        assertTrue(project.accepted)
        assertEquals("p".repeat(79), project.snapshot.projects.single().name)
        val task = policy.renameTask(initial, "t", "t".repeat(119) + " rest", 30)
        assertTrue(task.accepted)
        assertEquals("t".repeat(119), task.snapshot.tasks.single().title)
    }

    @Test
    fun malformedUtf16IsRejectedBeforeNormalizationAndWhenRestoringMetadata() {
        val initial = withTask()
        for (malformed in listOf("\uD800", "\uDC00", "\uD800x", "\uDC00\uD800")) {
            assertRejected(initial, "invalid_text") { policy.createTask(it, "p", "t2", "v2", malformed, 30) }
            assertRejected(initial, "invalid_text") { policy.renameProject(it, "p", "p".repeat(100) + malformed, 30) }
            assertRejected(initial, "invalid_text") { policy.renameTask(it, "t", "t".repeat(150) + malformed, 30) }
            assertRejected(empty, "invalid_text") { policy.openProject(it, malformed, "r", "Moru", "/project", 10) }
            val restored = listOf(
                initial.copy(projects = initial.projects.map { it.copy(name = malformed) }),
                initial.copy(projects = initial.projects.map { it.copy(path = "/$malformed") }),
                initial.copy(tasks = initial.tasks.map { it.copy(revisionId = malformed) }),
                initial.copy(tasks = initial.tasks.map { it.copy(desiredOutcome = malformed) }),
                initial.copy(selectedTaskId = malformed),
            )
            restored.forEach { assertEquals("invalid_text", policy.validate(it)) }
        }
    }

    private fun open() = policy.openProject(empty, "p", "r", "Moru", "/project", 10).snapshot
    private fun withTask() = policy.createTask(open(), "p", "t", "v", "Task", 20).snapshot
    private fun task(id: String, revision: String, outcome: String) =
        CatalogTask(id, "p", revision, "Task", outcome, TaskStatus.Understanding, 20, 20)

    private fun assertRejected(snapshot: CatalogSnapshot, code: String, operation: (CatalogSnapshot) -> CatalogTransition) {
        val result = operation(snapshot)
        assertFalse(result.accepted)
        assertEquals(code, result.errorCode)
        assertSame(snapshot, result.snapshot)
    }
}
