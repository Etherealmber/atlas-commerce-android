package ma.etherealmber.atlascommerce.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartupRoleRefreshRequest(val fingerprint: String)

@JsonClass(generateAdapter = true)
data class StartupRoleRefreshDto(
    val token: String = "",
    val role: String = "",
)
