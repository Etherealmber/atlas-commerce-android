package ma.etherealmber.atlascommerce.ui.components

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ma.etherealmber.atlascommerce.core.update.ManualApiRefreshRateLimiter
import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.localizedApiRefreshWait

/** Wraps an explicit network refresh tap; lifecycle and background refreshes remain unaffected. */
@Composable
fun rememberApiRefreshAction(
    key: String,
    cooldownMs: Long = 5_000L,
    onRefresh: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val language = LocalAtlasLanguage.current
    return remember(key, cooldownMs, onRefresh, context, language) {
        {
            val remaining = ManualApiRefreshRateLimiter.acquire(
                key = key,
                nowElapsedMs = SystemClock.elapsedRealtime(),
                minimumIntervalMs = cooldownMs,
            )
            if (remaining == null) {
                onRefresh()
            } else {
                val retrySeconds = ((remaining + 999L) / 1_000L).coerceAtLeast(1L).toInt()
                Toast.makeText(context, localizedApiRefreshWait(retrySeconds, language), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
