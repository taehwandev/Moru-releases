package app.thdev.moru.core.domain.code

/** Existing reader heuristics, selected by extension; unknown files remain plain. */
enum class CodeLanguage(
    internal val keywords: Set<String>,
    internal val lineComment: String?,
    internal val blockComments: Boolean,
    internal val tripleStrings: Boolean,
) {
    Kotlin(
        words("package import class interface object fun val var if else when for while do return break continue " +
            "try catch finally throw this super null true false is in as typealias constructor init companion data " +
            "sealed enum annotation inner open abstract override private protected public internal const lateinit " +
            "suspend inline reified operator infix vararg by where get set out"),
        "//", blockComments = true, tripleStrings = true,
    ),
    Java(
        words("package import class interface enum record extends implements new return if else switch case default " +
            "for while do break continue try catch finally throw throws this super null true false instanceof " +
            "public private protected static final abstract synchronized volatile transient native void var " +
            "boolean byte char short int long float double"),
        "//", blockComments = true, tripleStrings = true,
    ),
    Swift(
        words("import class struct enum protocol extension func var let if else guard switch case default for in " +
            "while repeat return break continue do try catch throw throws rethrows self Self super nil true false " +
            "is as init deinit static private fileprivate public internal open final override mutating some any " +
            "async await where typealias inout"),
        "//", blockComments = true, tripleStrings = true,
    ),
    Script(
        words("import export from default function const let var class extends new return if else switch case for " +
            "of in while do break continue try catch finally throw this super null undefined true false typeof " +
            "instanceof async await yield interface type enum implements public private protected readonly as"),
        "//", blockComments = true, tripleStrings = false,
    ),
    Python(
        words("import from as def class return if elif else for in while break continue try except finally raise " +
            "with lambda yield pass None True False and or not is global nonlocal async await self"),
        "#", blockComments = false, tripleStrings = true,
    ),
    Go(
        words("package import func var const type struct interface map chan go defer return if else switch case " +
            "default for range break continue select nil true false"),
        "//", blockComments = true, tripleStrings = false,
    ),
    Rust(
        words("use mod fn let mut const static struct enum trait impl pub crate self Self super return if else match " +
            "for in while loop break continue as where move ref async await dyn true false"),
        "//", blockComments = true, tripleStrings = false,
    ),
    Shell(
        words("if then else elif fi for in do done while case esac function return local export set unset echo"),
        "#", blockComments = false, tripleStrings = false,
    ),
    Data(words("true false null"), "#", blockComments = false, tripleStrings = false),
    Plain(emptySet(), null, blockComments = false, tripleStrings = false),
    ;

    companion object {
        fun of(path: String): CodeLanguage = when (path.substringAfterLast('.', "").lowercase()) {
            "kt", "kts", "gradle" -> Kotlin
            "java", "c", "h", "cpp", "hpp", "cs", "dart", "scala" -> Java
            "swift", "m", "mm" -> Swift
            "js", "jsx", "ts", "tsx", "mjs", "cjs" -> Script
            "py" -> Python
            "go" -> Go
            "rs" -> Rust
            "sh", "zsh", "bash" -> Shell
            "json", "yaml", "yml", "toml", "properties" -> Data
            else -> Plain
        }
    }
}

private fun words(text: String): Set<String> = text.split(' ').filter(String::isNotEmpty).toSet()
