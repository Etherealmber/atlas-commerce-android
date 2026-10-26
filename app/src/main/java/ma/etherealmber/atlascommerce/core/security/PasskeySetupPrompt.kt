package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot signal to offer passkey creation after a successful password/OTP sign-in. */
@Singleton
class PasskeySetupPrompt @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("passkey_setup", Context.MODE_PRIVATE)
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun request() {
        if (preferences.getBoolean(KEY_OFFERED, false)) return
        preferences.edit().putBoolean(KEY_OFFERED, true).apply()
        _events.tryEmit(Unit)
    }

    private companion object {
        const val KEY_OFFERED = "post_sign_in_offer_shown"
    }
}
