package app.thdev.moru.core.data

import androidx.room.Room
import java.io.File

/** The project's local database, in [folder] under the project root (a development build passes its own folder). */
fun defaultDesktopMoruDatabase(projectRoot: File, folder: String = ".moru-community"): MoruDatabase {
    val dataDir = projectRoot.resolve(folder).apply { mkdirs() }
    return createMoruDatabase(
        Room.databaseBuilder<MoruDatabase>(name = dataDir.resolve("moru.db").absolutePath),
    )
}
