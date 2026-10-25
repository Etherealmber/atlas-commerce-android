package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.BuildConfig
import java.net.URI

/**
 * Server address handling.
 *
 * The app is not tied to one deployment: a shop can run its own Atlas worker.
 * The user picks (and can change) the server address before logging in, so this
 * object centralises parsing, validation and the presets we offer.
 */
object ServerUrl {

    const val PRODUCTION = "https://stock.afmarbre.com"
    const val EMULATOR = "http://10.0.2.2:8787"
    const val DEVICE_LOCALHOST = "http://127.0.0.1:8787"

    data class Preset(val label: String, val url: String, val hint: String, val recommended: Boolean)

    fun presets(): List<Preset> = buildList {
        // Plain-HTTP developer presets only appear in debug builds; production
        // installs never advertise a private company domain as "Recommandé".
        if (BuildConfig.ALLOW_CLEARTEXT) {
            add(
                Preset(
                    label = "Émulateur",
                    url = EMULATOR,
                    hint = "Worker local vu depuis l'émulateur (npm run dev)",
                    recommended = false,
                ),
            )
            add(
                Preset(
                    label = "Local",
                    url = DEVICE_LOCALHOST,
                    hint = "Worker local depuis cet appareil",
                    recommended = false,
                ),
            )
        }
    }

    /**
     * Normalises user input into `scheme://host[:port][/path]` without a trailing
     * slash, or returns null when the value cannot be a server address.
     */
    fun normalize(raw: String?): String? {
        var value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val hasScheme = value.startsWith("http://", true) || value.startsWith("https://", true)
        if (!hasScheme) {
            // Reject other schemes outright instead of prefixing them with https://
            if (value.contains("://")) return null
            // Local dev servers (emulator, LAN) speak http; everything else is https.
            val looksLocal = Regex("^(localhost|127\\.|10\\.|192\\.168\\.|\\[::1\\])", RegexOption.IGNORE_CASE)
                .containsMatchIn(value)
            value = (if (looksLocal) "http://" else "https://") + value
        }
        value = value.trimEnd('/')
        for (suffix in listOf("/api", "/api/health", "/login")) {
            if (value.endsWith(suffix, ignoreCase = true)) {
                value = value.dropLast(suffix.length).trimEnd('/')
            }
        }
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        // Accept dotted names, "localhost", bracketed IPv6 and bare LAN hostnames.
        val looksLikeHost = host.startsWith("[") || HOST_PATTERN.matches(host)
        if (host.isBlank() || !looksLikeHost) return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return null
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val path = uri.path.orEmpty().trimEnd('/')
        return "$scheme://${host.lowercase()}$port$path"
    }

    /** Returns only the URI origin (scheme, host and non-default port), excluding any path. */
    fun origin(url: String?): String? {
        val normalized = normalize(url) ?: return null
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val defaultPort = (scheme == "https" && uri.port == 443) || (scheme == "http" && uri.port == 80)
        val port = if (uri.port > 0 && !defaultPort) ":${uri.port}" else ""
        return "$scheme://$host$port"
    }

    fun isCleartext(url: String): Boolean = url.startsWith("http://", ignoreCase = true)

    fun isLocal(url: String): Boolean {
        val host = host(url)
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host == "10.0.2.2" ||
            host == "[::1]" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.")
    }

    /** "stock.afmarbre.com" — what we show in compact chips. */
    fun display(url: String): String =
        url.removePrefix("https://").removePrefix("http://").trimEnd('/')

    fun host(url: String): String = runCatching { URI(url).host ?: url }.getOrDefault(url)

    fun withSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

    fun isProduction(url: String?): Boolean = normalize(url) == normalize(PRODUCTION)

    private val HOST_PATTERN =
        Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$", RegexOption.IGNORE_CASE)
}
