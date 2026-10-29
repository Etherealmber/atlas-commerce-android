package ma.etherealmber.atlascommerce.core.security

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot opt-in prompt for local device authentication on later sign-ins. */
@Singleton
class DeviceUnlockSetupPrompt @Inject constructor() {
    data class Request(val serverUrl: String, val username: String)

    private val _events = MutableSharedFlow<Request>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    fun request(serverUrl: String, username: String) {
        if (serverUrl.isNotBlank() && username.isNotBlank()) {
            _events.tryEmit(Request(serverUrl, username))
        }
    }
}
