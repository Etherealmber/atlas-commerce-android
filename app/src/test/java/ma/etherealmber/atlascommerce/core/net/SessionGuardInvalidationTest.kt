package ma.etherealmber.atlascommerce.core.net

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionGuardInvalidationTest {
    @Test
    fun passwordChangeDeactivationAndDeviceRevocationStillInvalidateTheSession() = runBlocking {
        listOf("PASSWORD_CHANGED", "USER_DEACTIVATED", "DEVICE_REVOKED").forEach { code ->
            val server = MockWebServer()
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"success":false,"code":"$code","error":"Session invalide"}"""),
            )
            server.start()
            try {
                val events = SessionEvents()
                val expired = async(start = CoroutineStart.UNDISPATCHED) { events.expired.first() }
                val response = OkHttpClient.Builder()
                    .addInterceptor(SessionGuardInterceptor(events))
                    .build()
                    .newCall(
                        Request.Builder()
                            .url(server.url("/api/products"))
                            .header("Authorization", "Bearer test-session")
                            .build(),
                    )
                    .execute()
                response.close()

                assertEquals("Bearer test-session", expired.await().authorizationHeader)
            } finally {
                server.shutdown()
            }
        }
    }
}
