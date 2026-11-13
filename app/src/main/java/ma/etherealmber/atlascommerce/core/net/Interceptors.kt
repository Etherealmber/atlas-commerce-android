package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.security.DeviceIdentityUnavailableException
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLException

internal fun Request.Builder.addAtlasAppVersionHeaders(): Request.Builder =
    header("X-Atlas-App-Version-Code", AppDetails.VERSION_CODE.toString())
        .header("X-Atlas-App-Version-Name", AppDetails.VERSION_NAME)

/** Classify failures without copying exception messages, which may contain addresses or other details. */
internal fun networkFailureCategory(failure: IOException): String = when (failure) {
    is UnknownHostException -> "dns"
    is SSLException -> "tls"
    is SocketTimeoutException -> "timeout"
    is ProtocolException -> "protocol"
    else -> "io"
}

/** Coarse answer classification catches synthetic/private DNS answers without retaining raw IPs. */
internal fun diagnosticAddressClass(address: InetAddress): String {
    if (address.isAnyLocalAddress) return "unspecified"
    if (address.isLoopbackAddress) return "loopback"
    if (address.isLinkLocalAddress) return "link-local"
    if (address.isMulticastAddress) return "multicast"
    if (address is Inet4Address) {
        val octets = address.address.map { it.toInt() and 0xff }
        if (octets.size == 4 && octets[0] == 198 && octets[1] in 18..19) return "benchmark"
    }
    if (address.isSiteLocalAddress) return "private"
    return "global"
}

internal data class DiagnosticDnsAnswerSummary(
    val classes: String,
    val hasNonGlobalAnswer: Boolean,
    val hasSuspiciousAnswer: Boolean,
)

internal fun summarizeDiagnosticDnsAnswers(addresses: List<InetAddress>): DiagnosticDnsAnswerSummary {
    val classes = addresses.map(::diagnosticAddressClass).groupingBy { it }.eachCount()
    return DiagnosticDnsAnswerSummary(
        classes = classes.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" },
        hasNonGlobalAnswer = classes.keys.any { it != "global" },
        hasSuspiciousAnswer = classes.keys.any { it in setOf("benchmark", "unspecified", "loopback", "multicast") },
    )
}

/** Delegates unchanged to Android/OkHttp DNS while adding timings to the local diagnostic buffer. */
class AtlasDiagnosticDns(
    private val delegate: Dns = Dns.SYSTEM,
    private val logDiagnostic: (tag: String, message: String, level: String) -> Unit = { tag, message, level ->
        AtlasLogBuffer.log(tag = tag, message = message, level = level)
    },
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val started = System.nanoTime()
        return try {
            val addresses = delegate.lookup(hostname)
            val elapsed = elapsedMs(started)
            val summary = summarizeDiagnosticDnsAnswers(addresses)
            if (elapsed >= SLOW_NETWORK_EVENT_MS || summary.hasNonGlobalAnswer) {
                logSafely(
                    "NETWORK",
                    "DNS_OK host=$hostname elapsed=${elapsed}ms classes=${summary.classes}",
                    if (summary.hasSuspiciousAnswer) "WARN" else "INFO",
                )
            }
            addresses
        } catch (failure: UnknownHostException) {
            logSafely(
                "NETWORK",
                "DNS failed host=$hostname in ${elapsedMs(started)}ms; category=${networkFailureCategory(failure)}",
                "WARN",
            )
            throw failure
        }
    }

    private fun logSafely(tag: String, message: String, level: String) {
        runCatching { logDiagnostic(tag, message, level) }
    }
}

/** Captures DNS/connect/TLS timing without recording IPs, request data, headers, or exception messages. */
class AtlasNetworkEventListener private constructor(private val traceId: Long) : EventListener() {
    private var callStartedNs = 0L
    private var connectStartedNs = 0L
    private var tlsStartedNs = 0L
    private var dnsAttempts = 0
    private var connectAttempts = 0
    private var lastConnectMs = 0L
    private var lastTlsMs = 0L
    private var lastProtocol = "unknown"

    @Suppress("UNUSED_PARAMETER")
    override fun callStart(call: Call) {
        callStartedNs = System.nanoTime()
    }

