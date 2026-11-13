@_cdecl("moru_backdrop_count")
public func moruBackdropCount() -> Int32 {
    BackdropTokenStore.shared.activeCount()
}
