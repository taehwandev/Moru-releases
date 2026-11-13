package app.thdev.moru.app

import androidx.compose.runtime.LaunchedEffect
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window
import java.io.File
import javax.imageio.ImageIO
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

class ProjectToolbarChromeOsTest {
    @Test
    fun menuPageAndScopeChangesStayAtTheOpeningAnchorInAShortWindow() = ProjectToolbarFixture().use { fixture ->
        repeat(15) { index ->
            fixture.sessions.createSession(app.thdev.moru.core.terminal.TerminalSessionSpec(
                title = "Session $index", workingDirectory = fixture.alpha.canonicalPath,
                attachedItemId = fixture.taskA.id))
        }
        fixture.catalog.updateSelection(fixture.projectB.id, fixture.taskB.id)
        MoruPopupWindowPolicy.install().use { _ -> NativeWorkbenchTestWindow(800, 380, menu = {
            LaunchedEffect(Unit) { MoruWindowChrome.apply(window.rootPane) }
        }) { fixture.App(dark = true) }.use { ui ->
            ui.waitFor("Project selector") { ui.exists("Switch project") }
            ui.click("Switch project")
            ui.waitFor("Project menu") { ui.exists("Open projects") }
            Thread.sleep(250)
            val popup = onEdt { frame().ownedWindows.single { it.isShowing } }
            val anchor = onEdt { popup.location }
            assertPopupInsideOwner()
            // One round through every page and scope change; repeating the same round checked nothing new.
            run {
                ui.click("Sessions")
                ui.waitFor("Sessions groups") { ui.exists("Alpha task · 15") }
                assertMenuAnchor(popup, anchor)
                ui.hoverWithoutMovingPopover("This project")
                ui.click("This project")
                ui.waitFor("filtered Sessions") { !ui.exists("Alpha task · 15") }
                assertMenuAnchor(popup, anchor)
                ui.click("All projects")
                ui.waitFor("all Sessions") { ui.exists("Alpha task · 15") }
                assertMenuAnchor(popup, anchor)
                ui.click("‹ Projects")
                ui.waitFor("Project menu") { ui.exists("Open projects") }
                assertMenuAnchor(popup, anchor)
            }
            ui.click("Sessions")
            ui.waitFor("Sessions after resizing") { ui.exists("Alpha task · 15") }
            onEdt { frame().setSize(800, 320) }
            Thread.sleep(300)
            assertMenuAnchor(popup, anchor)
            capture("project-menu-short-native")
        } }
    }

    private fun assertMenuAnchor(popup: Window, anchor: java.awt.Point) {
        Thread.sleep(250)
        onEdt {
            assertSame(popup, frame().ownedWindows.single { it.isShowing })
            assertEquals(anchor, popup.location, "Menu content changes must retain the opening anchor")
        }
        assertPopupInsideOwner()
    }

    @Test
    fun projectMenuShowsBoundedProjectAndTaskGroupsForLongSessionLists() = ProjectToolbarFixture().use { fixture ->
        repeat(15) { index ->
            fixture.sessions.createSession(app.thdev.moru.core.terminal.TerminalSessionSpec(
                title = "Session $index", workingDirectory = fixture.alpha.canonicalPath,
                attachedItemId = fixture.taskA.id))
        }
        NativeWorkbenchTestWindow(800, 680, menu = {
            LaunchedEffect(Unit) { MoruWindowChrome.apply(window.rootPane) }
        }) { fixture.App(dark = true) }.use { ui ->
            ui.waitFor("Project menu") { ui.exists("Switch project") }
            onEdt { frame().isAlwaysOnTop = true; frame().toFront(); frame().requestFocus() }
            ui.click("Switch project")
            ui.click("Sessions")
            ui.waitFor("Project and Task groups") { ui.exists("Alpha · 15") && ui.exists("Alpha task · 15") }
            assertPopupInsideOwner()
            capture("project-menu-grouped-sessions-native")
        }
    }

    @Test
    fun projectAndSessionsShareOneMenuInsideTheNativeWindow() = ProjectToolbarFixture().use { fixture ->
        NativeWorkbenchTestWindow(800, 680, menu = {
            LaunchedEffect(Unit) { MoruWindowChrome.apply(window.rootPane) }
        }) { fixture.App(dark = true) }.use { ui ->
            ui.waitFor("toolbar") { ui.exists("Switch project") }
            onEdt { frame().isAlwaysOnTop = true; frame().toFront(); frame().requestFocus() }
            ui.click("Switch project")
            ui.waitFor("Project menu") { ui.exists("Open projects") }
            assertPopupInsideOwner()
            ui.hoverWithoutMovingPopover("Switch to project Beta")
            ui.click("Sessions")
            ui.waitFor("in-place empty Sessions") { ui.exists("‹ Projects") }
            assertPopupInsideOwner()
            ui.click("‹ Projects")
            ui.click("Switch to project Beta")
            ui.waitFor("Project selection") { fixture.selectedRoot == fixture.beta.canonicalPath && !ui.exists("Open projects") }
            assertEquals(fixture.projectB.id, fixture.catalog.loadCatalog().selectedProjectId)
            assertTrue(fixture.sessions.listSessions().isEmpty())
            ui.click("Switch project")
            ui.waitFor("Project menu capture") { ui.exists("Open projects") }
            capture("project-toolbar-menu-native")
        }
    }

    private fun assertPopupInsideOwner() = onEdt {
        val content = (frame() as RootPaneContainer).contentPane
        val owner = Rectangle(content.locationOnScreen, content.size)
        val popups = frame().ownedWindows.filter { it.isShowing }
        // The shared popover supports both an in-scene menu and a native popup window.
        assertTrue(popups.size <= 1, "Project and Sessions must share one popup")
        popups.singleOrNull()?.let { popup ->
            assertTrue(popup.width >= 260, "The menu must retain its content width: ${popup.bounds}")
            assertTrue(owner.contains(popup.bounds), "Popup must remain in its owner: ${popup.bounds} in $owner")
        }
    }

    private fun frame() = Window.getWindows().filterIsInstance<Frame>().single { it.title == "Task session test" && it.isShowing }

    private fun capture(name: String) {
        // Raising the owner while a popup is focused can dismiss it during the capture.
        val bounds = onEdt { frame().bounds }
        Thread.sleep(300)
        val output = File("build/ui-captures/$name.png")
        output.parentFile.mkdirs()
        ImageIO.write(Robot().createScreenCapture(bounds), "png", output)
    }

    private fun <T> onEdt(action: () -> T): T {
        val result = AtomicReference<Result<T>>()
        SwingUtilities.invokeAndWait { result.set(runCatching(action)) }
        return result.get().getOrThrow()
    }
}
