package ma.etherealmber.atlascommerce.push

import com.squareup.moshi.JsonClass

/** Body of POST /api/assist/device-token (worker upserts into device_push_tokens). */
@JsonClass(generateAdapter = true)
data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android",
)

/** Worker ack: `{ success: true, data: { registered: true } }`. */
@JsonClass(generateAdapter = true)
data class DeviceTokenAckDto(
    val registered: Boolean = false,
)
