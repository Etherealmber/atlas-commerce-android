package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.core.AppDetails
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class AtlasVersionHeadersTest {
    @Test
    fun regularApiRequestsCanCarryTheAppVersionCompatibilityHeaders() {
        val request = Request.Builder()
            .url("https://atlas.example/api/health")
            .addAtlasAppVersionHeaders()
            .build()

        assertEquals(AppDetails.VERSION_CODE.toString(), request.header("X-Atlas-App-Version-Code"))
        assertEquals(AppDetails.VERSION_NAME, request.header("X-Atlas-App-Version-Name"))
    }
}