    @Suppress("UNUSED_PARAMETER")
    override fun dnsStart(call: Call, domainName: String) {
        dnsAttempts += 1
    }

    @Suppress("UNUSED_PARAMETER")
    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) = Unit

    @Suppress("UNUSED_PARAMETER")
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        connectStartedNs = System.nanoTime()
        connectAttempts += 1
    }

    @Suppress("UNUSED_PARAMETER")
    override fun secureConnectStart(call: Call) {
        tlsStartedNs = System.nanoTime()
    }

    @Suppress("UNUSED_PARAMETER")
    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        lastTlsMs = elapsedMs(tlsStartedNs)
        if (lastTlsMs >= SLOW_NETWORK_EVENT_MS) {
            trace(call, "TLS_SLOW elapsed=${lastTlsMs}ms", "WARN")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        lastConnectMs = elapsedMs(connectStartedNs)
        lastProtocol = protocol?.toString() ?: "unknown"
        if (lastConnectMs >= SLOW_NETWORK_EVENT_MS) {
            trace(call, "CONNECT_SLOW elapsed=${lastConnectMs}ms protocol=$lastProtocol", "WARN")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        failure: IOException,
    ) {
        trace(
            call,
            "CONNECT_FAIL elapsed=${elapsedMs(connectStartedNs)}ms category=${networkFailureCategory(failure)}",
            "WARN",
        )
    }

    @Suppress("UNUSED_PARAMETER")
    override fun connectionAcquired(call: Call, connection: Connection) {
        lastProtocol = connection.protocol().toString()
    }

    override fun callEnd(call: Call) {
        val elapsed = elapsedMs(callStartedNs)
        if (elapsed >= SLOW_CALL_EVENT_MS) {
            trace(
                call,
                "CALL_SLOW elapsed=${elapsed}ms dnsAttempts=$dnsAttempts connectAttempts=$connectAttempts " +
                    "lastConnect=${lastConnectMs}ms lastTls=${lastTlsMs}ms protocol=$lastProtocol",
                "WARN",
            )
        }
    }

    override fun callFailed(call: Call, failure: IOException) {
        trace(
            call,
            "CALL_FAIL elapsed=${elapsedMs(callStartedNs)}ms dnsAttempts=$dnsAttempts connectAttempts=$connectAttempts category=${networkFailureCategory(failure)}",
            "WARN",
        )
    }

    private fun trace(call: Call, details: String, level: String = "INFO") {
        val request = call.request()
        runCatching {
            AtlasLogBuffer.log(
                tag = "NETWORK",
                message = "trace=$traceId ${request.method} host=${request.url.host} path=${request.url.encodedPath} $details",
                level = level,
            )
        }
    }

    companion object Factory : EventListener.Factory {
        private val ids = AtomicLong(0L)
        override fun create(call: Call): EventListener = AtlasNetworkEventListener(ids.incrementAndGet())
    }
}

private const val SLOW_NETWORK_EVENT_MS = 1_000L
private const val SLOW_CALL_EVENT_MS = 3_000L

private fun elapsedMs(startedNs: Long): Long =
    if (startedNs == 0L) 0L else TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNs).coerceAtLeast(0L)

/** Convert a temporarily unavailable persisted identity into a recoverable network failure. */
internal fun DeviceIdentity.fingerprintForRequest(): String = try {
    fingerprint
} catch (unavailable: DeviceIdentityUnavailableException) {
    throw IOException("Saved device identity is temporarily unavailable", unavailable)
}

/** POSTs (especially login/OTP) are never automatically replayed; only safe read methods qualify. */
internal fun shouldRetrySafeRead(method: String, failure: IOException): Boolean =
    method.uppercase() in setOf("GET", "HEAD", "OPTIONS") &&
        failure !is SSLException &&
        failure !is ProtocolException

/** One bounded retry for idempotent reads helps recover from a stale pooled socket or brief route flap. */
class SafeReadRetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (first: IOException) {
            if (chain.call().isCanceled() || !shouldRetrySafeRead(request.method, first)) throw first
            try {
                chain.proceed(request)
            } catch (second: IOException) {
                second.addSuppressed(first)
                throw second
            }
        }
    }
}

