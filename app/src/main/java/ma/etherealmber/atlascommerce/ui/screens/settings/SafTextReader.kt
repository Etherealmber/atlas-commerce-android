package ma.etherealmber.atlascommerce.ui.screens.settings

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded SAF read: never loads an arbitrarily large document into memory. */
internal fun readBoundedText(input: InputStream?, maxBytes: Int): String? {
    if (input == null || maxBytes < 1) return null
    return input.use { stream ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 4096))
        val buffer = ByteArray(2048)
        while (true) {
            val remainingWithSentinel = maxBytes + 1 - output.size()
            if (remainingWithSentinel <= 0) return@use null
            val count = stream.read(buffer, 0, minOf(buffer.size, remainingWithSentinel))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        if (output.size() > maxBytes) null else output.toByteArray().toString(Charsets.UTF_8)
    }
}
