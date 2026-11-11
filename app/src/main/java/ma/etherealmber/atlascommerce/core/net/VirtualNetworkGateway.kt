package ma.etherealmber.atlascommerce.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import java.net.Inet4Address

/** Heuristic only: this identifies likely Android virtual runtimes, never changes routing by itself. */
internal fun isLikelyVirtualAndroidRuntime(
    manufacturer: String = Build.MANUFACTURER,
    model: String = Build.MODEL,
    hardware: String = Build.HARDWARE,
    fingerprint: String = Build.FINGERPRINT,
): Boolean {
    val markers = listOf(manufacturer, model, hardware, fingerprint).joinToString(" ").lowercase()
    return listOf("microsoft", "windows subsystem for android", "wsa", "emulator", "sdk_gphone", "goldfish", "ranchu")
        .any { marker -> markers.contains(marker) }
}

/**
 * Returns the active Android network's IPv4 default-route gateway as a *suggestion*.
 * A gateway is not guaranteed to be the Windows host or backend; callers must let the user
 * explicitly test/select it rather than silently redirect authentication traffic.
 */
internal fun discoverDefaultGatewayIpv4(context: Context): String? = runCatching {
    val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return@runCatching null
    val activeNetwork = connectivity.activeNetwork ?: return@runCatching null
    val linkProperties = connectivity.getLinkProperties(activeNetwork) ?: return@runCatching null
    linkProperties.routes.firstNotNullOfOrNull { route ->
        if (!route.isDefaultRoute) return@firstNotNullOfOrNull null
        val address = (route.gateway as? Inet4Address)?.hostAddress ?: return@firstNotNullOfOrNull null
        address.takeIf { ServerUrl.isLocal("http://$it") }
    }
}.getOrNull()

internal fun suggestedLocalEndpoint(gatewayIpv4: String, port: Int = 8787): String? {
    if (port !in 1..65535) return null
    val octets = gatewayIpv4.split('.')
    if (octets.size != 4) return null
    val parsed = octets.map { it.toIntOrNull()?.takeIf { octet -> octet in 0..255 } ?: return null }
    if (parsed[0] == 0 || parsed[0] >= 224 || parsed[0] == 127) return null
    if (!ServerUrl.isLocal("http://$gatewayIpv4")) return null
    return "http://$gatewayIpv4:$port"
}
