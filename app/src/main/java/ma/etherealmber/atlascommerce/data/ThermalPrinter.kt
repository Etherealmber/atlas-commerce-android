package ma.etherealmber.atlascommerce.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

/** Minimal Bluetooth Classic ESC/POS transport for common SPP receipt printers. */
object ThermalPrinter {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    internal fun requiresBluetoothConnectPermission(apiLevel: Int): Boolean = apiLevel >= Build.VERSION_CODES.S

    fun pairedDevices(context: Context): List<BluetoothDevice> {
        if (requiresBluetoothConnectPermission(Build.VERSION.SDK_INT) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Allow Nearby devices permission to list paired printers.")
        }
        return try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter ?: return emptyList()
            adapter.bondedDevices.orEmpty().sortedBy { it.name.orEmpty().lowercase() }
        } catch (denied: SecurityException) {
            throw SecurityException("Bluetooth access was revoked; allow Nearby devices and try again.", denied)
        }
    }

    suspend fun print(
        context: Context,
        address: String,
        paperWidthMm: Int,
        title: String,
        ticketNumber: String,
        client: String,
        lines: List<ReceiptLine>,
        total: Double?,
        currency: String,
        discount: Double = 0.0,
        vatFreeVr: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val verifiedTotal = requireVerifiedTotal(total)
        require(discount.isFinite() && discount >= 0.0) { "Receipt discount must be finite and nonnegative." }
        require(lines.all { it.quantity.isFinite() && it.unitPrice.isFinite() }) { "Receipt lines contain an invalid amount." }
        require(paperWidthMm == 58 || paperWidthMm == 80) { "Paper width must be 58 or 80 mm." }
        if (requiresBluetoothConnectPermission(Build.VERSION.SDK_INT) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Allow Nearby devices permission to print receipts.")
        }
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter ?: error("Bluetooth is not available on this device.")
            check(adapter.isEnabled) { "Turn on Bluetooth and try again." }
            val device = adapter.getRemoteDevice(address)
            cancelDiscoveryOnLegacyAndroid(adapter)
            val socket = device.createRfcommSocketToServiceRecord(sppUuid)
            try {
                socket.connect()
                socket.outputStream.use { out ->
                    out.write(byteArrayOf(0x1b, 0x40)) // Initialize
                    out.write(byteArrayOf(0x1b, 0x61, 0x01)) // Center
                    out.write(encode(wrapText(title, columns(paperWidthMm)).joinToString("\n") + "\n"))
                    out.write(encode(wrapText(ticketNumber, columns(paperWidthMm)).joinToString("\n") + "\n"))
                    out.write(byteArrayOf(0x1b, 0x61, 0x00)) // Left
                    out.write(encode(formatReceiptBody(client, lines, verifiedTotal, currency, columns(paperWidthMm), discount, vatFreeVr)))
                    out.write(encode("\nMerci pour votre confiance.\n\n\n"))
                    out.write(byteArrayOf(0x1d, 0x56, 0x00)) // Full cut (ignored by printers without cutter)
                    out.flush()
                }
            } finally {
                runCatching { socket.close() }
            }
        } catch (denied: SecurityException) {
            throw SecurityException("Bluetooth access was revoked; allow Nearby devices and try again.", denied)
        }
    }

    /** BLUETOOTH_ADMIN is sufficient on API 30; Android 12+ needs BLUETOOTH_SCAN, so skip it. */
    @SuppressLint("MissingPermission")
    private fun cancelDiscoveryOnLegacyAndroid(adapter: BluetoothAdapter) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) adapter.cancelDiscovery()
    }

    internal fun requireVerifiedTotal(total: Double?): Double =
        total?.takeIf(Double::isFinite)
            ?: throw IllegalArgumentException("The server did not return a verified receipt total; printing was stopped.")

    internal fun columnsForPaperWidth(paperWidthMm: Int): Int {
        require(paperWidthMm == 58 || paperWidthMm == 80) { "Paper width must be 58 or 80 mm." }
        return columns(paperWidthMm)
    }

    private fun columns(paperWidthMm: Int) = if (paperWidthMm == 58) 32 else 48

    internal fun formatReceiptBody(
        client: String,
        lines: List<ReceiptLine>,
        total: Double?,
        currency: String,
        columns: Int,
        discount: Double = 0.0,
        vatFreeVr: Boolean = false,
    ): String {
        val verifiedTotal = requireVerifiedTotal(total)
        require(discount.isFinite() && discount >= 0.0)
        require(lines.all { it.quantity.isFinite() && it.unitPrice.isFinite() })
        val rule = "-".repeat(columns)
        val output = mutableListOf<String>()
        output += rule
        output += wrapText("Client: $client", columns)
        output += rule
        lines.forEach { line ->
            output += formatItemRow(line.name, line.quantity, line.quantity * line.unitPrice, currency, columns).lines()
        }
        if (discount > 0.0) output += wrapRow("DISCOUNT", money(discount, currency), columns)
        if (vatFreeVr) output += wrapText("VAT: 0.00 $currency (VR - VAT exempt)", columns)
        output += rule
        output += wrapRow("TOTAL", money(verifiedTotal, currency), columns)
        output += ""
        return output.joinToString("\n")
    }

    private fun wrapText(value: String, columns: Int): List<String> =
        value.ifEmpty { " " }.chunked(columns).ifEmpty { listOf("") }

    // Printer firmware defaults to different code pages (often CP437); emit a safe ASCII subset.
    private fun encode(text: String): ByteArray = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\x20-\\x7E\\n]"), "?")
        .toByteArray(Charsets.US_ASCII)

    private fun money(value: Double, currency: String) = "%.2f %s".format(Locale.US, value, currency)
    private fun formatQty(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else "%.3f".format(Locale.US, value).trimEnd('0').trimEnd('.')

    internal fun formatItemRow(name: String, quantity: Double, amountValue: Double, currency: String, columns: Int): String {
        val quantitySuffix = " x${formatQty(quantity)}"
        val amount = money(amountValue, currency)
        if (quantitySuffix.length >= columns) {
            val quantityLines = quantitySuffix.chunked(columns).joinToString("\n")
            val amountLines = amount.chunked(columns).joinToString("\n") { it.padStart(columns) }
            return "${name.take(columns)}\n$quantityLines\n$amountLines"
        }
        val nameWidth = columns - quantitySuffix.length - amount.length - 1
        if (nameWidth >= 1) {
            return name.take(nameWidth).padEnd(nameWidth) + quantitySuffix + " " + amount
        }
        val shortNameWidth = (columns - quantitySuffix.length).coerceAtLeast(0)
        val firstLine = name.take(shortNameWidth).padEnd(shortNameWidth) + quantitySuffix
        val amountLines = amount.chunked(columns).joinToString("\n") { it.padStart(columns) }
        return "$firstLine\n$amountLines"
    }

    private fun wrapRow(left: String, right: String, columns: Int): String {
        val safeRight = right.take(columns)
        return left.take((columns - safeRight.length - 1).coerceAtLeast(1)).padEnd((columns - safeRight.length).coerceAtLeast(1)) + safeRight
    }
}

data class ReceiptLine(val name: String, val quantity: Double, val unitPrice: Double)