/**
 * Adds the headers the worker expects.
 *
 * `X-Atlas-Client: android` is what lets the worker skip Cloudflare Turnstile for
 * the native app (the app links its token to the device fingerprint instead), and
 * `X-Device-Fingerprint` is required by `/api/auth/me` and every authenticated
 * route because sessions are bound to the device.
 */
class AtlasHeaderInterceptor(
    private val tokenHolder: TokenHolder,
    private val deviceIdentity: DeviceIdentity,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Atlas-Client", "android")
            .addAtlasAppVersionHeaders()
            .header("X-Device-Fingerprint", deviceIdentity.fingerprintForRequest())
            .header("User-Agent", deviceIdentity.userAgent)
            .apply {
                tokenHolder.token?.takeIf { it.isNotBlank() }?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()
        val startNs = System.nanoTime()
        return try {
            val resp = chain.proceed(request)
            val ms = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            val apiError = if (resp.code >= 400) {
                runCatching {
                    API_ERROR_FIELD.findAll(resp.peekBody(2_048L).string())
                        .mapNotNull { match ->
                            val key = match.groupValues[1]
                            val value = match.groupValues[2].trim().take(180)
                            value.takeIf(String::isNotBlank)?.let { "$key=$it" }
                        }
                        .take(3)
                        .joinToString("; ")
                }.getOrDefault("")
            } else ""
            ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                tag = "HTTP",
                message = "${request.method} ${request.url.encodedPath} -> ${resp.code} (${ms}ms)" +
                    apiError.takeIf { it.isNotBlank() }?.let { "; $it" }.orEmpty(),
                level = if (resp.code >= 400) "WARN" else "INFO",
            )
            resp
        } catch (t: Throwable) {
            val ms = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                tag = "HTTP",
                message = "${request.method} ${request.url.encodedPath} FAILED (${ms}ms): ${t.javaClass.simpleName}: ${t.message}",
                level = "ERROR",
            )
            throw t
        }
    }

    private companion object {
        val API_ERROR_FIELD = Regex("\\\"(code|error|message)\\\"\\s*:\\s*\\\"([^\\\"]{1,400})\\\"", RegexOption.IGNORE_CASE)
    }
}

/**
 * Detects a dead session (401 with an auth code that is *not* a user mistake)
 * and raises a single app-wide event. Wrong-password / wrong-OTP replies never
 * destroy the session, matching the worker and the web client.
 */
class SessionGuardInterceptor(
    private val sessionEvents: SessionEvents,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code in setOf(401, 403) && request.header("Authorization") != null) {
            val errorBody = runCatching { response.peekBody(MAX_PEEK).string() }.getOrDefault("")
            val code = CODE_REGEX.find(errorBody)?.groupValues?.getOrNull(1)
            val revoked = code?.contains("REVOK", ignoreCase = true) == true ||
                code == "SESSION_EXPIRED" ||
                errorBody.contains("révoqué", ignoreCase = true) ||
                errorBody.contains("revoked", ignoreCase = true)
            val expired = response.code == 401 && (code == null || code !in USER_MISTAKE_CODES)
            if (expired || (response.code == 403 && revoked)) {
                ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer.log(
                    tag = "AUTH",
                    message = "Session guard received HTTP ${response.code} for ${request.url.encodedPath}; code=${code ?: "missing"}; ending the authenticated session",
                    level = "WARN",
                )
                sessionEvents.notifyExpired(request.header("Authorization").orEmpty())
            }
        }
        return response
    }

    private companion object {
        const val MAX_PEEK = 4096L
        val CODE_REGEX = Regex("\"code\"\\s*:\\s*\"([A-Z0-9_]+)\"")
        val USER_MISTAKE_CODES = setOf(
            "BAD_PASSWORD",
            "PASSWORD_REQUIRED",
            "BAD_OTP",
            "OTP_REQUIRED",
            "OTP_EXPIRED",
            "OTP_NOT_REQUESTED",
            "RATE_LIMITED",
            "TURNSTILE_FAILED",
        )
    }
}
