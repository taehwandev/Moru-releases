package app.thdev.moru.feature.terminal

import app.thdev.moru.core.terminal.drop.TerminalDropReader
import app.thdev.moru.core.terminal.drop.uriListFlavor
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.TransferHandler

/** Accepts files, images, links and text dropped on a terminal and pastes them through [reader]. */
internal class TerminalDropHandler(private val reader: TerminalDropReader) : TransferHandler() {

    constructor(paste: (String) -> Unit) : this(TerminalDropReader(paste))

    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDrop || support.dataFlavors.none { it.isDroppable() }) return false
        if (support.sourceDropActions and COPY != 0) support.dropAction = COPY
        return true
    }

    override fun importData(support: TransferSupport): Boolean =
        canImport(support) && runCatching { importFrom(support.transferable) }.getOrDefault(false)

    internal fun importFrom(transferable: Transferable): Boolean = reader.read(transferable)

    private fun DataFlavor.isDroppable(): Boolean =
        this == DataFlavor.javaFileListFlavor || this == DataFlavor.imageFlavor || this == uriListFlavor ||
            isFlavorTextType
}
