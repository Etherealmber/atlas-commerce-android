package ma.etherealmber.atlascommerce.core.net

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-layer session signals: the HTTP layer cannot navigate, so it emits here
 * and the root composable reacts (snackbar + return to login).
 */
@Singleton
class SessionEvents @Inject constructor() {

    private val _expired = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val expired: SharedFlow<String> = _expired.asSharedFlow()

    private val _unauthorizedWrite = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val forbidden: SharedFlow<String> = _unauthorizedWrite.asSharedFlow()

    fun notifyExpired(message: String = "Session expirée — reconnectez-vous.") {
        _expired.tryEmit(message)
    }

    fun notifyForbidden(message: String) {
        _unauthorizedWrite.tryEmit(message)
    }
}
