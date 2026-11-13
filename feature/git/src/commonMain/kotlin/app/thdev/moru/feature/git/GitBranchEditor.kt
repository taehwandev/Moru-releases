package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitBranchInfo

/** The name form the sidebar shows below its refs; nothing shows there until a menu asks for a new name. */
internal sealed interface GitBranchEditor {
    data object Closed : GitBranchEditor

    /** A new branch from HEAD or from [source]. */
    data class Create(val source: GitBranchInfo?) : GitBranchEditor
    data class Rename(val branch: GitBranchInfo) : GitBranchEditor
}
