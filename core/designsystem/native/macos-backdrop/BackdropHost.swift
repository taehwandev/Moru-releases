import AppKit

@MainActor
final class BackdropHost: NSObject {
    static let shared = BackdropHost()

    @MainActor
    private final class WindowState {
        let window: NSWindow
        let root: NSView
        let container: NSView
        let view: NSView
        let kind: Int32
        let originalOpaque: Bool
        let originalBackground: NSColor
        let originalFrameNotifications: Bool
        let originalBoundsNotifications: Bool
        var tokens: Set<Int64> = []

        init(window: NSWindow, root: NSView, container: NSView, view: NSView, kind: Int32) {
            self.window = window
            self.root = root
            self.container = container
            self.view = view
            self.kind = kind
            originalOpaque = window.isOpaque
            originalBackground = window.backgroundColor
            originalFrameNotifications = root.postsFrameChangedNotifications
            originalBoundsNotifications = root.postsBoundsChangedNotifications
        }
    }

    private var attachments: [Int64: ObjectIdentifier] = [:]
    private var windows: [ObjectIdentifier: WindowState] = [:]
    private var contentWindows: [ObjectIdentifier: ObjectIdentifier] = [:]

    func install(
        token: Int64,
        window: NSWindow,
        role: Int32,
        cornerRadius: Double,
        dark: Int32
    ) {
        guard BackdropTokenStore.shared.canInstall(token),
              window.isVisible,
              let root = window.contentView, let container = root.superview else {
            BackdropTokenStore.shared.completeRemoval(token)
            return
        }
        let windowKey = ObjectIdentifier(window)
        let state: WindowState
        if let existing = windows[windowKey] {
            state = existing
        } else {
            let material = BackdropMaterialFactory.make(
                frame: root.convert(root.bounds, to: container),
                role: role, cornerRadius: cornerRadius, dark: dark
            )
            state = WindowState(
                window: window, root: root, container: container,
                view: material.view, kind: material.kind
            )
            // Only content-root notifications resize this sibling; the frame's autoresizing
            // would otherwise apply a second, independently ordered size adjustment.
            material.view.autoresizingMask = []
            windows[windowKey] = state
            contentWindows[ObjectIdentifier(root)] = windowKey
            root.postsFrameChangedNotifications = true
            root.postsBoundsChangedNotifications = true
            for name in [NSView.frameDidChangeNotification, NSView.boundsDidChangeNotification] {
                NotificationCenter.default.addObserver(
                    self, selector: #selector(contentBoundsChanged(_:)), name: name, object: root
                )
            }
            NotificationCenter.default.addObserver(
                self,
                selector: #selector(windowWillClose(_:)),
                name: NSWindow.willCloseNotification,
                object: window
            )
            window.isOpaque = false
            window.backgroundColor = .clear
            // Preserve AWT's content view and its Swing/Metal layers. The material is a sibling,
            // so AppKit owns its ordering without inserting a backing layer into the renderer.
            container.addSubview(material.view, positioned: .below, relativeTo: root)
        }
        state.tokens.insert(token)
        attachments[token] = windowKey
        state.view.frame = root.convert(root.bounds, to: state.container)
        if !BackdropTokenStore.shared.completeInstall(token, kind: state.kind) {
            remove(token: token)
        }
    }

    @objc private func contentBoundsChanged(_ notification: Notification) {
        guard let root = notification.object as? NSView,
              let key = contentWindows[ObjectIdentifier(root)], let state = windows[key] else { return }
        // AWT can disable autoresizesSubviews, so autoresizingMask alone cannot track popup layout.
        state.view.frame = root.convert(root.bounds, to: state.container)
    }

    func remove(token: Int64) {
        guard let windowKey = attachments.removeValue(forKey: token) else {
            BackdropTokenStore.shared.completeRemoval(token)
            return
        }
        if let state = windows[windowKey] {
            state.tokens.remove(token)
            if state.tokens.isEmpty {
                state.view.removeFromSuperview()
                contentWindows.removeValue(forKey: ObjectIdentifier(state.root))
                NotificationCenter.default.removeObserver(self, name: nil, object: state.root)
                state.root.postsFrameChangedNotifications = state.originalFrameNotifications
                state.root.postsBoundsChangedNotifications = state.originalBoundsNotifications
                state.window.isOpaque = state.originalOpaque
                state.window.backgroundColor = state.originalBackground
                NotificationCenter.default.removeObserver(
                    self,
                    name: NSWindow.willCloseNotification,
                    object: state.window
                )
                windows.removeValue(forKey: windowKey)
            }
        }
        BackdropTokenStore.shared.completeRemoval(token)
    }

    @objc private func windowWillClose(_ notification: Notification) {
        guard let window = notification.object as? NSWindow,
              let state = windows[ObjectIdentifier(window)] else { return }
        for token in Array(state.tokens) {
            remove(token: token)
        }
    }
}
