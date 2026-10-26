package ma.etherealmber.atlascommerce.ui.i18n

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ma.etherealmber.atlascommerce.core.AppLanguage

val LocalAtlasLanguage = staticCompositionLocalOf { AppLanguage.FRENCH }
val LocalSetAtlasLanguage = staticCompositionLocalOf<(AppLanguage) -> Unit> { {} }

/** Select the localized copy for a language-aware, high-priority piece of UI. */
@Composable
fun atlasText(french: String, english: String, arabic: String): String =
    when (LocalAtlasLanguage.current) {
        AppLanguage.FRENCH -> french
        AppLanguage.ENGLISH -> english
        AppLanguage.ARABIC -> arabic
    }

@Composable
fun AtlasLanguageSelector(
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val language = LocalAtlasLanguage.current
    val setLanguage = LocalSetAtlasLanguage.current
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = atlasText("Langue", "Language", "اللغة"),
                tint = contentColor,
            )
            Spacer(Modifier.width(4.dp))
            Text(language.tag.uppercase(), color = contentColor)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.nativeLabel) },
                    onClick = {
                        expanded = false
                        setLanguage(option)
                    },
                    leadingIcon = if (option == language) {
                        { Text("✓", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}
