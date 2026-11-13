import AppKit

@MainActor
enum BackdropMaterialFactory {
    static func make(
        frame: NSRect,
        role: Int32,
        cornerRadius: Double,
        dark: Int32
    ) -> (view: NSView, kind: Int32) {
        let radius = cornerRadius.isFinite ? max(0, cornerRadius) : 0
        let backing = BackdropVisualEffectView(frame: frame)
        backing.material = role == 1 ? .popover : .sidebar
        backing.blendingMode = .behindWindow
        backing.state = .followsWindowActiveState
        backing.autoresizingMask = [.width, .height]
        // This view has its own layer; the AWT/Skiko content remains an untouched sibling.
        backing.wantsLayer = true
        if role == 1 {
            backing.layer?.cornerRadius = CGFloat(radius)
            backing.layer?.masksToBounds = true
        }
        let kind: Int32
        if role == 1, #available(macOS 26.0, *) {
            // Popup glass needs a behind-window material to supply its live backdrop.
            let glass = BackdropGlassEffectView(frame: backing.bounds)
            glass.style = .regular
            glass.cornerRadius = CGFloat(radius)
            glass.autoresizingMask = [.width, .height]
            let content = NSView(frame: glass.bounds)
            content.autoresizingMask = [.width, .height]
            glass.contentView = content
            backing.addSubview(glass)
            kind = 2
        } else {
            kind = 1
        }
        switch dark {
        case 0: backing.appearance = NSAppearance(named: .aqua)
        case 1: backing.appearance = NSAppearance(named: .darkAqua)
        default: backing.appearance = nil
        }
        return (backing, kind)
    }
}
