package ma.etherealmber.atlascommerce.core.net

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

data class VerifyLink(
    val username: String,
    val otp: String,
    val sig: String,
    val exp: Long,
)

/**
 * Pure-JVM parser for signed one-tap e-mail verification links (companion to
 * `POST /api/auth/verify-link`).
 *
 * Accepts both:
 * - Web form on any configured Atlas Commerce Worker host:
 *   `https://<worker-host>/verify?user=<username>&code=<otp>&sig=<sha256hex>&exp=<epoch ms>`
 *   (plus local dev `http://localhost` / `http://127.0.0.1` / `http://10.0.2.2`)
 * - Native browser handoff intent form:
 *   `atlascommerce://verify?user=<username>&code=<otp>&sig=<sha256hex>&exp=<epoch ms>`
 */
object VerifyLinkParser {

    fun parse(raw: String?): VerifyLink? {
        val cleaned = raw?.trim()?.substringBefore('#')?.trim().orEmpty()
        if (cleaned.isEmpty()) return null

        val query = extractVerifiedQuery(cleaned) ?: return null
        val params = parseQueryParams(query)

        val username = params["user"]?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val otp = params["code"]?.trim().orEmpty()
        val sig = params["sig"]?.trim().orEmpty()
        val exp = params["exp"]?.trim()?.toLongOrNull() ?: return null

        if (username.isEmpty()) return null
        if (otp.length < 4 || !otp.all { it.isDigit() }) return null
        if (sig.isEmpty()) return null
        if (exp <= 0L) return null

        return VerifyLink(
            username = username,
            otp = otp,
            sig = sig,
            exp = exp,
        )
    }

    /**
     * Extracts the Worker server origin (`scheme://host[:port]`) when [raw] is a valid
     * HTTP/HTTPS verification deep link, or `null` when opened via `atlascommerce://verify`.
     */
    fun extractServerOrigin(raw: String?): String? {
        val cleaned = raw?.trim()?.substringBefore('#')?.trim().orEmpty()
        if (cleaned.isEmpty()) return null
        if (parse(cleaned) == null) return null
        val schemeSep = cleaned.indexOf("://")
        if (schemeSep <= 0) return null
        val scheme = cleaned.substring(0, schemeSep).lowercase(Locale.ROOT)
        if (scheme != "https" && scheme != "http") return null
        val rest = cleaned.substring(schemeSep + 3)
        val authority = rest.substringBefore('/').substringBefore('?').trim()
        return if (authority.isNotEmpty()) "$scheme://$authority" else null
    }

    private fun extractVerifiedQuery(url: String): String? {
        val schemeSep = url.indexOf("://")
        if (schemeSep <= 0) return null
        val scheme = url.substring(0, schemeSep).lowercase(Locale.ROOT)
        val remainder = url.substring(schemeSep + 3)
        val qIdx = remainder.indexOf('?')
        if (qIdx <= 0 || qIdx >= remainder.length - 1) return null

        val authorityAndPath = remainder.substring(0, qIdx)
        val query = remainder.substring(qIdx + 1)

        return when (scheme) {
            "atlascommerce" -> {
                val normalized = authorityAndPath.trimEnd('/')
                if (normalized.equals("verify", ignoreCase = true)) query else null
            }

            "https", "http" -> {
                val slashIdx = authorityAndPath.indexOf('/')
                if (slashIdx <= 0) return null
                val authority = authorityAndPath.substring(0, slashIdx).trim()
                val hostOnly = authority.substringBefore(':').lowercase(Locale.ROOT)
                if (hostOnly.isEmpty()) return null
                if (scheme == "http" && hostOnly !in LOCAL_DEV_HOSTS) return null
                val path = authorityAndPath.substring(slashIdx).trimEnd('/')
                if (path == "/verify") query else null
            }

            else -> null
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (segment in query.split('&')) {
            if (segment.isEmpty()) continue
            val eq = segment.indexOf('=')
            if (eq <= 0) continue
            val key = percentDecode(segment.substring(0, eq)).trim()
            val value = percentDecode(segment.substring(eq + 1))
            if (key.isNotEmpty() && !out.containsKey(key)) {
                out[key] = value
            }
        }
        return out
    }

    private fun percentDecode(input: String): String {
        if (!input.contains('%')) return input
        val out = ByteArrayOutputStream(input.length)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (ch == '%' && i + 2 < input.length) {
                val hi = Character.digit(input[i + 1], 16)
                val lo = Character.digit(input[i + 2], 16)
                if (hi >= 0 && lo >= 0) {
                    out.write((hi shl 4) or lo)
                    i += 3
                    continue
                }
            }
            val bytes = ch.toString().toByteArray(StandardCharsets.UTF_8)
            out.write(bytes, 0, bytes.size)
            i++
        }
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    private val LOCAL_DEV_HOSTS = setOf("localhost", "127.0.0.1", "10.0.2.2")
}
