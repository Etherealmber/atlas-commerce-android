package ma.etherealmber.atlascommerce.ui.screens.devices

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import java.net.URI

/** Exact parser for the separate desktop native-auth v2 phone-approval QR. */
data class DesktopPhoneQr(val origin: String, val requestId: String)

object DesktopPhoneQrParser {
    private val fragmentPattern = Regex("^request_id=([A-Fa-f0-9]{64})$")

    /** Broad route recognition prevents malformed v2 links from being reinterpreted as legacy QR tokens. */
    fun isCandidate(raw: String?): Boolean = runCatching {
        val value = raw?.trim().orEmpty()
        value.isNotEmpty() && URI(value).rawPath.orEmpty().trimEnd('/') == "/login/desktop-phone"
    }.getOrDefault(false)

    /** Accepts only the fixed v2 URL structure, pinned to the currently selected HTTPS origin. */
    fun parse(raw: String?, selectedServer: String?): DesktopPhoneQr? {
        val value = raw ?: return null
        if (value != value.trim() || value.isEmpty()) return null
        val qr = runCatching { URI(value) }.getOrNull() ?: return null
        if (!qr.scheme.equals("https", ignoreCase = true) || qr.rawUserInfo != null ||
            qr.rawQuery != null || qr.rawPath != "/login/desktop-phone"
        ) return null
        val id = fragmentPattern.matchEntire(qr.rawFragment ?: return null)?.groupValues?.get(1) ?: return null
        val qrOrigin = strictHttpsOrigin(qr) ?: return null
        val activeUri = runCatching { URI(ServerUrl.normalize(selectedServer) ?: return null) }.getOrNull() ?: return null
        val activeOrigin = strictHttpsOrigin(activeUri) ?: return null
        if (qrOrigin != activeOrigin) return null
        return DesktopPhoneQr(activeOrigin, id)
    }

    /** Preserve explicit ports (including :443) rather than normalizing them away. */
    private fun strictHttpsOrigin(uri: URI): String? {
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null ||
            uri.rawQuery != null || uri.rawPath.orEmpty().let { it.isNotEmpty() && it != "/" }
        ) return null
        val host = uri.host ?: return null
        val authority = uri.rawAuthority ?: return null
        if (authority.contains('@') || uri.port !in -1..65535) return null
        val explicitPort = if (host.startsWith("[")) authority.substringAfter(']', "").startsWith(":")
            else authority.substring(host.length).startsWith(":")
        if (explicitPort && (uri.port !in 1..65535)) return null
        val portSuffix = if (explicitPort) ":${uri.port}" else ""
        return "https://${host.lowercase()}$portSuffix"
    }
}

/**
 * Pure parser distinguishing Atlas QR pairing links/tokens from warehouse 1D product barcodes.
 */
object QrTokenParser {

    private val rawTokenRegex = Regex("^[A-Za-z0-9_-]{18,128}$")
    private val digitsOnlyRegex = Regex("^\\d{6,16}$")

    /**
     * Extracts the QR session token from:
     * - `https://stock.afmarbre.com/login-qr?t=<token>` or `/qr?token=<token>`
     * - `atlascommerce://qr?t=<token>`
     * - `https://.../api/auth/qr/session/<token>`
     * - Raw QR token string (`18..128` URL-safe chars containing non-EAN entropy)
     *
     * Returns `null` for plain EAN/UPC numeric product barcodes or unrelated URLs.
     */
    fun extractQrToken(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        // Custom scheme deep link: atlascommerce://qr?t=...
        if (trimmed.startsWith("atlascommerce://", ignoreCase = true)) {
            val query = trimmed.substringAfter('?', "")
            parseQueryParam(query)?.let { return it }
            val tail = trimmed.substringAfterLast('/').substringBefore('?').trim()
            if (tail.isNotEmpty() && !tail.equals("qr", ignoreCase = true) && rawTokenRegex.matches(tail)) {
                return tail
            }
            return null
        }

        // Standard HTTP/HTTPS URL
        if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            val parsed = trimmed.toHttpUrlOrNull() ?: return null
            val fromQuery = parsed.queryParameter("t")
                ?: parsed.queryParameter("token")
                ?: parsed.queryParameter("session")
            if (!fromQuery.isNullOrBlank()) return fromQuery.trim()

            val segments = parsed.pathSegments.filter { it.isNotBlank() }
            val sessionIdx = segments.indexOf("session")
            if (sessionIdx >= 0 && sessionIdx + 1 < segments.size) {
                return segments[sessionIdx + 1].trim()
            }
            val qrIdx = segments.indexOfFirst {
                it.equals("qr", ignoreCase = true) || it.equals("login-qr", ignoreCase = true)
            }
            if (qrIdx >= 0 && qrIdx + 1 < segments.size) {
                return segments[qrIdx + 1].trim()
            }
            return null
        }

        // Reject standard numeric EAN-8 / UPC-A / EAN-13 / ITF-14 product barcodes
        if (digitsOnlyRegex.matches(trimmed)) return null

        return if (rawTokenRegex.matches(trimmed)) trimmed else null
    }

    fun looksLikeProductBarcode(raw: String?): Boolean {
        val trimmed = raw?.trim().orEmpty()
        return digitsOnlyRegex.matches(trimmed)
    }

    private fun parseQueryParam(query: String): String? {
        if (query.isBlank()) return null
        for (pair in query.split('&')) {
            val eq = pair.indexOf('=')
            if (eq <= 0) continue
            val key = pair.substring(0, eq).trim()
            val value = pair.substring(eq + 1).trim()
            if (value.isNotEmpty() && key in setOf("t", "token", "session")) {
                return value
            }
        }
        return null
    }
}
