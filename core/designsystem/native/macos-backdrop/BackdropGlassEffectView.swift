import AppKit

@available(macOS 26.0, *)
final class BackdropGlassEffectView: NSGlassEffectView {
    override func hitTest(_ point: NSPoint) -> NSView? {
        nil
    }
}
