import Foundation

@_cdecl("moru_backdrop_remove")
public func moruBackdropRemove(_ token: Int64) {
    guard BackdropTokenStore.shared.requestRemoval(token) else { return }
    DispatchQueue.main.async {
        BackdropHost.shared.remove(token: token)
    }
}
