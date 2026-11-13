import AppKit

@_cdecl("moru_backdrop_probe")
public func moruBackdropProbe(_ handle: Int64, _ x: Double, _ y: Double) -> Int32 {
    guard x.isFinite, y.isFinite, handle != 0 else { return 0 }
    let address = UInt(truncatingIfNeeded: handle)
    var result: Int32 = 0
    let probe = {
        // AWT may dispose the captured window before this main-queue block runs.
        // Resolve a live object by address without dereferencing an unretained pointer.
        guard let window = NSApp.windows.first(where: {
            UInt(bitPattern: Unmanaged.passUnretained($0).toOpaque()) == address
        }) else { return }
        let screen = NSPoint(x: x, y: (NSScreen.screens.first?.frame.maxY ?? 0) - y)
        let number = NSWindow.windowNumber(at: screen, belowWindowWithWindowNumber: 0)
        let local = window.convertPoint(fromScreen: screen)
        guard let root = window.contentView, let parent = root.superview else { return }
        let rootPoint = parent.convert(local, from: nil)
        let hit = root.hitTest(rootPoint)
        let contentHit = hit === root || hit?.isDescendant(of: root) == true
        result = (number == window.windowNumber ? 1 : 0)
            | (contentHit ? 2 : 0) | (CGPreflightPostEventAccess() ? 4 : 0)
    }
    if Thread.isMainThread { probe() } else { DispatchQueue.main.sync(execute: probe) }
    return result
}
