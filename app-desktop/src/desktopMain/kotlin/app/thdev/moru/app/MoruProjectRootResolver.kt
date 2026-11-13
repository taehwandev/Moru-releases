package app.thdev.moru.app

import java.io.File

private const val ProjectRootProperty = "moru.projectRoot"
private const val ProjectRootEnvironment = "MORU_PROJECT_ROOT"

/** Resolve launch intent without inventing or registering a Project: only a folder the user named counts. */
internal fun resolveMoruProjectLaunch(
    arguments: Array<String> = emptyArray(),
    configuredPath: String? = System.getProperty(ProjectRootProperty)
        ?: System.getenv(ProjectRootEnvironment),
    userDir: File = File(System.getProperty("user.dir")),
    userHome: File = File(System.getProperty("user.home")),
): MoruProjectLaunch {
    val argumentPath = when {
        arguments.isEmpty() -> null
        arguments.size == 2 && arguments[0] == "--project" -> arguments[1]
        arguments.size == 1 && arguments[0].startsWith("--project=") ->
            arguments[0].substringAfter('=')
        arguments.size == 1 && !arguments[0].startsWith("--") -> arguments[0]
        else -> return MoruProjectLaunch(
            root = null,
            explicit = true,
            notice = "Open a folder with Moru <folder> or Moru --project <folder>.",
        )
    }
    val requestedPath = argumentPath ?: configuredPath
    if (requestedPath != null) {
        val expandedPath = when {
            requestedPath == "~" -> userHome.path
            requestedPath.startsWith("~/") -> userHome.resolve(requestedPath.drop(2)).path
            else -> requestedPath
        }
        val requested = File(expandedPath).let { file ->
            if (file.isAbsolute) file else userDir.resolve(file.path)
        }
        val root = requested.takeIf { requestedPath.isNotBlank() }?.readableCanonicalDirectory()
        return MoruProjectLaunch(
            root = root,
            explicit = true,
            notice = if (root == null) {
                "Cannot open project folder '$requestedPath'. Choose an existing, readable folder."
            } else null,
        )
    }

    // No folder was asked for: nothing is guessed. The working directory, the repository the app was built
    // from and the folder the app lives in say nothing about what the user wants to work on, so the caller
    // restores the last selected project or shows the welcome screen.
    return MoruProjectLaunch(root = null, explicit = false)
}

internal fun resolveMoruProjectRoot(
    configuredPath: String? = System.getProperty(ProjectRootProperty)
        ?: System.getenv(ProjectRootEnvironment),
    userDir: File = File(System.getProperty("user.dir")),
    userHome: File = File(System.getProperty("user.home")),
): File {
    val launch = resolveMoruProjectLaunch(
        configuredPath = configuredPath,
        userDir = userDir,
        userHome = userHome,
    )
    require(launch.notice == null) { launch.notice.orEmpty() }
    return launch.root ?: defaultMoruStorageRoot(userHome)
}
