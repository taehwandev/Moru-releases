package app.thdev.moru.core.domain.catalog

/** Rejection always retains the original snapshot; errors contain no user text. */
data class CatalogTransition(val snapshot: CatalogSnapshot, val errorCode: String?) {
    val accepted: Boolean get() = errorCode == null
}
