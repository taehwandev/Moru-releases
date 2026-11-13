package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitBranchInfo

/** Local and remote branches may legally share a display name. */
internal val GitBranchInfo.selectionKey: String
    get() = "${if (isRemote) "remote" else "local"}:$name"

internal val GitBranchInfo.actionRef: String
    get() = "refs/${if (isRemote) "remotes" else "heads"}/$name"
