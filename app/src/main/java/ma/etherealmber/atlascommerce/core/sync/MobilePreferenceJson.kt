package ma.etherealmber.atlascommerce.core.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/** Small JSON object adapter shared by backup validation and error-body parsing. */
internal object MobilePreferenceJson {
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val adapter = Moshi.Builder().build().adapter<Map<String, Any?>>(mapType)

    fun decodeObject(json: String): Map<String, Any?>? = runCatching { adapter.fromJson(json) }.getOrNull()
    fun encodeObject(value: Map<String, Any?>): String = adapter.toJson(value)
}
