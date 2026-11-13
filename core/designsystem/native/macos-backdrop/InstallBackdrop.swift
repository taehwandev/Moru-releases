import AppKit

@_cdecl("moru_backdrop_install")
public func moruBackdropInstall(
    _ nativeWindow: Int64,
    _ role: Int32,
    _ cornerRadius: Double,
    _ dark: Int32
) -> Int64 {
    guard role == 0 || role == 1,
          let address = UnsafeRawPointer(bitPattern: UInt(truncatingIfNeeded: nativeWindow)) else {
        return 0
    }
    // The in-process caller owns a live NSWindow pointer. Capture it strongly before queuing.
    let window = Unmanaged<NSWindow>.fromOpaque(address).takeUnretainedValue()
    let token = BackdropTokenStore.shared.reserve()
    DispatchQueue.main.async {
        BackdropHost.shared.install(
            token: token,
            window: window,
            role: role,
            cornerRadius: cornerRadius,
            dark: dark
        )
    }
    return token
}
