package app.thdev.moru.core.terminal.iterm

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Reads an XML property list into plain values: dict → Map, array → List, string, real → Double, integer → Long,
 * true/false → Boolean. Data and date values become null; nothing Moru imports uses them.
 */
internal object PlistXml {
    fun parse(xml: String): Any? {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // The plist DOCTYPE names Apple's DTD; never fetch it or anything else.
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val root = document.documentElement.childElements().firstOrNull() ?: return null
        return value(root)
    }

    private fun value(element: Element): Any? = when (element.tagName) {
        "dict" -> {
            val children = element.childElements()
            buildMap {
                children.chunked(2).forEach { pair ->
                    if (pair.size == 2 && pair[0].tagName == "key") put(pair[0].textContent, value(pair[1]))
                }
            }
        }
        "array" -> element.childElements().map(::value)
        "string" -> element.textContent
        "real" -> element.textContent.trim().toDoubleOrNull()
        "integer" -> element.textContent.trim().toLongOrNull()
        "true" -> true
        "false" -> false
        else -> null
    }

    private fun Element.childElements(): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
    }
}
