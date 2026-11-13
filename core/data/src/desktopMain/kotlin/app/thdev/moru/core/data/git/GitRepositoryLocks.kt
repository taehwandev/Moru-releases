package app.thdev.moru.core.data.git

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object GitRepositoryLocks {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    fun <T> withRepository(context: GitRepositoryDiscovery, block: () -> T): T =
        locks.computeIfAbsent(context.commonDirectory.toString()) { ReentrantLock() }.withLock(block)
}
