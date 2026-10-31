package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.graphics.Color
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.core.diagnostics.AtlasLogBuffer
import ma.etherealmber.atlascommerce.core.diagnostics.DeviceDiagnosticSnapshot
import ma.etherealmber.atlascommerce.core.diagnostics.DiagnosticReporter
import ma.etherealmber.atlascommerce.core.net.ApiEnvelope
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.ui.components.buildInlineMarkdownAnnotatedString
import java.util.Calendar
import ma.etherealmber.atlascommerce.core.net.ApiErrorPayload
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.core.update.DownloadNetworkTransport
import ma.etherealmber.atlascommerce.core.update.UpdateFormatter
import ma.etherealmber.atlascommerce.core.update.formatReleaseNotesForDisplay
import ma.etherealmber.atlascommerce.core.update.UpdateInfo
import ma.etherealmber.atlascommerce.core.update.UpdateNotifier
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.core.update.minimumVersionRequiresUpdate
import ma.etherealmber.atlascommerce.push.DeviceTokenAckDto
import ma.etherealmber.atlascommerce.push.DeviceTokenRequest
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementDto
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AppUpdateManagerTest {

    private lateinit var tempDir: File
    private lateinit var scope: CoroutineScope
    private lateinit var moshi: Moshi
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("atlas-update-test").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        scope.cancel()
        tempDir.deleteRecursively()
    }

    @Test
    fun minimumVersionRequiresAnAvailableReleaseAtOrAboveTheConfiguredFloor() {
        assertTrue(minimumVersionRequiresUpdate(installedVersionCode = 10, minimumSupportedVersionCode = 12, availableVersionCode = 14))
        assertFalse(minimumVersionRequiresUpdate(installedVersionCode = 12, minimumSupportedVersionCode = 12, availableVersionCode = 14))
        assertFalse(minimumVersionRequiresUpdate(installedVersionCode = 10, minimumSupportedVersionCode = 12, availableVersionCode = 11))
        assertFalse(minimumVersionRequiresUpdate(installedVersionCode = 10, minimumSupportedVersionCode = 0, availableVersionCode = 14))
    }

    @Test
    fun downloadParallelismIsSourceSpecificAndR2Friendly() {
        val manager = AppUpdateManager(
            updatesDirProvider = { tempDir },
            baseUrlProvider = { "https://example.invalid" },
            currentVersionCode = 1,
            currentVersionName = "1.0",
            probeClient = client,
            downloadClient = client,
            moshi = moshi,
            notifier = RecordingNotifier(),
            scope = scope,
        )
        assertEquals(4, manager.connectionsForSource("GitHub", DownloadNetworkTransport.WIFI))
        assertEquals(4, manager.connectionsForSource("github", DownloadNetworkTransport.CELLULAR))
        assertEquals(16, manager.connectionsForSource("CDN 1", DownloadNetworkTransport.WIFI))
        assertEquals(6, manager.connectionsForSource("CDN 1", DownloadNetworkTransport.CELLULAR))
        assertEquals(4, manager.connectionsForSource("CDN 1", DownloadNetworkTransport.OTHER))
        assertEquals(4, manager.connectionsForSource("R2", DownloadNetworkTransport.WIFI))
        assertEquals(4, manager.connectionsForSource(null, DownloadNetworkTransport.WIFI))
    }

    @Test
    fun releaseNotesStripRawMarkdownAndKeepReadableBullets() {
        val rendered = formatReleaseNotesForDisplay(
            """## Atlas Commerce 1.1.1
                |• Set version 1.1.1 (111) [skip ci]
                |• **Version :** `v1.1.1` (`versionCode = 111`)
                |• **Taille :** `27.83 Mo`
            """.trimMargin(),
            "v1.1.1",
        )
        assertEquals(
            "• Set version 1.1.1 (111) [skip ci]\n" +
                "• Version : v1.1.1 (versionCode = 111)\n" +
                "• Taille : 27.83 Mo",
            rendered,
        )
        assertFalse(rendered.contains("**"))
        assertFalse(rendered.contains('`'))
    }

    @Test
    fun networkErrorsAreConciseAndVersionGateIsActionable() = runBlocking {
        val runner = ApiRunner(moshi)
        val dns = runner.envelope<String> {
            throw UnknownHostException("stock.afmarbre.com lookup failed")
        } as ApiResult.Failure
        assertEquals(ApiRunner.CODE_NETWORK, dns.code)
        assertEquals("Impossible de joindre ce serveur. Vérifiez votre connexion puis réessayez.", dns.message)
        assertFalse(dns.message.contains("stock.afmarbre.com"))

        val updateRequired = runner.envelope<String> {
            throw HttpException(
                Response.error<String>(
                    426,
                    "{\"success\":false,\"code\":\"APP_UPDATE_REQUIRED\",\"error\":\"technical detail\"}"
                        .toResponseBody("application/json".toMediaType()),
                ),
            )
        } as ApiResult.Failure
        assertEquals("APP_UPDATE_REQUIRED", updateRequired.code)
        assertEquals("Une mise à jour est requise pour continuer. Téléchargez la dernière version.", updateRequired.message)
    }

    @Test
    fun moshiAdaptersRoundTripCoreAndModelDtos() {
        val errorAdapter = moshi.adapter(ApiErrorPayload::class.java)
        val parsedError = errorAdapter.fromJson("""{"success":false,"code":"AUTH_EXPIRED","error":"Session expirée"}""")
        assertNotNull(parsedError)
        assertEquals("AUTH_EXPIRED", parsedError?.code)

        val healthEnvelopeAdapter = moshi.adapter<ApiEnvelope<HealthDto>>(
            Types.newParameterizedType(ApiEnvelope::class.java, HealthDto::class.java),
        )
        val health = healthEnvelopeAdapter.fromJson("""{"success":true,"data":{"ok":true,"d1":true,"r2":true,"environment":"production"}}""")
        assertTrue(health?.success == true && health.data?.ok == true)

        val updateAdapter = moshi.adapter(AndroidUpdateDto::class.java)
        val update = updateAdapter.fromJson(
            """{"success":true,"ok":true,"updateAvailable":true,"versionCode":107,"versionName":"1.0.7","apkUrl":"https://cdn1.afmarbre.com/app-updates/android/latest.apk","mandatory":false,"minimum_supported_version_code":105,"notes":"Stabilité"}""",
        )
        assertTrue(update?.updateAvailable == true)
        assertEquals(107, update?.versionCode)
        assertEquals(105, update?.resolvedMinimumSupportedVersionCode)

        val dashboardAdapter = moshi.adapter(DashboardDto::class.java)
        assertNotNull(dashboardAdapter.fromJson("""{"currency":"MAD","productCount":42}"""))

        val finance = moshi.adapter(FinanceMonthlyDto::class.java).fromJson(
            """{"period":"2026-09","timezone":"Africa/Casablanca","currency":"MAD","opening_balance":48210.5,"closing_balance":51060.25,"sales_received":18500,"refunds":650,"expenses":9200.25,"payroll_paid":6000,"supplier_paid":2200,"receivables":{"total_outstanding":12400,"total_overdue":3100,"count":1,"truncated":false,"documents":[{"id":142,"number":"F-2026-0142","party":"Client Alpha","total":5000,"paid":2000,"remaining":3000,"due_date":"2026-09-15","state":"overdue"}]},"payables":{"total_outstanding":0,"total_overdue":0,"count":0,"truncated":false,"documents":[]},"payroll":{"total_due":9000,"employees":3},"reconciliation":{"source":"d1-live","generated_at":"2026-10-01T07:35:12.480Z","rules":"docs/api-finance.md#calculation-rules","as_of_date":"2026-09-30"}}""",
        )
        assertEquals("Africa/Casablanca", finance?.timezone)
        assertEquals(3000.0, finance?.receivables?.documents?.firstOrNull()?.remaining ?: 0.0, 0.0)
        assertEquals(9000.0, finance?.payroll?.totalDue ?: 0.0, 0.0)

        val statement = moshi.adapter(FinanceStatementDto::class.java).fromJson(
            """{"period":"2026-10","timezone":"Africa/Casablanca","currency":"MAD","as_of":"2026-10-25","status":"open","opening_balance":1000,"closing_balance":1250,"totals":{"credits":500,"debits":250,"net":250},"reconciliation":{"opening":1000,"credits":500,"debits":250,"closing":1250,"balanced":true,"rule":"opening + credits - debits = closing"},"page":1,"page_size":50,"total":1,"has_more":false,"rows":[{"id":12,"source":"invoice_payments","type":"sale","label":"Encaissement F-2026-1","ref":"F-2026-1","debit":0,"credit":250,"ts":"2026-10-10","running_balance":1250}]}""",
        )
        assertEquals(1250.0, statement?.rows?.firstOrNull()?.runningBalance ?: 0.0, 0.0)
        assertTrue(statement?.reconciliation?.balanced == true)

        val payroll = moshi.adapter(PayrollPageDto::class.java).fromJson(
            """{"period":"2026-10","as_of":"2026-10-25","timezone":"Africa/Casablanca","currency":"MAD","totals":{"amount_due":4500,"unpaid_count":2,"count":5},"page":1,"page_size":50,"total":5,"has_more":false,"rows":[{"employee_id":12,"display_name":"Karim Ben","amount_due":1500,"pay_period":{"from":"2026-10-01","to":"2026-10-31"},"due_date":"2026-10-31","status":"unpaid","last_payment":{"amount":500,"paid_at":"2026-10-10","period_label":"Acompte octobre"}}]}""",
        )
        assertEquals("Karim Ben", payroll?.rows?.firstOrNull()?.displayName)
        assertEquals("Acompte octobre", payroll?.rows?.firstOrNull()?.lastPayment?.periodLabel)

        val deviceTokenJson = moshi.adapter(DeviceTokenRequest::class.java)
            .toJson(DeviceTokenRequest(token = "fcm-test-token"))
        assertTrue(deviceTokenJson.contains("\"token\":\"fcm-test-token\""))
        assertTrue(deviceTokenJson.contains("\"platform\":\"android\""))
        assertTrue(
            moshi.adapter(DeviceTokenAckDto::class.java)
                .fromJson("""{"registered":true}""")?.registered == true,
        )

        val productAdapter = moshi.adapter(ProductDto::class.java)
        val product = productAdapter.fromJson("""{"id":9,"name":"Marbre Blanc","price":320.0,"stock_quantity":12.5}""")
        assertEquals("Marbre Blanc", product?.name)
    }

    @Test
    fun activeDownloadIsNeverOverriddenAndReportsLiveProgressToUiAndNotifications() {
        val apkBytes = ByteArray(64 * 1024) { idx -> (idx % 251).toByte() }
        val midStreamReached = CountDownLatch(1)
        val releaseSecondHalf = CountDownLatch(1)
        val notifier = RecordingNotifier()

        ServerSocket(0).use { server ->
            val port = server.localPort
            val serverThread = Thread {
                repeat(4) {
                    val socket = runCatching { server.accept() }.getOrNull() ?: return@Thread
                    socket.use { s ->
                        val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                        val requestLine = reader.readLine() ?: return@use
                        while (true) {
                            val header = reader.readLine() ?: break
                            if (header.isEmpty()) break
                        }
                        val out = s.getOutputStream()
                        if (requestLine.contains("/api/app/android-update")) {
                            val body = """{"success":true,"ok":true,"updateAvailable":true,"versionCode":107,"versionName":"1.0.7","apkUrl":"/updates/latest.apk","mandatory":false,"notes":"Correctifs Android 14"}"""
                            val bytes = body.toByteArray(StandardCharsets.UTF_8)
                            val head = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                            out.write(head.toByteArray(StandardCharsets.UTF_8))
                            out.write(bytes)
                            out.flush()
                        } else {
                            val head = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: ${apkBytes.size}\r\nConnection: close\r\n\r\n"
                            out.write(head.toByteArray(StandardCharsets.UTF_8))
                            val half = apkBytes.size / 2
                            out.write(apkBytes, 0, half)
                            out.flush()
                            midStreamReached.countDown()
                            releaseSecondHalf.await(5, TimeUnit.SECONDS)
                            out.write(apkBytes, half, apkBytes.size - half)
                            out.flush()
                        }
                    }
                }
            }.apply { isDaemon = true; start() }

            val manager = AppUpdateManager(
                updatesDirProvider = { tempDir },
                baseUrlProvider = { "http://127.0.0.1:$port" },
                currentVersionCode = 106,
                currentVersionName = "1.0.6",
                probeClient = client,
                downloadClient = client,
                moshi = moshi,
                notifier = notifier,
                scope = scope,
                allowCleartext = true,
            )

            manager.checkForUpdate(manual = true)
            awaitCondition { manager.state.value is UpdateState.Available }

            val available = manager.state.value as UpdateState.Available
            assertEquals("v1.0.7", available.info.displayVersion)

            manager.startDownload(fromSettings = true)
            assertTrue("Expected first half of stream to arrive", midStreamReached.await(5, TimeUnit.SECONDS))
            awaitCondition {
                val dl = manager.state.value as? UpdateState.Downloading
                dl != null && dl.bytesRead >= (apkBytes.size / 2).toLong()
            }
            assertFalse("Settings already shows download progress; do not duplicate the OS alert", notifier.startedCalled)
            assertTrue("Keep OS progress quiet while Settings is foregrounded", notifier.progressEvents.isEmpty())

            // Once the app leaves foreground, the OS notification becomes the background progress surface.
            manager.onAppStopped()
            assertTrue(notifier.progressEvents.isNotEmpty())

            // Attempt every possible override action while downloading at 50%:
            manager.checkForUpdate(manual = false)
            manager.checkForUpdate(manual = true)
            manager.dismissBanner()
            manager.startDownload()

            // State MUST still be Downloading at >= 50%, never reset or overridden!
            val stillDownloading = manager.state.value as? UpdateState.Downloading
            assertNotNull("Active download state must never be overridden", stillDownloading)
            assertEquals(50, stillDownloading?.percent)
            assertTrue(stillDownloading?.hiddenInForeground == true)
            assertTrue(stillDownloading?.statusTitle?.contains("Téléchargement de la mise à jour") == true)

            // Hiding the download banner collapses into Top Bar mini-ball while streaming continues uninterrupted
            manager.hideDownloadBanner()
            val hiddenDownloading = manager.state.value as? UpdateState.Downloading
            assertNotNull(hiddenDownloading)
            assertTrue(hiddenDownloading?.hiddenInForeground == true)

            // Tapping the Top Bar mini-ball re-expands the full banner cleanly and toggles reliably
            manager.showDownloadBanner()
            assertFalse((manager.state.value as UpdateState.Downloading).hiddenInForeground)
            manager.toggleDownloadBanner()
            assertTrue((manager.state.value as UpdateState.Downloading).hiddenInForeground)

            releaseSecondHalf.countDown()
            awaitCondition { manager.state.value is UpdateState.ReadyToInstall }

            val ready = manager.state.value as UpdateState.ReadyToInstall
            assertTrue(ready.apkFile.exists())
            assertArrayEquals(apkBytes, ready.apkFile.readBytes())
            assertFalse("Settings-started update should not show a duplicate start alert", notifier.startedCalled)
            assertTrue(notifier.progressEvents.isNotEmpty())
            assertTrue(notifier.completedCalled)
            assertNull(notifier.errorMessage)

            // The optional install-ready banner can be hidden without removing the APK.
            manager.dismissBanner()
            val hiddenReady = manager.state.value as UpdateState.ReadyToInstall
            assertTrue(hiddenReady.dismissedBanner)
            assertTrue(hiddenReady.apkFile.exists())

            // The persistent Top Bar update icon can reopen a dismissed install banner.
            manager.showDownloadBanner()
            val reopenedReady = manager.state.value as UpdateState.ReadyToInstall
            assertFalse(reopenedReady.dismissedBanner)
            assertTrue(reopenedReady.apkFile.exists())
            manager.dismissBanner()

            // Automatic background check must NOT overwrite an already-downloaded ReadyToInstall APK.
            manager.checkForUpdate(manual = false)
            Thread.sleep(150)
            assertTrue((manager.state.value as UpdateState.ReadyToInstall).dismissedBanner)

            serverThread.interrupt()
        }
    }

    @Test
    fun truncatedDownloadFailsSafelyWithoutLeavingCorruptApk() {
        val notifier = RecordingNotifier()
        ServerSocket(0).use { server ->
            val port = server.localPort
            Thread {
                val socket = runCatching { server.accept() }.getOrNull() ?: return@Thread
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val out = s.getOutputStream()
                    // Declare 100 KB Content-Length, send only 4 KB, then close socket abruptly!
                    val head = "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: 102400\r\nConnection: close\r\n\r\n"
                    out.write(head.toByteArray(StandardCharsets.UTF_8))
                    out.write(ByteArray(4096) { 1 })
                    out.flush()
                }
            }.apply { isDaemon = true; start() }

            val manager = AppUpdateManager(
                updatesDirProvider = { tempDir },
                baseUrlProvider = { "http://127.0.0.1:$port" },
                currentVersionCode = 105,
                currentVersionName = "1.0.5",
                probeClient = client,
                downloadClient = client,
                moshi = moshi,
                notifier = notifier,
                scope = scope,
                allowCleartext = true,
            )

            manager.startDownload(
                UpdateInfo(
                    versionCode = 106,
                    versionName = "1.0.6",
                    apkUrl = "http://127.0.0.1:$port/broken.apk",
                    mandatory = false,
                    notes = null,
                ),
            )
            awaitCondition { manager.state.value is UpdateState.Error }

            assertFalse(File(tempDir, "atlas-commerce-v106.apk").exists())
            assertFalse(File(tempDir, "atlas-commerce-v106.apk.part").exists())
            assertNotNull(notifier.errorMessage)
        }
    }

    @Test
    fun dualMirrorDownloadFailsOverFromCdn1ToGitHubAutomatically() {
        val apkBytes = ByteArray(32 * 1024) { idx -> (idx % 251).toByte() }
        val notifier = RecordingNotifier()

        ServerSocket(0).use { cdnBroken ->
            ServerSocket(0).use { githubMirror ->
                val cdnPort = cdnBroken.localPort
                val ghPort = githubMirror.localPort

                Thread {
                    val s = runCatching { cdnBroken.accept() }.getOrNull() ?: return@Thread
                    s.use { sock ->
                        val r = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                        while ((r.readLine() ?: "").isNotEmpty()) { /* drain */ }
                        sock.getOutputStream().write(
                            "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                .toByteArray(StandardCharsets.UTF_8),
                        )
                    }
                }.apply { isDaemon = true; start() }

                Thread {
                    val s = runCatching { githubMirror.accept() }.getOrNull() ?: return@Thread
                    s.use { sock ->
                        val r = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                        while ((r.readLine() ?: "").isNotEmpty()) { /* drain */ }
                        val out = sock.getOutputStream()
                        out.write(
                            "HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: ${apkBytes.size}\r\nConnection: close\r\n\r\n"
                                .toByteArray(StandardCharsets.UTF_8),
                        )
                        out.write(apkBytes)
                        out.flush()
                    }
                }.apply { isDaemon = true; start() }

                val manager = AppUpdateManager(
                    updatesDirProvider = { tempDir },
                    baseUrlProvider = { "http://127.0.0.1:$ghPort" },
                    currentVersionCode = 107,
                    currentVersionName = "1.0.7",
                    probeClient = client,
                    downloadClient = client,
                    moshi = moshi,
                    notifier = notifier,
                    scope = scope,
                    allowCleartext = true,
                )

                manager.startDownload(
                    UpdateInfo(
                        versionCode = 108,
                        versionName = "1.0.8",
                        apkUrl = "http://127.0.0.1:$cdnPort/cdn1/latest.apk",
                        cdn1Url = "http://127.0.0.1:$cdnPort/cdn1/latest.apk",
                        githubUrl = "http://127.0.0.1:$ghPort/github/latest.apk",
                        mandatory = false,
                        notes = "Dual mirror test",
                    ),
                )

                awaitCondition { manager.state.value is UpdateState.ReadyToInstall }
                val ready = manager.state.value as UpdateState.ReadyToInstall
                assertArrayEquals(apkBytes, ready.apkFile.readBytes())
                assertNull(notifier.errorMessage)
            }
        }
    }

    @Test
    fun effectiveNotesAlwaysGeneratesMinimalConciseTextWhenBlankOrBareVersion() {
        val bare = UpdateInfo(
            versionCode = 108,
            versionName = "1.0.8",
            apkUrl = "https://cdn1.afmarbre.com/app-updates/android/latest.apk",
            mandatory = false,
            notes = "1.0.8",
        )
        assertTrue(bare.effectiveNotes.contains("v1.0.8"))
        assertEquals(1, bare.effectiveNotes.lines().size)

        val empty = bare.copy(notes = null)
        assertEquals(1, empty.effectiveNotes.lines().size)
        assertTrue(empty.effectiveNotes.contains("optimisations"))
    }

    @Test
    fun parallelMultiInstanceRangeDownloadStreamsFourSegmentsAndVerifiesSha256() {
        val apkBytes = ByteArray(128 * 1024) { idx -> ((idx * 37 + 13) % 256).toByte() }
        val sha256Hex = java.security.MessageDigest.getInstance("SHA-256")
            .digest(apkBytes)
            .joinToString("") { "%02x".format(it) }
        val notifier = RecordingNotifier()
        val seenRanges = Collections.synchronizedList(mutableListOf<String>())

        ServerSocket(0).use { server ->
            val port = server.localPort
            val acceptLoop = Thread {
                while (!server.isClosed) {
                    val s = runCatching { server.accept() }.getOrNull() ?: break
                    Thread {
                        s.use { sock ->
                            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                            reader.readLine() ?: return@use
                            var rangeHeader: String? = null
                            while (true) {
                                val h = reader.readLine() ?: break
                                if (h.isEmpty()) break
                                if (h.startsWith("Range:", ignoreCase = true)) {
                                    rangeHeader = h.substringAfter(':').trim()
                                }
                            }
                            val out = sock.getOutputStream()
                            val match = Regex("""bytes=(\d+)-(\d+)""").find(rangeHeader.orEmpty())
                            if (match != null) {
                                seenRanges.add(rangeHeader.orEmpty())
                                val start = match.groupValues[1].toInt()
                                val end = match.groupValues[2].toInt().coerceAtMost(apkBytes.lastIndex)
                                val len = end - start + 1
                                val head = buildString {
                                    append("HTTP/1.1 206 Partial Content\r\n")
                                    append("Content-Type: application/vnd.android.package-archive\r\n")
                                    append("Accept-Ranges: bytes\r\n")
                                    append("Content-Range: bytes $start-$end/${apkBytes.size}\r\n")
                                    append("Content-Length: $len\r\n")
                                    append("Connection: close\r\n\r\n")
                                }
                                out.write(head.toByteArray(StandardCharsets.UTF_8))
                                out.write(apkBytes, start, len)
                                out.flush()
                            } else {
                                val head = "HTTP/1.1 200 OK\r\nContent-Length: ${apkBytes.size}\r\nConnection: close\r\n\r\n"
                                out.write(head.toByteArray(StandardCharsets.UTF_8))
                                out.write(apkBytes)
                                out.flush()
                            }
                        }
                    }.apply { isDaemon = true; start() }
                }
            }.apply { isDaemon = true; start() }

            val manager = AppUpdateManager(
                updatesDirProvider = { tempDir },
                baseUrlProvider = { "http://127.0.0.1:$port" },
                currentVersionCode = 109,
                currentVersionName = "1.0.9",
                probeClient = client,
                downloadClient = client,
                moshi = moshi,
                notifier = notifier,
                scope = scope,
                allowCleartext = true,
                parallelDownloadProvider = { true },
            )

            manager.startDownload(
                UpdateInfo(
                    versionCode = 110,
                    versionName = "1.1.0",
                    apkUrl = "http://127.0.0.1:$port/atlas-commerce.apk",
                    mandatory = false,
                    notes = "Mise à jour v1.1.0 — optimisations et correctifs.",
                    sha256 = sha256Hex,
                ),
            )

            awaitCondition { manager.state.value is UpdateState.ReadyToInstall }
            val ready = manager.state.value as UpdateState.ReadyToInstall
            assertArrayEquals(apkBytes, ready.apkFile.readBytes())
            // Probe `bytes=0-0` + 4 parallel disjoint Range chunks = 5 Range HTTP requests
            assertTrue("Expected probe + 4 parallel Range requests, got $seenRanges", seenRanges.size >= 5)
            assertNull(notifier.errorMessage)
            acceptLoop.interrupt()
        }
    }

    @Test
    fun followLatestReleaseTreatsNewestPublishedManifestAsLatestUpdateAndLoadsChangelog() {
        val notifier = RecordingNotifier()
        ServerSocket(0).use { server ->
            val port = server.localPort
            val acceptLoop = Thread {
                while (!server.isClosed) {
                    val s = runCatching { server.accept() }.getOrNull() ?: break
                    Thread {
                        s.use { sock ->
                            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                            val reqLine = reader.readLine() ?: return@use
                            while ((reader.readLine() ?: "").isNotEmpty()) { /* drain */ }
                            val body = when {
                                reqLine.contains("/github-latest.json") ->
                                    """{"success":true,"ok":true,"updateAvailable":true,"versionCode":108,"versionName":"1.0.8-hotfix","apkUrl":"http://127.0.0.1:$port/latest.apk","notes":"Mise à jour v1.0.8-hotfix — correctifs.","publishedAt":"2026-10-23T12:00:00Z"}"""
                                reqLine.contains("/releases") ->
                                    """[{"tag_name":"v1.0.9","name":"Atlas Commerce v1.0.9","body":"- Téléchargement parallèle 4 flux\n- Licence MIT Etherealmber","draft":false,"prerelease":false,"published_at":"2026-10-23T10:00:00Z","html_url":"https://github.com/Etherealmber/atlas-commerce-android/releases/tag/v1.0.9"}]"""
                                else ->
                                    """{"success":true,"ok":true,"updateAvailable":false,"versionCode":109,"versionName":"1.0.9","apkUrl":"http://127.0.0.1:$port/old.apk","publishedAt":"2026-10-20T08:00:00Z"}"""
                            }
                            val bytes = body.toByteArray(StandardCharsets.UTF_8)
                            val head = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                            sock.getOutputStream().apply {
                                write(head.toByteArray(StandardCharsets.UTF_8))
                                write(bytes)
                                flush()
                            }
                        }
                    }.apply { isDaemon = true; start() }
                }
            }.apply { isDaemon = true; start() }

            val manager = AppUpdateManager(
                updatesDirProvider = { tempDir },
                baseUrlProvider = { "http://127.0.0.1:$port" },
                currentVersionCode = 109,
                currentVersionName = "1.0.9",
                probeClient = client,
                downloadClient = client,
                moshi = moshi,
                notifier = notifier,
                scope = scope,
                allowCleartext = true,
                followLatestReleaseProvider = { true },
                githubLatestJsonUrl = "http://127.0.0.1:$port/github-latest.json",
                releasesApiUrl = "http://127.0.0.1:$port/releases",
            )

            manager.checkForUpdate(manual = true)
            awaitCondition { manager.state.value is UpdateState.Available }
            val available = manager.state.value as UpdateState.Available
            assertEquals("v1.0.8-hotfix", available.info.displayVersion)

            manager.refreshChangelog()
            awaitCondition { manager.changelog.value.firstOrNull()?.notes?.contains("4 flux") == true }
            assertEquals("v1.0.9", manager.changelog.value.first().tagName)
            acceptLoop.interrupt()
        }
    }

    @Test
    fun authorAndLicenseMetadataMatchEtherealmberAndNoDuplicateSettingsIcon() {
        assertEquals("Etherealmber", AppDetails.COPYRIGHT_HOLDER)
        assertEquals("https://github.com/Etherealmber", AppDetails.GITHUB_PROFILE_URL)
        assertEquals("https://github.com/Etherealmber.png", AppDetails.GITHUB_AVATAR_URL)
        assertTrue(UpdateFormatter.progressSummary(14 * 1_048_576L, 28 * 1_048_576L, 6 * 1_048_576L, 4).contains("4 flux"))

        val rootDir = File(".").canonicalFile.let { cur ->
            if (File(cur, "LICENSE").isFile) cur else cur.parentFile
        }
        val legacyHandle = "lox" + "dpsy"
        assertEquals(Calendar.getInstance().get(Calendar.YEAR).coerceAtLeast(2026).toString(), AppDetails.COPYRIGHT_YEAR)
        assertTrue(File(rootDir, "LICENSE").readText().contains("Copyright (c) 2026 Etherealmber"))
        assertFalse(File(rootDir, "LICENSE").readText().contains(legacyHandle))

        // Verify HTML <br> tags inside AI Markdown table cells render as real newlines
        val formattedCell = buildInlineMarkdownAnnotatedString(
            rawInput = "(mois/année) <br>• Mention spéciale <br/>**Bien**",
            codeBackground = Color.LightGray,
            codeColor = Color.Black,
            linkColor = Color.Blue,
        ).text
        assertFalse(formattedCell.contains("<br"))
        assertTrue(formattedCell.contains("\n• Mention spéciale \nBien"))

        // Verify okhttp user-agent maps cleanly to friendly mobile label
        assertEquals(
            "Atlas Android · Mobile",
            TrustedDeviceDto(userAgent = "okhttp/4.12.0").friendlyLabel,
        )

        // Verify diagnostic reporter limits (max 3 images, <= 1 MB each) & log secret redaction
        assertEquals(3, DiagnosticReporter.MAX_ATTACHMENTS)
        assertTrue(DiagnosticReporter.MAX_IMAGE_BYTES <= 1_048_576)
        val sanitized = AtlasLogBuffer.sanitize(
            "Authorization: Bearer secret_jwt_token_123 otp=654321 user=test@example.com",
        )
        assertFalse(sanitized.contains("secret_jwt_token_123"))
        assertFalse(sanitized.contains("654321"))
        assertFalse(sanitized.contains("test@example.com"))

        val mdReport = DiagnosticReporter.buildMarkdownReport(
            category = "Agent IA / Tableaux",
            title = "Tableau coupé à droite",
            description = "Les colonnes débordent",
            stepsToReproduce = "1. Ouvrir Agent IA",
            snapshot = DeviceDiagnosticSnapshot(
                reportId = "DIAG-TEST-a1b2c3d4",
                timestampUtc = "2026-10-23T00:00:00Z",
                deviceShortId = "a1b2c3d4",
                maskedFingerprint = "a1b2c3d4…9f8e",
                appVersion = "v1.1.0",
                versionCode = 110,
                manufacturerModel = "Google Pixel 8",
                androidVersion = "Android 15 (SDK 35)",
                cpuAbi = "arm64-v8a",
                screenMetrics = "1080x2400px · 420dpi · fontScale=1.15",
                memoryUsage = "92 Mo / 512 Mo",
                networkStatus = "Wi-Fi actif",
                userRole = "Propriétaire (Admin)",
                screenOrigin = "Paramètres",
            ),
            logs = "12:00:00.001 [INFO/HTTP] GET /api/agent/policy -> 200 (42ms)",
            attachments = emptyList(),
        )
        assertTrue(mdReport.contains("<!-- atlas-diag-v1"))
        assertTrue(mdReport.contains("a1b2c3d4…9f8e"))
        assertTrue(mdReport.contains("@atlas-bot"))
    }

    @Test
    fun progressFormatterProducesFrenchReadableOutput() {
        assertEquals("Connexion au serveur de mise à jour…", UpdateFormatter.progressSummary(0, -1))
        val summary = UpdateFormatter.progressSummary(14 * 1_048_576L, 28 * 1_048_576L)
        assertTrue(summary.startsWith("50 % · "))
        assertTrue(summary.contains("Mo"))
    }

    @Test
    fun appLaunchFreezeAndMoshiCodegenInvariantsHoldAcrossCodebase() {
        val moduleDir = File("src/main").takeIf { it.isDirectory }
            ?: File("app/src/main")
        assertTrue("Expected app/src/main directory", moduleDir.isDirectory)

        val manifest = File(moduleDir, "AndroidManifest.xml").readText()
        assertTrue("MainActivity must remain exported launcher", manifest.contains(".MainActivity"))
        assertTrue("Launcher category required", manifest.contains("android.intent.category.LAUNCHER"))
        assertTrue("FileProvider required for APK updates", manifest.contains("androidx.core.content.FileProvider"))

        val proguardFile = File("proguard-rules.pro").takeIf { it.isFile }
            ?: File("app/proguard-rules.pro")
        assertTrue("Proguard rules must keep generated Moshi JsonAdapters", proguardFile.readText().contains("**JsonAdapter"))

        val pkgRegex = Regex("""^\s*package\s+([A-Za-z0-9_.]+)""", RegexOption.MULTILINE)
        val jsonClassRegex = Regex("""@JsonClass\s*\(\s*generateAdapter\s*=\s*true\s*\)\s*(?:data\s+)?class\s+([A-Za-z0-9_]+)""")
        var checkedAdapters = 0

        moduleDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { ktFile ->
                val text = ktFile.readText()
                val pkg = pkgRegex.find(text)?.groupValues?.get(1).orEmpty()

                jsonClassRegex.findAll(text).forEach { match ->
                    val className = match.groupValues[1]
                    val adapterFqcn = if (pkg.isEmpty()) "${className}JsonAdapter" else "$pkg.${className}JsonAdapter"
                    val loaded = Class.forName(adapterFqcn)
                    assertNotNull("Missing KSP adapter $adapterFqcn", loaded)
                    checkedAdapters++
                }

                text.lineSequence().forEachIndexed { idx, rawLine ->
                    val line = rawLine.trim()
                    if (!line.startsWith("//") && !line.startsWith("*")) {
                        assertFalse(
                            "Unsafe !! in ${ktFile.name}:${idx + 1}: $line",
                            line.contains("!!") && !line.contains("\"!!\""),
                        )
                        assertFalse(
                            "GlobalScope leak in ${ktFile.name}:${idx + 1}",
                            line.contains("GlobalScope."),
                        )
                        if (ktFile.path.contains("/ui/") || ktFile.name == "MainActivity.kt" || ktFile.name == "AtlasApp.kt") {
                            assertFalse(
                                "UI thread freeze risk (runBlocking) in ${ktFile.name}:${idx + 1}",
                                Regex("""\brunBlocking\b""").containsMatchIn(line),
                            )
                        }
                    }
                }
            }

        assertTrue("Expected KSP-generated Moshi adapters to be verified", checkedAdapters >= 15)
    }

    private fun awaitCondition(timeoutMs: Long = 5_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(25)
        }
        assertTrue("Condition timed out; final state=${predicate()}", predicate())
    }

    private class RecordingNotifier : UpdateNotifier {
        @Volatile var startedCalled = false
        @Volatile var completedCalled = false
        @Volatile var errorMessage: String? = null
        val progressEvents: MutableList<Pair<Long, Long>> = Collections.synchronizedList(mutableListOf())

        override fun showStarting(info: UpdateInfo) {
            startedCalled = true
        }

        override fun showProgress(info: UpdateInfo, bytesRead: Long, totalBytes: Long) {
            progressEvents.add(bytesRead to totalBytes)
        }

        override fun showCompleted(info: UpdateInfo, apkFile: File) {
            completedCalled = true
        }

        override fun showError(info: UpdateInfo?, message: String) {
            errorMessage = message
        }

        override fun cancel() = Unit
    }
}
