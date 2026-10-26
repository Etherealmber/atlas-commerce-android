package ma.etherealmber.atlascommerce.core.security

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot signal to offer passkey creation after a successful password/OTP sign-in. */
@Singleton
class PasskeySetupPrompt @Inject constructor() {
    private val offeredThisProcess = AtomicBoolean(false)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun request() {
        if (offeredThisProcess.compareAndSet(false, true)) _events.tryEmit(Unit)
    }
}
