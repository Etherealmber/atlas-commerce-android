package ma.etherealmber.atlascommerce.ui.root

/** Generic, safe messages based only on the verification response status. */
internal fun passkeyRegistrationFailureMessage(status: Int?): String = when (status) {
    401 -> "Votre session a expiré. Reconnectez-vous puis réessayez."
    403 -> "Le serveur refuse cette opération. Vérifiez vos autorisations."
    409 -> "Le serveur a signalé un conflit. Réessayez ou contactez l'administrateur."
    400, 422 -> "Le serveur n'a pas pu vérifier la passkey. Réessayez ou contactez l'administrateur."
    else -> "Le serveur n'a pas pu enregistrer la passkey. Réessayez plus tard."
}
