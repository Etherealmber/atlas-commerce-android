package ma.etherealmber.atlascommerce.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.core.security.AtRestCipher
import ma.etherealmber.atlascommerce.core.security.CryptoStore
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Isolated offline product cache strictly partitioned by `(serverUrl, username)`.
 *
 * Every `(serverUrl, username)` pair maps to `products_<srvHash>_<usrHash>.json`, and
 * unauthenticated calls read/write nothing — guaranteeing zero cross-user or
 * cross-server data leakage across cold restarts, shared devices, and server switches.
 */
@Singleton
class OfflineCache internal constructor(
    private val directory: File,
    private val moshi: Moshi,
    private val cryptoStore: AtRestCipher,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        moshi: Moshi,
        cryptoStore: CryptoStore,
    ) : this(File(context.filesDir, "atlas-cache"), moshi, cryptoStore)

    init {
        directory.mkdirs()
        // Purge the old un-scoped global file and migrate account-scoped plaintext catalogs.
        File(directory, LEGACY_PRODUCTS_FILE).delete()
        migrateLegacyCatalogFiles()
    }

    @Volatile
    private var activeScopeKey: String? = null

    private val productAdapter by lazy {
        moshi.adapter<List<ProductDto>>(
            Types.newParameterizedType(List::class.java, ProductDto::class.java),
        )
    }

    /** Updates active `(serverUrl, username)` partition; passes `null` on sign-out. */
    fun bindScope(serverUrl: String?, username: String?) {
        val srv = serverUrl?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val usr = username?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        activeScopeKey = if (srv != null && usr != null) {
            scopeKey(srv, usr)
        } else {
            null
        }
    }

    fun unbindScope() {
        activeScopeKey = null
    }

    suspend fun saveProducts(products: List<ProductDto>) = withContext(Dispatchers.IO) {
        val key = activeScopeKey ?: return@withContext
        write("products_$key.json") { cryptoStore.encryptAtRest(productAdapter.toJson(products)) }
    }

    suspend fun readProducts(): List<ProductDto> = withContext(Dispatchers.IO) {
        val key = activeScopeKey ?: return@withContext emptyList()
        val raw = read("products_$key.json") ?: return@withContext emptyList()
        val clearText = cryptoStore.decryptAtRest(raw) ?: return@withContext emptyList()
        runCatching { productAdapter.fromJson(clearText) }.getOrNull().orEmpty()
    }

    /** Clears only the active `(serverUrl, username)` cache file. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        val key = activeScopeKey
        if (key != null) {
            runCatching { File(directory, "products_$key.json").delete() }
        }
        runCatching { File(directory, LEGACY_PRODUCTS_FILE).delete() }
    }

    /** Clears a specific `(serverUrl, username)` cache partition (e.g. when forgetting a saved account). */
    suspend fun clearAccountScope(serverUrl: String, username: String) = withContext(Dispatchers.IO) {
        val srv = serverUrl.trim().lowercase()
        val usr = username.trim().lowercase()
        if (srv.isNotEmpty() && usr.isNotEmpty()) {
            runCatching { File(directory, "products_${scopeKey(srv, usr)}.json").delete() }
        }
    }

    /** Clears every cached account partition belonging to [serverUrl]. */
    suspend fun clearServerScope(serverUrl: String) = withContext(Dispatchers.IO) {
        val srv = serverUrl.trim().lowercase()
        if (srv.isEmpty()) return@withContext
        val prefix = "products_${shortSha(srv)}_"
        runCatching {
            directory.listFiles()?.forEach { f ->
                if (f.name.startsWith(prefix)) f.delete()
            }
        }
    }

    /** Clears every cached file across all partitions. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        runCatching { directory.listFiles()?.forEach { it.delete() } }
    }

    private fun scopeKey(serverUrl: String, username: String): String =
        "${shortSha(serverUrl)}_${shortSha("$serverUrl::$username")}"

    private fun shortSha(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun migrateLegacyCatalogFiles() {
        directory.listFiles()
            ?.filter { it.isFile && it.name.startsWith("products_") && it.extension.equals("json", true) }
            ?.forEach { file ->
                runCatching {
                    val stored = file.readText()
                    if (!stored.startsWith(AT_REST_PREFIX)) {
                        file.writeText(cryptoStore.encryptAtRest(stored))
                    }
                }.onFailure {
                    // Fail closed: never leave a plaintext product catalog behind if encryption fails.
                    runCatching { file.delete() }
                }
            }
    }

    private inline fun write(name: String, block: () -> String) {
        runCatching { File(directory, name).writeText(block()) }
    }

    private fun read(name: String): String? = runCatching {
        File(directory, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    private companion object {
        const val AT_REST_PREFIX = "v2:"
        const val LEGACY_PRODUCTS_FILE = "products.json"
    }
}
