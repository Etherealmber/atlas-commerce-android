package ma.etherealmber.atlascommerce.core.net

import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleRefreshInterceptorTest {
    @Test
    fun roleChangedRefreshesTokenAndRoleThenRetriesRejectedRequestOnce() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"ROLE_CHANGED"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true,"data":{"token":"new-token","role":"manager"}}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()
        try {
            val tokenHolder = TokenHolder().apply { set("old-token") }
            val refreshedRole = async(start = CoroutineStart.UNDISPATCHED) { tokenHolder.roleRefreshes.first { it != null }!! }
            val client = OkHttpClient.Builder()
                .addInterceptor(RoleRefreshInterceptor(tokenHolder, { "device-fingerprint" }, SessionEvents(), Moshi.Builder().build()))
                .build()
            val request = Request.Builder()
                .url(server.url("api/products"))
                .header("Authorization", "Bearer old-token")
                .get()
                .build()

            client.newCall(request).execute().use { response -> assertEquals(200, response.code) }

            val rejected = server.takeRequest()
            val refresh = server.takeRequest()
            val retried = server.takeRequest()
            assertEquals("/api/products", rejected.path)
            assertEquals("/api/auth/role-refresh", refresh.path)
            assertEquals("Bearer old-token", refresh.getHeader("Authorization"))
            assertEquals("device-fingerprint", refresh.getHeader("X-Device-Fingerprint"))
            assertTrue(refresh.body.readUtf8().contains("\"fingerprint\":\"device-fingerprint\""))
            assertEquals("/api/products", retried.path)
            assertEquals("Bearer new-token", retried.getHeader("Authorization"))
            assertEquals("new-token", tokenHolder.token)
            assertEquals("manager", refreshedRole.await().role)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun roleReductionAlsoUpdatesTokenAndLeavesSessionActive() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"ROLE_CHANGED"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success":true,"data":{"token":"reduced-token","role":"cashier"}}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()
        try {
            val tokenHolder = TokenHolder().apply { set("old-token") }
            val refreshedRole = async(start = CoroutineStart.UNDISPATCHED) { tokenHolder.roleRefreshes.first { it != null }!! }
            val client = OkHttpClient.Builder()
                .addInterceptor(RoleRefreshInterceptor(tokenHolder, { "device-fingerprint" }, SessionEvents(), Moshi.Builder().build()))
                .build()
            val request = Request.Builder().url(server.url("api/products")).header("Authorization", "Bearer old-token").get().build()
            client.newCall(request).execute().use { response -> assertEquals(200, response.code) }
            assertEquals("cashier", refreshedRole.await().role)
            assertEquals("reduced-token", tokenHolder.token)
            assertEquals(3, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun offlineRefreshFailureReturnsOriginal409WithoutLogoutOrReplay() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"ROLE_CHANGED"}"""))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.start()
        try {
            val tokenHolder = TokenHolder().apply { set("old-token") }
            val events = SessionEvents()
            val expiredEvent = async(start = CoroutineStart.UNDISPATCHED) { events.expired.first() }
            val client = OkHttpClient.Builder()
                .addInterceptor(RoleRefreshInterceptor(tokenHolder, { "device-fingerprint" }, events, Moshi.Builder().build()))
                .build()
            val request = Request.Builder().url(server.url("api/products")).header("Authorization", "Bearer old-token").get().build()

            client.newCall(request).execute().use { response -> assertEquals(409, response.code) }
            assertEquals("old-token", tokenHolder.token)
            assertFalse(expiredEvent.isCompleted)
            expiredEvent.cancel()
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun staleRequestFromPreviousAccountIsNeverRetriedWithCurrentAccountToken() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"ROLE_CHANGED"}"""))
        server.start()
        try {
            val tokenHolder = TokenHolder().apply { set("new-account-token") }
            val client = OkHttpClient.Builder()
                .addInterceptor(RoleRefreshInterceptor(tokenHolder, { "device-fingerprint" }, SessionEvents(), Moshi.Builder().build()))
                .build()
            val request = Request.Builder().url(server.url("api/products")).header("Authorization", "Bearer old-account-token").get().build()
            client.newCall(request).execute().use { response -> assertEquals(409, response.code) }
            assertEquals(1, server.requestCount)
            assertEquals("new-account-token", tokenHolder.token)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun refresh401SignalsInvalidSessionButDoesNotTreatTransientFailuresAsLogout() = runBlocking {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"ROLE_CHANGED"}"""))
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"SESSION_REVOKED"}"""))
        server.start()
        try {
            val tokenHolder = TokenHolder().apply { set("old-token") }
            val events = SessionEvents()
            val expired = async(start = CoroutineStart.UNDISPATCHED) { events.expired.first() }
            val client = OkHttpClient.Builder()
                .addInterceptor(RoleRefreshInterceptor(tokenHolder, { "device-fingerprint" }, events, Moshi.Builder().build()))
                .build()
            val request = Request.Builder().url(server.url("api/products")).header("Authorization", "Bearer old-token").get().build()

            client.newCall(request).execute().use { response -> assertEquals(409, response.code) }
            assertEquals("old-token", tokenHolder.token)
            assertEquals("Bearer old-token", expired.await().authorizationHeader)
        } finally {
            server.shutdown()
        }
    }
}
