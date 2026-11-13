package app.thdev.moru.core.terminal.drop

import java.awt.Image
import java.io.File

/** Saves a pasted or dropped picture as a PNG where terminal drops go, so an agent CLI can be handed its path. */
fun saveDroppedImage(image: Image): File = saveImage(image, defaultDropFolder())
