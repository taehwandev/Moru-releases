package app.thdev.moru.core.model.preview

/** Where a browser page view is: still opening, showing the page, or unable to because Chrome is missing or failed. */
enum class BrowserPageStatus { Starting, Showing, ChromeMissing, Failed }
