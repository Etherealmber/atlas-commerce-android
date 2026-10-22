package ma.etherealmber.atlascommerce.ui.screens.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.AppDetails
import ma.etherealmber.atlascommerce.ui.components.AtlasAvatar
import ma.etherealmber.atlascommerce.ui.components.AtlasBullet
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasMark
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.theme.Atlas
import ma.etherealmber.atlascommerce.ui.util.openExternal

/**
 * Reusable GitHub maintainer identity card displaying @Etherealmber's rounded
 * circular profile picture (`CircleShape`), MIT copyright ownership, and direct
 * one-tap link to https://github.com/Etherealmber.
 */
@Composable
fun EtherealmberAuthorCard(
    onToast: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val openProfile = {
        if (!openExternal(context, AppDetails.GITHUB_PROFILE_URL)) {
            onToast?.invoke("Aucun navigateur disponible.")
        }
    }

    AtlasCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = openProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AtlasAvatar(
                avatarUrl = AppDetails.GITHUB_AVATAR_URL,
                label = AppDetails.GITHUB_HANDLE,
                size = 48.dp,
                onClick = openProfile,
                modifier = Modifier.clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = AppDetails.COPYRIGHT_HOLDER,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AtlasChip(text = "MIT", tone = AtlasTone.POSITIVE)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "github.com/${AppDetails.GITHUB_HANDLE} · © ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = "Ouvrir le profil GitHub Etherealmber",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LegalPage(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AtlasTopBar(title = title, subtitle = subtitle, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Atlas.density.screenPadding, vertical = Atlas.density.itemGap),
            verticalArrangement = Arrangement.spacedBy(Atlas.density.itemGap),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// License
// ---------------------------------------------------------------------------

private const val MIT_TEXT = """MIT License

Copyright (c) 2026 Etherealmber

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

@Composable
fun LicenseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LegalPage(
        title = "Licence",
        subtitle = "MIT · open source",
        onBack = onBack,
    ) {
        EtherealmberAuthorCard()

        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Licence MIT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                AtlasChip(text = "Open source", tone = AtlasTone.POSITIVE)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}. L'application Android " +
                    "Atlas Commerce est distribuée sous licence MIT, comme le projet web dont elle dépend.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            AtlasButton(
                text = "Profil GitHub @${AppDetails.GITHUB_HANDLE}",
                onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            AtlasButton(
                text = "Voir le dépôt web",
                onClick = { openExternal(context, AppDetails.WEB_REPO) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AtlasCard {
            Text(
                text = "Texte de la licence",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = MIT_TEXT,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AtlasCard {
            Text(
                text = "Composants tiers",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Chaque composant reste la propriété de ses auteurs et est utilisé selon sa licence.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            ThirdParty("Kotlin, Kotlin Coroutines", "Apache License 2.0")
            ThirdParty("Jetpack Compose, AndroidX, DataStore", "Apache License 2.0")
            ThirdParty("Hilt / Dagger", "Apache License 2.0")
            ThirdParty("OkHttp, Retrofit, Moshi", "Apache License 2.0")
            ThirdParty("CameraX", "Apache License 2.0")
            ThirdParty("Google ML Kit — Barcode Scanning", "Google APIs Terms of Service (modèle embarqué)")
            ThirdParty("Atlas Commerce worker & Android", "MIT — © ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}")
        }

        AtlasCard {
            Text(
                text = "Marques",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "« Atlas Commerce » et le logo Atlas appartiennent à ${AppDetails.COPYRIGHT_HOLDER}. " +
                    "Android et Google Play sont des marques de Google LLC. " +
                    "Cloudflare est une marque de Cloudflare, Inc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ThirdParty(name: String, license: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = license,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))
}

// ---------------------------------------------------------------------------
// Security & privacy
// ---------------------------------------------------------------------------

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LegalPage(
        title = "Sécurité & confidentialité",
        subtitle = "Ce que l'app fait de vos données",
        onBack = onBack,
    ) {
        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AtlasMark(size = 36.dp)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Aucun traqueur publicitaire", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Pas d'analytics, pas de publicité. Seul ML Kit (scan) transmet ses " +
                            "statistiques d'usage à Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AtlasBullet("L'application ne contacte que le serveur Atlas que vous avez choisi, à une exception : ML Kit.")
            AtlasBullet("Aucune image de caméra n'est envoyée : les codes-barres sont décodés sur l'appareil.")
            AtlasBullet(
                "ML Kit (Google, lecture des codes-barres) transmet ses métriques d'usage et de " +
                    "performance à Google : version de l'app, identifiant d'installation, appareil. " +
                    "Aucune image, aucun code scanné.",
            )
            AtlasBullet("Aucun contact, aucune localisation, aucun accès au stockage n'est demandé.")
        }

        AtlasCard {
            SectionTitle("Authentification", "Mot de passe + code e-mail")
            AtlasBullet("Le mot de passe part en HTTPS et n'est jamais stocké sur l'appareil.")
            AtlasBullet("Un nouvel appareil déclenche un code OTP à 6 chiffres, valable 10 minutes.")
            AtlasBullet("La session est liée à l'adresse IP et à l'empreinte de l'appareil.")
            AtlasBullet("Le jeton est chiffré en AES-256-GCM avec une clé du Keystore Android.")
            AtlasBullet("La déconnexion efface le jeton, le panier et le cache du catalogue.")
        }

        AtlasCard {
            SectionTitle("Transport réseau", "HTTPS obligatoire")
            AtlasBullet("Les versions de production refusent le trafic HTTP en clair.")
            AtlasBullet("Le certificat TLS est vérifié par la pile réseau d'Android.")
            AtlasBullet("Seules les versions debug autorisent un serveur local (émulateur).")
        }

        AtlasCard {
            SectionTitle("Données conservées", "Minimum vital")
            AtlasBullet("Jeton de session chiffré + identifiant, rôle et nom affiché.")
            AtlasBullet("Catalogue produits en cache pour le mode hors ligne (aucune donnée financière).")
            AtlasBullet("Préférences d'apparence et adresse du serveur.")
            AtlasBullet("Effacement complet : déconnexion, puis désinstallation de l'app.")
        }

        AtlasCard {
            SectionTitle("Vos responsabilités", "Auto-hébergement")
            Text(
                text = "Atlas Commerce fonctionne avec votre propre serveur. L'administrateur du serveur " +
                    "voit les ventes, le stock et les comptes. Choisissez un serveur de confiance et un " +
                    "mot de passe fort, et activez la double authentification côté web.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AtlasCard {
            SectionTitle("Signaler une vulnérabilité", "Réponse sous 48 h")
            Text(
                text = "Merci de ne pas ouvrir d'issue publique pour une faille. Écrivez à " +
                    "${AppDetails.SECURITY_EMAIL} avec les étapes de reproduction.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            AtlasButton(
                text = "Contacter la sécurité",
                onClick = { openExternal(context, "mailto:${AppDetails.SECURITY_EMAIL}") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

// ---------------------------------------------------------------------------
// About
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LegalPage(
        title = "À propos",
        subtitle = "${AppDetails.NAME} ${AppDetails.VERSION_NAME}",
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AtlasMark(size = 72.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = AppDetails.NAME,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = AppDetails.TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AtlasChip(text = "v${AppDetails.VERSION_NAME}", tone = AtlasTone.INFO)
                AtlasChip(text = "build ${AppDetails.VERSION_CODE}")
                AtlasChip(text = "MIT", tone = AtlasTone.POSITIVE)
            }
        }

        EtherealmberAuthorCard()

        AtlasCard {
            Text(
                text = AppDetails.DESCRIPTION,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cette application est le compagnon Android du projet web Atlas Commerce : " +
                    "elle se connecte à votre serveur et ne stocke aucune comptabilité localement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AtlasCard {
            Text("Fonctionnalités", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            AtlasBullet("Tableau de bord : trésorerie, valeur du stock, créances, tendance.")
            AtlasBullet("Catalogue produits avec recherche, stock faible et mode hors ligne.")
            AtlasBullet("Scan de codes-barres pour ajouter un article au panier.")
            AtlasBullet("Vente rapide : ticket VR sans TVA, prêt à encaisser sur le web.")
            AtlasBullet("Mises à jour accélérées multi-connexions (4 flux HTTP Range) et journal intégré.")
        }

        AtlasCard {
            Text("Liens", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            AtlasButton(
                text = "GitHub @${AppDetails.GITHUB_HANDLE}",
                onClick = { openExternal(context, AppDetails.GITHUB_PROFILE_URL) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            AtlasButton(
                text = "Projet Atlas Commerce (web)",
                onClick = { openExternal(context, AppDetails.WEB_REPO) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            AtlasButton(
                text = "Code source Android",
                onClick = { openExternal(context, AppDetails.PROJECT_REPO) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AtlasCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Atlas.palette.positive,
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = "© ${AppDetails.COPYRIGHT_YEAR} ${AppDetails.COPYRIGHT_HOLDER}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Distribué sous licence MIT. Forkez, modifiez, citez-nous.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
