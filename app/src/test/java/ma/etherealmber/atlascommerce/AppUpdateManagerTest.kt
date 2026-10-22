package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ma.etherealmber.atlascommerce.core.net.ApiEnvelope
import ma.etherealmber.atlascommerce.core.net.ApiErrorPayload
import ma.etherealmber.atlascommerce.core.update.AppUpdateManager
import ma.etherealmber.atlascommerce.core.update.UpdateFormatter
import ma.etherealmber.atlascommerce.core.update.UpdateInfo
import ma.etherealmber.atlascommerce.core.update.UpdateNotifier
import ma.etherealmber.atlascommerce.core.update.UpdateState
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import okhttp3.OkHttpClient
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
            """{"success":true,"ok":true,"updateAvailable":true,"versionCode":107,"versionName":"1.0.7","apkUrl":"https://cdn1.afmarbre.com/app-updates/android/latest.apk","mandatory":false,"notes":"Stabilité"}""",
        )
        assertTrue(update?.updateAvailable == true)
        assertEquals(107, update?.versionCode)

        val dashboardAdapter = moshi.adapter(DashboardDto::class.java)
        assertNotNull(dashboardAdapter.fromJson("""{"currency":"MAD","productCount":42}"""))

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

            manager.startDownload()
            assertTrue("Expected first half of stream to arrive", midStreamReached.await(5, TimeUnit.SECONDS))
            awaitCondition {
                val dl = manager.state.value as? UpdateState.Downloading
                dl != null && dl.bytesRead >= (apkBytes.size / 2).toLong()
            }

            // Attempt every possible override action while downloading at 50%:
            manager.checkForUpdate(manual = false)
            manager.checkForUpdate(manual = true)
            manager.dismissBanner()
            manager.startDownload()

            // State MUST still be Downloading at >= 50%, never reset or overridden!
            val stillDownloading = manager.state.value as? UpdateState.Downloading
            assertNotNull("Active download state must never be overridden", stillDownloading)
            assertEquals(50, stillDownloading?.percent)
            assertTrue(stillDownloading!!.statusTitle.contains("Téléchargement de la mise à jour"))

            releaseSecondHalf.countDown()
            awaitCondition { manager.state.value is UpdateState.ReadyToInstall }

            val ready = manager.state.value as UpdateState.ReadyToInstall
            assertTrue(ready.apkFile.exists())
            assertArrayEquals(apkBytes, ready.apkFile.readBytes())
            assertTrue(notifier.startedCalled)
            assertTrue(notifier.progressEvents.isNotEmpty())
            assertTrue(notifier.completedCalled)
            assertNull(notifier.errorMessage)

            // Automatic background check must NOT overwrite an already-downloaded ReadyToInstall APK.
            manager.checkForUpdate(manual = false)
            Thread.sleep(150)
            assertTrue(manager.state.value is UpdateState.ReadyToInstall)

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
    fun effectiveNotesAlwaysGeneratesReadableTextWhenBlankOrBareVersion() {
        val bare = UpdateInfo(
            versionCode = 108,
            versionName = "1.0.8",
            apkUrl = "https://cdn1.afmarbre.com/app-updates/android/latest.apk",
            mandatory = false,
            notes = "1.0.8",
        )
        assertTrue(bare.effectiveNotes.contains("v1.0.8"))
        assertTrue(bare.effectiveNotes.lines().size >= 3)

        val empty = bare.copy(notes = null)
        assertTrue(empty.effectiveNotes.contains("CDN 1"))
    }

    @Test
    fun progressFormatterProducesFrenchReadableOutput() {
        assertEquals("Connexion au serveur de mise à jour…", UpdateFormatter.progressSummary(0, -1))
        val summary = UpdateFormatter.progressSummary(14 * 1_048_576L, 28 * 1_048_576L)
        assertTrue(summary.startsWith("50 % · "))
        assertTrue(summary.contains("Mo"))
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
