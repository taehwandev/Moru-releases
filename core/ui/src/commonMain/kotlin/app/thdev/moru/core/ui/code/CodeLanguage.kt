package app.thdev.moru.core.ui.code

import app.thdev.moru.core.domain.code.CodeLanguage as SharedCodeLanguage

/** Public Compose compatibility enum; extension selection and reader rules live in the shared policy. */
enum class CodeLanguage(internal val policy: SharedCodeLanguage) {
    Kotlin(SharedCodeLanguage.Kotlin),
    Java(SharedCodeLanguage.Java),
    Swift(SharedCodeLanguage.Swift),
    Script(SharedCodeLanguage.Script),
    Python(SharedCodeLanguage.Python),
    Go(SharedCodeLanguage.Go),
    Rust(SharedCodeLanguage.Rust),
    Shell(SharedCodeLanguage.Shell),
    Data(SharedCodeLanguage.Data),
    Plain(SharedCodeLanguage.Plain),
    ;

    companion object {
        fun of(path: String): CodeLanguage = valueOf(SharedCodeLanguage.of(path).name)
    }
}
