package app.thdev.moru.core.data.git

import java.nio.file.Files
import java.nio.file.Path

internal data class GitRepositoryDiscovery(val root: Path, val gitDirectory: Path, val commonDirectory: Path) {
    companion object {
        fun read(workspace: String, runner: GitProcessRunner): GitRepositoryDiscovery {
            val folder = Path.of(workspace)
            require(Files.isDirectory(folder)) { "The selected folder is missing or is not a directory." }
            val canonical = folder.toRealPath()
            fun path(vararg args: String): Path {
                val result = runner.run(canonical, args.toList())
                result.requireSuccess()
                return Path.of(result.text()).toRealPath()
            }
            return GitRepositoryDiscovery(path("rev-parse", "--show-toplevel"),
                path("rev-parse", "--absolute-git-dir"),
                path("rev-parse", "--path-format=absolute", "--git-common-dir"))
        }
    }
}
