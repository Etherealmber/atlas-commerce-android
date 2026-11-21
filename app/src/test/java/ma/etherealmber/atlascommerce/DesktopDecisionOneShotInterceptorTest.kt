package ma.etherealmber.atlascommerce

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import ma.etherealmber.atlascommerce.core.net.ApiEnvelope
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.core.net.DesktopDecisionOneShotInterceptor
import ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionDto
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class DesktopDecisionOneShotInterceptorTest {
    @Test
    fun decisionSuccessWorksAndRetryAfterZeroDoesNotReplayPost() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"data":{"status":"approved"}}"""),
        )
        server.enqueue(
            MockResponse().setResponseCode(503)
                .addHeader("Retry-After", "0")
                .setBody("""{"success":false,"code":"UNAVAILABLE","error":"unavailable"}"""),
        )
        // If OkHttp replays the POST, this response would turn the failure into success.
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"success":true,"data":{"status":"approved"}}"""),
        )
        server.start()
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .addInterceptor(DesktopDecisionOneShotInterceptor())
                .build()
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DecisionTestApi::class.java)
            val runner = ApiRunner(moshi)
            val requestBody = """{"decision":"approve","compare_confirmed":true}"""
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val success = runner.envelope { api.decision(requestBody) }
            assertTrue(success is ApiResult.Success<*>)
            assertEquals("approved", (success as ApiResult.Success<DesktopPhoneDecisionDto>).data.status)

            val failure = runner.envelope { api.decision(requestBody) }
            assertTrue(failure is ApiResult.Failure)
            assertEquals(503, (failure as ApiResult.Failure).httpStatus)
            assertEquals("The decision endpoint should receive one POST per attempt", 2, server.requestCount)
            assertEquals("POST", server.takeRequest().method)
            assertEquals("POST", server.takeRequest().method)
        } finally {
            server.shutdown()
        }
    }

    private interface DecisionTestApi {
        @POST("api/auth/desktop/v2/phone/decision")
        suspend fun decision(@Body body: RequestBody): ApiEnvelope<DesktopPhoneDecisionDto>
    }
}
