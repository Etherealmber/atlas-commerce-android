package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ServerUrl
import ma.etherealmber.atlascommerce.core.net.VerifyLink
import ma.etherealmber.atlascommerce.core.net.VerifyLinkParser
import ma.etherealmber.atlascommerce.core.security.AtRestCipher
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.PayrollRowDto
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadDetailDto
import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.StockAdjustmentRequestDto
import ma.etherealmber.atlascommerce.data.model.SnakePersonalBestDto
import ma.etherealmber.atlascommerce.data.model.UserReportReceiptDto
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.screens.products.productStockTone
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.model.VerifyLinkRequest
import ma.etherealmber.atlascommerce.ui.screens.agent.AgentViewModel
import ma.etherealmber.atlascommerce.ui.screens.devices.QrTokenParser
import ma.etherealmber.atlascommerce.ui.screens.devices.DesktopPhoneQrParser
import ma.etherealmber.atlascommerce.ui.screens.devices.DesktopPhoneApprovalRules
import ma.etherealmber.atlascommerce.ui.screens.devices.SingleUseDecisionGate
import ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

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
    fun desktopPhoneV2ParserIsStrictlyPinnedToHttpsOriginAndExplicitPort() {
        val id = "0123456789abcdef".repeat(4)
        val valid = "https://shop.example:8443/login/desktop-phone#request_id=$id"
        assertEquals("https://shop.example:8443", DesktopPhoneQrParser.parse(valid, "https://shop.example:8443")?.origin)
        assertEquals(id, DesktopPhoneQrParser.parse(valid, "https://shop.example:8443")?.requestId)
        assertNull("v2 QR identifiers must never enter the legacy token parser", QrTokenParser.extractQrToken(valid))
        assertNull(DesktopPhoneQrParser.parse(valid, "https://shop.example:9443"))
        assertNull(DesktopPhoneQrParser.parse(valid, "https://shop.example"))
        assertNull(DesktopPhoneQrParser.parse("http://shop.example:8443/login/desktop-phone#request_id=$id", "https://shop.example:8443"))
        val trailingSlashVariant = "https://shop.example:8443/login/desktop-phone/?token=legacytoken#request_id=$id"
        assertTrue(DesktopPhoneQrParser.isCandidate(trailingSlashVariant))
        assertNull(DesktopPhoneQrParser.parse(trailingSlashVariant, "https://shop.example:8443"))
        assertNull(DesktopPhoneQrParser.parse("https://shop.example:8443/login/desktop-phone/?#request_id=$id", "https://shop.example:8443"))
        assertNull(DesktopPhoneQrParser.parse("https://shop.example:8443/login/desktop-phone?x=1#request_id=$id", "https://shop.example:8443"))
        assertNull(DesktopPhoneQrParser.parse("https://shop.example:8443/login/desktop-phone#request_id=$id&extra=1", "https://shop.example:8443"))
        assertNull(DesktopPhoneQrParser.parse("https://user@shop.example:8443/login/desktop-phone#request_id=$id", "https://shop.example:8443"))
        assertNull(DesktopPhoneQrParser.parse("https://shop.example:8443/login/desktop-phone#request_id=not-a-hex-id", "https://shop.example:8443"))
    }

    @Test
    fun desktopPhoneConsentApprovalDenialExpiryAndReplayAreFailClosed() {
        val now = 1_900_000_000_000L
        val requestExpiry = "2030-03-17T17:51:40Z"
        val consentExpiry = "2030-03-17T17:48:40Z"
        assertFalse(DesktopPhoneApprovalRules.canDecide(false, requestExpiry, consentExpiry, now))
        assertTrue(DesktopPhoneApprovalRules.canDecide(true, requestExpiry, consentExpiry, now))
        assertFalse(DesktopPhoneApprovalRules.canDecide(true, "2030-01-01T00:00:00Z", consentExpiry, now))
        assertFalse(DesktopPhoneApprovalRules.canDecide(true, requestExpiry, "not-a-date", now))
        assertFalse(DesktopPhoneApprovalRules.isValidWindow(requestExpiry, "2032-01-01T00:00:00Z", now))
        assertFalse(DesktopPhoneApprovalRules.isValidWindow(requestExpiry, "2031-03-17T17:48:41Z", now))
        val fixedRequestExpiry = java.time.Instant.ofEpochMilli(now + 300_000L).toString()
        assertTrue(DesktopPhoneApprovalRules.isValidWindow(
            fixedRequestExpiry,
            java.time.Instant.ofEpochMilli(now + 120_000L).toString(),
            now,
        ))
        assertFalse(DesktopPhoneApprovalRules.isValidWindow(
            fixedRequestExpiry,
            java.time.Instant.ofEpochMilli(now + 120_001L).toString(),
            now,
        ))
        assertFalse(DesktopPhoneApprovalRules.isValidWindow(
            fixedRequestExpiry,
            java.time.Instant.ofEpochMilli(now).toString(),
            now,
        ))
        assertTrue(DesktopPhoneApprovalRules.matchesResponse(true, "approved"))
        assertFalse(DesktopPhoneApprovalRules.matchesResponse(true, "denied"))
        assertTrue(DesktopPhoneApprovalRules.matchesResponse(false, "denied"))
        assertFalse(DesktopPhoneApprovalRules.matchesResponse(false, "approved"))

        val gate = SingleUseDecisionGate()
        assertFalse(gate.begin(false, requestExpiry, consentExpiry, now))
        assertTrue(gate.begin(true, requestExpiry, consentExpiry, now))
        assertFalse("A one-time request must not be replayed", gate.begin(true, requestExpiry, consentExpiry, now))

        val adapter = moshi.adapter(DesktopPhoneDecisionRequest::class.java)
        val payload = adapter.toJson(DesktopPhoneDecisionRequest("https://shop.example:8443", "a".repeat(64), "b".repeat(64), "approve", true))
        assertTrue(payload.contains("\"consent_nonce\""))
        assertTrue(payload.contains("\"compare_confirmed\":true"))
        assertTrue(payload.contains("\"decision\":\"approve\""))
    }

    @Test
    fun verifyLinkParserAcceptsSignedEmailLinksAndRejectsEverythingElse() {
        val webLink =
            "https://stock.afmarbre.com/verify?user=Alice&code=482910&sig=9f86d081884c7d659a2feaa0&exp=1761200000000"
        val customWorkerWebLink =
            "https://erp.moncommerce.ma/verify?user=Admin%40Shop&code=930412&sig=abcdef1234567890&exp=1761200500000"
        val intentLink =
            "atlascommerce://verify?user=Amine&code=739201&sig=deadbeefcafe1234&exp=1761200999000"

        assertEquals(
            VerifyLink(
                username = "alice",
                otp = "482910",
                sig = "9f86d081884c7d659a2feaa0",
                exp = 1761200000000L,
            ),
            VerifyLinkParser.parse(webLink),
        )
        assertEquals("https://stock.afmarbre.com", VerifyLinkParser.extractServerOrigin(webLink))

        assertEquals(
            VerifyLink(
                username = "admin@shop",
                otp = "930412",
                sig = "abcdef1234567890",
                exp = 1761200500000L,
            ),
            VerifyLinkParser.parse(customWorkerWebLink),
        )
        assertEquals("https://erp.moncommerce.ma", VerifyLinkParser.extractServerOrigin(customWorkerWebLink))

        assertEquals(
            VerifyLink(
                username = "amine",
                otp = "739201",
                sig = "deadbeefcafe1234",
                exp = 1761200999000L,
            ),
            VerifyLinkParser.parse(intentLink),
        )
        assertNull(VerifyLinkParser.extractServerOrigin(intentLink))

        // Zero cross-talk with QR pairing parser
        assertNull(QrTokenParser.extractQrToken(webLink))
        assertNull(QrTokenParser.extractQrToken(customWorkerWebLink))
        assertNull(QrTokenParser.extractQrToken(intentLink))

        // Reject QR links, unsigned/incomplete links, short codes, null & EAN barcodes
        assertNull(VerifyLinkParser.parse("atlascommerce://qr?token=abc123XYZ_-9876543210"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/login-qr?t=qr_tok_98ab76cd54ef321098ab"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/qr?token=qr_tok_98ab76cd54ef321098ab"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/verify?user=alice&code=482910&exp=1761200000000"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/verify?user=alice&code=123&sig=deadbeef&exp=1761200000000"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/verify?user=alice&code=482910&sig=deadbeef&exp=0"))
        assertNull(VerifyLinkParser.parse("https://stock.afmarbre.com/verify?code=482910&sig=deadbeef&exp=1761200000000"))
        assertNull(VerifyLinkParser.parse("http://stock.afmarbre.com/verify?user=alice&code=482910&sig=deadbeef&exp=1761200000000"))
        assertNull(VerifyLinkParser.parse(null))
        assertNull(VerifyLinkParser.parse(""))
        assertNull(VerifyLinkParser.parse("6111035000214"))

        // VerifyLinkRequest JSON contract matches POST /api/auth/verify-link
        val reqAdapter = moshi.adapter(VerifyLinkRequest::class.java)
        val encoded = reqAdapter.toJson(
            VerifyLinkRequest(
                username = "alice",
                otp = "482910",
                sig = "9f86d081884c7d659a2feaa0",
                exp = 1761200000000L,
                fingerprint = "fp_sha256_abc",
            ),
        )
        val decoded = reqAdapter.fromJson(encoded)
        assertEquals("android", decoded?.client)
        assertEquals("482910", decoded?.otp)
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
        val android = TrustedDeviceDto(
            userAgent = "Atlas-Android/1.8.1 (Android 17; Subsystem for Android(TM))",
            current = true,
        )
        assertEquals("Atlas Android · Windows Subsystem for Android", android.friendlyLabel)
    }

    @Test
    fun productStockToneUsesRequestedThresholds() {
        assertEquals(AtlasTone.DANGER, productStockTone(19.99))
        assertEquals(AtlasTone.WARNING, productStockTone(20.0))
        assertEquals(AtlasTone.WARNING, productStockTone(40.0))
        assertEquals(AtlasTone.POSITIVE, productStockTone(40.01))
        assertEquals(AtlasTone.POSITIVE, productStockTone(51.0))
        assertEquals(AtlasTone.NEUTRAL, productStockTone(0.0, trackInventory = false))
    }

    @Test
    fun privateSnakeScoreUsesTheUnwrappedSingleFieldContract() {
        val adapter = Moshi.Builder().build().adapter(SnakePersonalBestDto::class.java)
        assertEquals("{\"personalBest\":123}", adapter.toJson(SnakePersonalBestDto(123)))
        assertEquals(123, adapter.fromJson("{\"personalBest\":123}")?.personalBest)
    }

    @Test
    fun reportReceiptCanRepresentAnAlreadyReportedClosedIssue() {
        val adapter = Moshi.Builder().build().adapter(UserReportReceiptDto::class.java)
        val receipt = adapter.fromJson(
            """{"reference":"atlas-42","issue_url":"https://github.com/Etherealmber/atlas-commerce-android/issues/42","already_reported":true,"issue_status":"closed"}""",
        )
        assertEquals(true, receipt?.alreadyReported)
        assertEquals("closed", receipt?.issueStatus)
    }

    @Test
    fun productNeededQuantityAndNewDtosRoundTripCleanly() {
        val low = ProductDto(id = 1, name = "Granit Noir", stock = 2.0, lowStockThresholdRaw = 10.0)
        assertEquals(8.0, low.neededQuantity, 0.001)

        val healthy = ProductDto(id = 2, name = "Colle Pro", stock = 25.0, lowStockThresholdRaw = 10.0)
        assertEquals(0.0, healthy.neededQuantity, 0.001)

        assertTrue(Roles.canMutateStock("owner"))
        assertTrue(Roles.canMutateStock("admin"))
        assertFalse(Roles.canMutateStock("magasinier"))
        assertFalse(Roles.canAdjustStock("magasinier"))
        assertTrue(Roles.canRequestStockAdjustment("magasinier"))
        assertTrue(Roles.canAdjustStock("secretaire"))
        assertFalse(Roles.canMutateStock("user"))
        assertFalse(Roles.canAdjustStock("user"))
        assertTrue(Roles.canRequestStockAdjustment("user"))

        val stockRequest = moshi.adapter(StockAdjustmentRequestDto::class.java)
            .fromJson("""{"id":2,"product_id":7,"product_name":"Tile","direction":"out","quantity":2,"requested_by":"staff","stock_before":10,"stock_after":8,"reason":null,"status":"pending"}""")
        assertNull(stockRequest?.reason)
        assertEquals("Tile", stockRequest?.productName)
        assertEquals("staff", stockRequest?.requestedBy)
        assertEquals(10.0, stockRequest?.stockBefore!!, 0.0)
        assertEquals(8.0, stockRequest?.stockAfter!!, 0.0)

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

    @Test
    fun agentPolicyQuotaCalculationsAndWebThreadIdsMatchWebContract() {
        val threadId = AgentViewModel.newWebCompatibleThreadId()
        assertTrue(threadId.matches(Regex("^t-[0-9a-z]+$")))
        assertFalse(threadId.startsWith("android-"))

        val normalHalf = AgentPolicyDto(
            blocked = false,
            isAdmin = false,
            dailyUsed = 12_500,
            dailyLimit = 25_000,
            weeklyUsed = 100_000,
            weeklyLimit = 100_000,
        )
        assertEquals(0.5f, normalHalf.dailyFraction, 0.001f)
        assertFalse(normalHalf.isDailyExhausted)
        assertTrue(normalHalf.isWeeklyExhausted)
        assertTrue(normalHalf.isExhausted)

        val adminOverLimit = AgentPolicyDto(
            blocked = false,
            isAdmin = true,
            dailyUsed = 80_000,
            dailyLimit = 25_000,
            weeklyUsed = 250_000,
            weeklyLimit = 100_000,
        )
        assertFalse("Admins bypass quota exhaustion", adminOverLimit.isExhausted)

        val detail = moshi.adapter(AgentThreadDetailDto::class.java)
            .fromJson("""{"title":"Ventes du jour","messages":[{"role":"user","text":"CA ?"},{"role":"assistant","text":"12 400 DH","ms":420}]}""")
        assertEquals("Ventes du jour", detail?.title)
        assertEquals(2, detail?.messages?.size)

        // Never pre-populate stock.afmarbre.com in UI server presets
        assertTrue(ServerUrl.presets().none { it.url.contains("afmarbre", ignoreCase = true) })
    }

    @Test
    fun offlinePayrollSnapshotIsEncryptedAndAccountScoped() = runBlocking {
        val dir = Files.createTempDirectory("atlas-finance-cache-test").toFile()
        try {
            val cipher = object : AtRestCipher {
                override fun encryptAtRest(plain: String): String = "test:" +
                    java.util.Base64.getEncoder().encodeToString(plain.toByteArray(Charsets.UTF_8))

                override fun decryptAtRest(stored: String?): String? = stored
                    ?.takeIf { it.startsWith("test:") }
                    ?.removePrefix("test:")
                    ?.let { String(java.util.Base64.getDecoder().decode(it), Charsets.UTF_8) }
            }
            val cache = OfflineCache(dir, moshi, cipher)
            cache.bindScope("https://alpha.example.com", "administrator")
            val administratorScope = cache.scopeFor("https://alpha.example.com", "administrator")!!
            val finance = FinanceMonthlyDto(period = "2026-11", currency = "MAD", closingBalance = 4321.0)
            val payroll = PayrollPageDto(
                period = "2026-11",
                rows = listOf(PayrollRowDto(employeeId = 7, displayName = "Private payroll name", amountDue = 125.0)),
            )
            cache.saveFinanceBundle(finance, payroll, isLatest = true, scope = administratorScope)

            val persisted = dir.listFiles()!!.filter { it.name.startsWith("finance_") }
            assertTrue(persisted.isNotEmpty())
            assertTrue(persisted.none { it.readText().contains("Private payroll name") })
            assertEquals("Private payroll name", cache.readFinanceBundle("2026-11")?.payroll?.rows?.single()?.displayName)
            assertEquals("2026-11", cache.readFinanceBundle()?.finance?.period)

            cache.bindScope("https://alpha.example.com", "another-admin")
            assertNull(cache.readFinanceBundle("2026-11"))
            cache.bindScope("https://beta.example.com", "administrator")
            assertNull(cache.readFinanceBundle())
            assertEquals(
                "Private payroll name",
                cache.readFinanceBundle("2026-11", scope = administratorScope)?.payroll?.rows?.single()?.displayName,
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun offlineCacheStrictlyIsolatesEveryUserAndEveryServerWithZeroLeak() = runBlocking {
        val dir = Files.createTempDirectory("atlas-cache-test").toFile()
        try {
            // Legacy global file from older builds must be purged automatically on startup
            File(dir, "products.json").writeText("""[{"id":999,"name":"Legacy Leak"}]""")

            val testCipher = object : AtRestCipher {
                override fun encryptAtRest(plain: String): String = "test:" +
                    java.util.Base64.getEncoder().encodeToString(plain.toByteArray(Charsets.UTF_8))

                override fun decryptAtRest(stored: String?): String? = stored
                    ?.takeIf { it.startsWith("test:") }
                    ?.removePrefix("test:")
                    ?.let { String(java.util.Base64.getDecoder().decode(it), Charsets.UTF_8) }
            }
            val cache = OfflineCache(dir, moshi, testCipher)
            assertFalse("Legacy global products.json must be deleted on init", File(dir, "products.json").exists())
            assertTrue("Unbound cache must return empty list", cache.readProducts().isEmpty())

            // User A on Server 1 writes secret products
            cache.bindScope("https://alpha.example.com", "alice")
            cache.saveProducts(listOf(ProductDto(id = 1, name = "Alpha Secret Marble")))
            val persistedCatalog = dir.listFiles()!!.single { it.name.startsWith("products_") }
            assertFalse("Offline catalog must not persist product names in plaintext", persistedCatalog.readText().contains("Alpha Secret Marble"))
            assertEquals("Alpha Secret Marble", cache.readProducts().single().name)

            // Switching to User B on the SAME Server 1 sees ZERO data from User A
            cache.bindScope("https://alpha.example.com", "bob")
            assertTrue("User B on same server must see zero items from User A", cache.readProducts().isEmpty())
            cache.saveProducts(listOf(ProductDto(id = 2, name = "Bob Only Granite")))

            // Switching to User A (same username!) on Server 2 sees ZERO data from Server 1
            cache.bindScope("https://beta.example.com", "alice")
            assertTrue("Same username on different server must see zero items", cache.readProducts().isEmpty())

            // Switching back to User A on Server 1 restores ONLY User A's Server 1 cache
            cache.bindScope("https://alpha.example.com", "alice")
            assertEquals("Alpha Secret Marble", cache.readProducts().single().name)

            // Unbinding on sign-out immediately exposes zero cached items
            cache.unbindScope()
            assertTrue(cache.readProducts().isEmpty())

            // Clearing Server 1 wipes both Alice & Bob on Server 1 completely
            cache.clearServerScope("https://alpha.example.com")
            cache.bindScope("https://alpha.example.com", "alice")
            assertTrue(cache.readProducts().isEmpty())
            cache.bindScope("https://alpha.example.com", "bob")
            assertTrue(cache.readProducts().isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
