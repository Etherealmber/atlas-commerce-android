package ma.etherealmber.atlascommerce.core.net

import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/** Prevents OkHttp follow-ups from replaying the single-use desktop phone decision POST. */
internal class DesktopDecisionOneShotInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath != DECISION_PATH) return chain.proceed(request)
        val body = request.body ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .method(request.method, OneShotRequestBody(body))
                .build(),
        )
    }

    private class OneShotRequestBody(private val delegate: RequestBody) : RequestBody() {
        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()
        override fun isOneShot() = true
        override fun isDuplex() = delegate.isDuplex()
        override fun writeTo(sink: BufferedSink) = delegate.writeTo(sink)
    }

    private companion object {
        const val DECISION_PATH = "/api/auth/desktop/v2/phone/decision"
    }
}
