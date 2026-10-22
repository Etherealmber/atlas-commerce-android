package ma.etherealmber.atlascommerce.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.data.model.ProductDto
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolated offline product cache strictly partitioned by `(serverUrl, username)`.
 *
 * Every `(serverUrl, username)` pair maps to its own SHA-256 hashed filename, and
 * unauthenticated calls read/write nothing — guaranteeing zero cross-user or
 * cross-server data leakage even across cold restarts or shared devices.
 */
@Singleton
class OfflineCache @Inject constructor(
    @ApplicationContext context: Context,
    private val moshi: Moshi,
) {

    private val directory = File(context.filesDir, "atlas-cache").apply {
        mkdirs()
        // Purge any legacy un-scoped global `products.json` immediately on startup
        File(this, LEGACY_PRODUCTS_FILE).delete()
    }

    @Volatile
    private var activeScopeHash: String? = null

    private val productAdapter by lazy {
        moshi.adapter<List<ProductDto>>(
            Types.newParameterizedType(List::class.java, ProductDto::class.java),
        )
    }

    /** Updates active `(serverUrl, username)` partition; passes `null` on sign-out. */
    fun bindScope(serverUrl: String?, username: String?) {
        val srv = serverUrl?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val usr = username?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        activeScopeHash = if (srv != null && usr != null) {
            scopeHash(srv, usr)
        } else {
            null
        }
    }

    suspend fun saveProducts(products: List<ProductDto>) = withContext(Dispatchers.IO) {
        val hash = activeScopeHash ?: return@withContext
        write("products_$hash.json") { productAdapter.toJson(products) }
    }

    suspend fun readProducts(): List<ProductDto> = withContext(Dispatchers.IO) {
        val hash = activeScopeHash ?: return@withContext emptyList()
        val raw = read("products_$hash.json") ?: return@withContext emptyList()
        runCatching { productAdapter.fromJson(raw) }.getOrNull().orEmpty()
    }

    /** Clears only the active `(serverUrl, username)` cache file. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        val hash = activeScopeHash
        if (hash != null) {
            runCatching { File(directory, "products_$hash.json").delete() }
        }
        runCatching { File(directory, LEGACY_PRODUCTS_FILE).delete() }
    }

    /** Clears a specific `(serverUrl, username)` cache partition (e.g. when forgetting a saved account). */
    suspend fun clearAccountScope(serverUrl: String, username: String) = withContext(Dispatchers.IO) {
        val srv = serverUrl.trim().lowercase()
        val usr = username.trim().lowercase()
        if (srv.isNotEmpty() && usr.isNotEmpty()) {
            runCatching { File(directory, "products_${scopeHash(srv, usr)}.json").delete() }
        }
    }

    /** Clears every cached file across all partitions. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching { directory.listFiles()?.forEach { it.delete() } }
    }

    private fun scopeHash(serverUrl: String, username: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest("$serverUrl::$username".toByteArray(StandardCharsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private inline fun write(name: String, block: () -> String) {
        runCatching { File(directory, name).writeText(block()) }
    }

    private fun read(name: String): String? = runCatching {
        File(directory, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    private companion object {
        const val LEGACY_PRODUCTS_FILE = "products.json"
    }
}
