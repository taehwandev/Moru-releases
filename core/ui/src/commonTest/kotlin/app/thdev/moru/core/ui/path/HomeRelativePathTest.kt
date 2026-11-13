package app.thdev.moru.core.ui.path

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRelativePathTest {
    @Test
    fun aPathUnderHomeStartsWithTilde() {
        assertEquals("~/git/moru", homeRelativePath("/Users/me/git/moru", "/Users/me"))
    }

    @Test
    fun aPathOutsideHomeOrWithoutAHomeIsUnchanged() {
        assertEquals("/tmp/work", homeRelativePath("/tmp/work", "/Users/me"))
        assertEquals("/Users/me/git", homeRelativePath("/Users/me/git", ""))
    }
}
