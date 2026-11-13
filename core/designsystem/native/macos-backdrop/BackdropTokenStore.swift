import Foundation

final class BackdropTokenStore {
    static let shared = BackdropTokenStore()

    private struct Entry {
        var kind: Int32 = 0
        var removalRequested = false
    }

    private let lock = NSLock()
    private var nextToken: Int64 = 1
    private var entries: [Int64: Entry] = [:]

    func reserve() -> Int64 {
        lock.lock()
        defer { lock.unlock() }
        while entries[nextToken] != nil {
            advanceToken()
        }
        let token = nextToken
        entries[token] = Entry()
        advanceToken()
        return token
    }

    func canInstall(_ token: Int64) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard let entry = entries[token] else { return false }
        return entry.kind == 0 && !entry.removalRequested
    }

    func completeInstall(_ token: Int64, kind: Int32) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard var entry = entries[token], !entry.removalRequested else {
            return false
        }
        entry.kind = kind
        entries[token] = entry
        return true
    }

    func requestRemoval(_ token: Int64) -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard var entry = entries[token], !entry.removalRequested else {
            return false
        }
        entry.removalRequested = true
        entries[token] = entry
        return true
    }

    func completeRemoval(_ token: Int64) {
        lock.lock()
        defer { lock.unlock() }
        entries.removeValue(forKey: token)
    }

    func kind(_ token: Int64) -> Int32 {
        lock.lock()
        defer { lock.unlock() }
        return entries[token]?.kind ?? -1
    }

    func activeCount() -> Int32 {
        lock.lock()
        defer { lock.unlock() }
        let count = entries.values.filter { $0.kind > 0 }.count
        return Int32(clamping: count)
    }

    private func advanceToken() {
        nextToken = nextToken == Int64.max ? 1 : nextToken + 1
    }
}
