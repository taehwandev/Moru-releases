@_cdecl("moru_backdrop_kind")
public func moruBackdropKind(_ token: Int64) -> Int32 {
    BackdropTokenStore.shared.kind(token)
}
