package app.thdev.moru.core.model

import kotlinx.serialization.Serializable

/** The order of a folder's entries: by name, or by kind (the extension) and then name. */
@Serializable
enum class CodeExplorerSort { Name, Type }
