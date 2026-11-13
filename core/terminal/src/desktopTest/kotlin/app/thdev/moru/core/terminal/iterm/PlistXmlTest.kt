package app.thdev.moru.core.terminal.iterm

import kotlin.test.Test
import kotlin.test.assertEquals

class PlistXmlTest {
    @Test
    fun readsNestedDictsArraysAndScalarsWithoutFetchingTheDoctype() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Name</key><string>Default</string>
                <key>Spacing</key><real>1.25</real>
                <key>Lines</key><integer>1000</integer>
                <key>Blink</key><true/>
                <key>Blur</key><false/>
                <key>Stamp</key><date>2026-10-10T00:00:00Z</date>
                <key>List</key><array><string>a</string><dict><key>k</key><integer>2</integer></dict></array>
            </dict>
            </plist>
        """.trimIndent()

        assertEquals(
            mapOf(
                "Name" to "Default", "Spacing" to 1.25, "Lines" to 1000L, "Blink" to true, "Blur" to false,
                "Stamp" to null, "List" to listOf("a", mapOf("k" to 2L)),
            ),
            PlistXml.parse(xml),
        )
    }
}
