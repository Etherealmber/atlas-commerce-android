package ma.etherealmber.atlascommerce.core.security

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential

/** Thin Android Credential Manager bridge. It returns only the signed WebAuthn response JSON. */
object AndroidPasskeyCredentials {
    suspend fun authenticate(context: Context, requestJson: String): String {
        val result = CredentialManager.create(context).getCredential(
            context = context,
            request = GetCredentialRequest(
                credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson)),
            ),
        )
        val credential = result.credential as? PublicKeyCredential
            ?: error("Credential Manager n'a pas renvoyé une assertion passkey.")
        return credential.authenticationResponseJson
    }

    suspend fun create(context: Context, requestJson: String): String {
        val result = CredentialManager.create(context).createCredential(
            context = context,
            request = CreatePublicKeyCredentialRequest(
                requestJson = requestJson,
                preferImmediatelyAvailableCredentials = false,
            ),
        )
        val credential = result as? CreatePublicKeyCredentialResponse
            ?: error("Credential Manager n'a pas renvoyé une attestation passkey.")
        return credential.registrationResponseJson
    }
}
