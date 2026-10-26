package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import org.json.JSONObject

/** Credential Manager bridge using only the selected Worker's origin; returns signed WebAuthn JSON. */
object AndroidPasskeyCredentials {
    /** Summarizes WebAuthn request shape for troubleshooting without logging secrets or user data. */
    fun registrationOptionsSummary(requestJson: String): String = runCatching {
        val root = JSONObject(requestJson)
        val rpId = root.optJSONObject("rp")
            ?.optString("id")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.take(120) ?: "missing"
        val userId = root.optJSONObject("user")?.optString("id")
        val params = root.optJSONArray("pubKeyCredParams")
        val algorithms = if (params == null) {
            "missing"
        } else {
            (0 until params.length()).mapNotNull { index ->
                params.optJSONObject(index)?.let { param ->
                    "${param.optString("type", "?")}:${param.optInt("alg", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE } ?: "?"}"
                }
            }.joinToString(",").ifBlank { "none" }
        }
        val selection = root.optJSONObject("authenticatorSelection")
        val excludedCount = root.optJSONArray("excludeCredentials")?.length() ?: 0
        "requestShape: rpId=$rpId; challengePresent=${root.optString("challenge").isNotBlank()}; " +
            "userIdPresent=${!userId.isNullOrBlank() && userId != "null"}; algorithms=$algorithms; " +
            "authenticatorAttachment=${selection?.optString("authenticatorAttachment")?.takeIf { it.isNotBlank() } ?: "unspecified"}; " +
            "residentKey=${selection?.optString("residentKey")?.takeIf { it.isNotBlank() } ?: "unspecified"}; " +
            "userVerification=${selection?.optString("userVerification")?.takeIf { it.isNotBlank() } ?: "unspecified"}; " +
            "excludeCredentials=$excludedCount"
    }.getOrElse { "requestShape=parseFailed:${it.javaClass.simpleName}" }

    suspend fun authenticate(context: Context, requestJson: String, origin: String): String {
        val result = CredentialManager.create(context).getCredential(
            context = context,
            request = GetCredentialRequest(
                credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson)),
                origin = origin,
            ),
        )
        val credential = result.credential as? PublicKeyCredential
            ?: error("Credential Manager n'a pas renvoyé une assertion passkey.")
        return credential.authenticationResponseJson
    }

    suspend fun create(context: Context, requestJson: String, origin: String): String {
        val result = CredentialManager.create(context).createCredential(
            context = context,
            request = CreatePublicKeyCredentialRequest(
                requestJson = requestJson,
                preferImmediatelyAvailableCredentials = false,
                origin = origin,
            ),
        )
        val credential = result as? CreatePublicKeyCredentialResponse
            ?: error("Credential Manager n'a pas renvoyé une attestation passkey.")
        return credential.registrationResponseJson
    }
}
