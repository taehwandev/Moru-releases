package app.thdev.moru.feature.terminal

/**
 * Types [text] into the terminal behind [sessionKey] exactly as if the user had typed it, without
 * pressing Enter, so a person can read it before it is submitted. Returns false if no such live terminal exists.
 */
expect fun typeIntoTerminalSession(sessionKey: String, text: String): Boolean
