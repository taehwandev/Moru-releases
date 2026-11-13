package app.thdev.moru.feature.git

import app.thdev.moru.core.model.git.GitRepositorySnapshot

/** Presentation options only; the native adapter still validates every selected ref. */
internal fun GitRepositorySnapshot.remoteTargets(remote: String): List<String> {
    if (remote !in remotes) return emptyList()
    val prefix = "$remote/"
    val discovered = branches.asSequence()
        .filter { it.isRemote && it.name.startsWith(prefix) && !it.name.endsWith("/HEAD") }
        .map { it.name.removePrefix(prefix) }.toList()
    return (discovered + listOfNotNull(
        upstreamBranch?.takeIf { upstreamRemote == remote },
        branchName,
    )).distinct().sorted()
}
