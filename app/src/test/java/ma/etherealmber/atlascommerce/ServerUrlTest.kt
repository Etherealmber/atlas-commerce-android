package ma.etherealmber.atlascommerce

import ma.etherealmber.atlascommerce.core.net.ServerUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {

    @Test
    fun `bare host becomes https`() {
        assertEquals("https://stock.afmarbre.com", ServerUrl.normalize("stock.afmarbre.com"))
    }

    @Test
    fun `trailing slashes are removed`() {
        assertEquals("https://example.com", ServerUrl.normalize("https://example.com///"))
    }

    @Test
    fun `api suffix is stripped`() {
        assertEquals("https://example.com", ServerUrl.normalize("https://example.com/api"))
        assertEquals("https://example.com", ServerUrl.normalize("https://example.com/api/health"))
    }

    @Test
    fun `local addresses default to http`() {
        assertEquals("http://10.0.2.2:8787", ServerUrl.normalize("10.0.2.2:8787"))
        assertEquals("http://127.0.0.1:8787", ServerUrl.normalize("127.0.0.1:8787"))
    }

    @Test
    fun `explicit scheme is preserved`() {
        assertEquals("http://192.168.1.20:8787", ServerUrl.normalize("http://192.168.1.20:8787"))
    }

    @Test
    fun `blank and invalid values are rejected`() {
        assertNull(ServerUrl.normalize(null))
        assertNull(ServerUrl.normalize("   "))
        assertNull(ServerUrl.normalize("ftp://example.com"))
        assertNull(ServerUrl.normalize("https://exa mple.com"))
        assertNull(ServerUrl.normalize("https://example.com/path?token=1"))
    }

    @Test
    fun `bare lan hostname is accepted`() {
        assertEquals("https://atlas.local:8787", ServerUrl.normalize("atlas.local:8787"))
        assertEquals("https://boutique", ServerUrl.normalize("boutique"))
    }

    @Test
    fun `origin removes paths and default ports but keeps non-default ports`() {
        assertEquals("https://example.com", ServerUrl.origin("https://example.com:443/api"))
        assertEquals("http://localhost:8787", ServerUrl.origin("http://localhost:8787/api"))
        assertNull(ServerUrl.origin("not a valid server"))
    }

    @Test
    fun `display hides the scheme`() {
        assertEquals("stock.afmarbre.com", ServerUrl.display("https://stock.afmarbre.com"))
        assertEquals("10.0.2.2:8787", ServerUrl.display("http://10.0.2.2:8787/"))
    }

    @Test
    fun `local detection only matches private hosts`() {
        assertTrue(ServerUrl.isLocal("http://10.0.2.2:8787"))
        assertTrue(ServerUrl.isLocal("http://localhost:8787"))
        assertTrue(!ServerUrl.isLocal("https://stock.afmarbre.com"))
    }
}
