package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductUnitDto(
    val id: Long? = null,
    val name: String = "",
    val symbol: String? = null,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "from_products") val fromProducts: Boolean = false,
)

object ProductUnitDefaults {
    val names: List<String> = listOf(
        "unité", "pièce", "kg", "g", "L", "mL", "m", "cm", "m²", "m³",
        "boîte", "carton", "paquet", "lot", "sac", "bouteille", "pack",
        "douzaine", "heure", "jour", "forfait", "service",
    )

    fun merge(serverUnits: List<ProductUnitDto>): List<String> {
        val ordered = serverUnits.mapNotNull { it.name.trim().takeIf(String::isNotEmpty) } + names
        val seen = HashSet<String>()
        return ordered.filter { seen.add(it.lowercase()) }
    }
}
