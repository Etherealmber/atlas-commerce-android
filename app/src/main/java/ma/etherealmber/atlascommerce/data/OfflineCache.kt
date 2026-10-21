package ma.etherealmber.atlascommerce.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import ma.etherealmber.atlascommerce.data.model.ProductDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tiny disk cache for the product list.
 *
 * Shops scan and sell in basements and warehouses with no signal; the last
 * catalogue is still browsable and searchable. Only non-sensitive catalogue
 * fields are cached — never the session, prices are public to the shop staff.
 */
@Singleton
class OfflineCache @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi,
) {

    private val directory = File(context.filesDir, "atlas-cache").apply { mkdirs() }

    private val productAdapter = moshi.adapter<List<ProductDto>>(
        Types.newParameterizedType(List::class.java, ProductDto::class.java),
    )

    /** Disk I/O, so callers never block the main thread with a 300-product JSON. */
    suspend fun saveProducts(products: List<ProductDto>) = withContext(Dispatchers.IO) {
        write(PRODUCTS_FILE) { productAdapter.toJson(products) }
    }

    suspend fun readProducts(): List<ProductDto> = withContext(Dispatchers.IO) {
        val raw = read(PRODUCTS_FILE) ?: return@withContext emptyList()
        runCatching { productAdapter.fromJson(raw) }.getOrNull().orEmpty()
    }

    /**
     * Drops the cached catalogue. Called on sign-out and on server change: the
     * cache is shared by all accounts, so keeping it would leak the previous
     * shop's prices into an offline session.
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { directory.listFiles()?.forEach { it.delete() } }
    }

    private inline fun write(name: String, block: () -> String) {
        runCatching { File(directory, name).writeText(block()) }
    }

    private fun read(name: String): String? = runCatching {
        File(directory, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    private companion object {
        const val PRODUCTS_FILE = "products.json"
    }
}
