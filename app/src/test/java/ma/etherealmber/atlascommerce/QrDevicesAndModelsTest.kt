package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.ui.screens.devices.QrTokenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrDevicesAndModelsTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun qrTokenParserDistinguishesSecurityPairingLinksFromWarehouseBarcodes() {
        assertEquals(
            "qr_tok_98ab76cd54ef321098ab",
            QrTokenParser.extractQrToken("https://stock.afmarbre.com/login-qr?t=qr_tok_98ab76cd54ef321098ab"),
        )
        assertEquals(
            "abc123XYZ_-9876543210",
            QrTokenParser.extractQrToken("atlascommerce://qr?token=abc123XYZ_-9876543210"),
        )
        assertEquals(
            "session_tok_abcdef1234567890",
            QrTokenParser.extractQrToken("https://stock.afmarbre.com/api/auth/qr/session/session_tok_abcdef1234567890"),
        )

        // Warehouse EAN-13 / UPC-A barcodes MUST NEVER be accepted as PC pairing tokens
        assertTrue(QrTokenParser.looksLikeProductBarcode("6111035000214"))
        assertNull(QrTokenParser.extractQrToken("6111035000214"))
        assertNull(QrTokenParser.extractQrToken("012345678905"))
    }

    @Test
    fun trustedDeviceUsesFullSha256FingerprintWhenRevoking() {
        val adapter = moshi.adapter(TrustedDeviceDto::class.java)
        val parsed = adapter.fromJson(
            """
            {
              "fingerprint": "9f86d081884c…",
              "full": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
              "last_ip": "196.200.141.12",
              "last_seen": "2026-10-22 14:30",
              "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0",
              "current": false
            }
            """.trimIndent(),
        )
        assertNotNull(parsed)
        assertEquals(
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            parsed?.revokeId,
        )
        assertEquals("PC Windows · Chrome", parsed?.friendlyLabel)
    }

    @Test
    fun productNeededQuantityAndNewDtosRoundTripCleanly() {
        val low = ProductDto(id = 1, name = "Granit Noir", stock = 2.0, lowStockThresholdRaw = 10.0)
        assertEquals(8.0, low.neededQuantity, 0.001)

        val healthy = ProductDto(id = 2, name = "Colle Pro", stock = 25.0, lowStockThresholdRaw = 10.0)
        assertEquals(0.0, healthy.neededQuantity, 0.001)

        assertTrue(Roles.canMutateStock("owner"))
        assertTrue(Roles.canMutateStock("magasinier"))
        assertFalse(Roles.canMutateStock("user"))

        val qr = moshi.adapter(QrSessionStatusDto::class.java)
            .fromJson("""{"status":"claimed","username":"amine","avatar_url":"data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="}""")
        assertEquals("claimed", qr?.status)

        val req = moshi.adapter(ProductCreationRequestDto::class.java)
            .fromJson("""{"id":7,"code":"61112456","name":"Plinthe","unit":"m²","quantity":14,"status":"open"}""")
        assertEquals("Plinthe", req?.name)

        val action = moshi.adapter(MobileActionDto::class.java)
            .fromJson("""{"id":3,"type":"stock_in","title":"Réception palette","status":"pending"}""")
        assertEquals("pending", action?.status)

        val chat = moshi.adapter(AssistChatResponseDto::class.java)
            .fromJson("""{"reply":"3 articles sont sous le seuil.","proposals":[{"proposal_id":"p_1","title":"Commander Marbre"}]}""")
        assertEquals("p_1", chat?.proposals?.single()?.proposalId)
    }
}
