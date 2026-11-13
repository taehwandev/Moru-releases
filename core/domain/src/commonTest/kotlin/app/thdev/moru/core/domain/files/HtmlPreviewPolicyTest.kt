package app.thdev.moru.core.domain.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlPreviewPolicyTest {
    @Test fun htmlClassificationPreservesFeatureNamesButMimeStillRequiresAnExtension() {
        for (path in listOf("index.html", "page.HTM", "pages/index.HTML", "html", "HTM", ".html")) {
            assertTrue(HtmlPreviewPolicy.isHtmlPath(path), path)
        }
        for (path in listOf("", "folder/html", "folder.html/no-extension", "page.xhtml", "page.html?query")) {
            assertFalse(HtmlPreviewPolicy.isHtmlPath(path), path)
        }
        assertNull(HtmlPreviewPolicy.mimeType("html"))
        assertNull(HtmlPreviewPolicy.mimeType("htm"))
        assertFalse(HtmlPreviewPolicy.isAllowedPath(".html"))
    }

    @Test fun decodedRelativeNamesKeepLiteralUnicodeSpacesPercentPlusAndColon() {
        for (path in listOf("index.html", "nested/page.HTM", "a +% 한글/page %25+.HTM", "C:/page.html",
            "dir:name/asset.css", " space /file.css", "line\nname.css", "%2e%2e/index.html", "file.unknown")) {
            assertTrue(HtmlPreviewPolicy.isAllowedPath(path), path)
        }
        assertNull(HtmlPreviewPolicy.mimeType("file.unknown"))
    }

    @Test fun absoluteBackslashNulEmptyDotAndHiddenSegmentsAreRejectedWithoutNormalization() {
        for (path in listOf("", "/", "/page.html", "//page.html", "dir//page.html", "dir/page.html/",
            "./index.html", "dir/../page.html", "../page.html", ".hidden.css", "dir/.private/asset.css",
            "dir/.", "dir/..", "dir\\asset.css", "C:\\page.html", "bad\u0000.css")) {
            assertFalse(HtmlPreviewPolicy.isAllowedPath(path), path)
        }
        // MIME lookup is independent of lexical permission and never makes a hidden path readable.
        assertEquals("text/css; charset=utf-8", HtmlPreviewPolicy.mimeType(".hidden.css"))
    }

    @Test fun allThirtyFiveExistingAssetExtensionsKeepExactMimeValues() {
        val groups = mapOf(
            "html htm" to "text/html; charset=utf-8", "css" to "text/css; charset=utf-8",
            "js mjs" to "text/javascript; charset=utf-8", "json" to "application/json; charset=utf-8",
            "txt" to "text/plain; charset=utf-8", "wasm" to "application/wasm", "png" to "image/png",
            "jpg jpeg" to "image/jpeg", "gif" to "image/gif", "svg" to "image/svg+xml", "webp" to "image/webp",
            "avif" to "image/avif", "bmp" to "image/bmp", "ico" to "image/x-icon", "apng" to "image/apng",
            "woff" to "font/woff", "woff2" to "font/woff2", "ttf" to "font/ttf", "otf" to "font/otf",
            "eot" to "application/vnd.ms-fontobject", "mp3" to "audio/mpeg", "wav" to "audio/wav",
            "ogg oga" to "audio/ogg", "m4a" to "audio/mp4", "aac" to "audio/aac", "flac" to "audio/flac",
            "mp4 m4v" to "video/mp4", "webm" to "video/webm", "ogv" to "video/ogg", "mov" to "video/quicktime",
        )
        assertEquals(35, groups.keys.sumOf { it.split(' ').size })
        for ((extensions, mime) in groups) for (extension in extensions.split(' ')) {
            assertEquals(mime, HtmlPreviewPolicy.mimeType("assets/file.$extension"))
            assertEquals(mime, HtmlPreviewPolicy.mimeType("assets/file.${extension.uppercase()}"))
        }
        for (path in listOf("", "file", "folder.css/file", "folder/", "file.kt", "file.xhtml", "file.txt?raw=1")) {
            assertNull(HtmlPreviewPolicy.mimeType(path), path)
        }
    }

    @Test fun acquisitionLimitsRemainTheExistingByteBounds() {
        assertEquals(1_048_576, HtmlPreviewPolicy.maximumHtmlBytes)
        assertEquals(16_777_216, HtmlPreviewPolicy.maximumAssetBytes)
    }
}
