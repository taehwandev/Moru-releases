package app.thdev.moru.core.model.git

sealed interface GitRepositoryState {
    data class Loaded(val snapshot: GitRepositorySnapshot) : GitRepositoryState
    data class Unavailable(val message: String) : GitRepositoryState
}
