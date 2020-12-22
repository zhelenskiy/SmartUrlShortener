import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class LoopGuard(val maxConnections: Long, val rememberFor: Duration) {
    init {
        if (maxConnections < 0) throw IllegalArgumentException("maxConnections must be positive")
    }

    private val storage = ConcurrentHashMap<Url, Long>()

    fun isSafe(url: Url): Boolean {
        var res = false
        storage.compute(url) { _, old ->
            when (val new = (old?.inc() ?: 1).takeIf { it <= maxConnections }) {
                null -> old
                else -> {
                    res = true
                    GlobalScope.launch {
                        delay(rememberFor)
                        storage.computeIfPresent(url) { _, old -> old.dec().takeIf { it > 0 } }
                    }
                    new
                }
            }
        }
        return res
    }
}