package ma.etherealmber.atlascommerce.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import kotlin.random.Random

internal fun nextSurprise(previous: Int, count: Int, random: Random = Random.Default): Int {
    require(count > 0)
    if (count == 1) return 0
    if (previous !in 0 until count) return random.nextInt(count)
    val next = random.nextInt(count - 1)
    return if (next >= previous) next + 1 else next
}

@Composable
internal fun AtlasSurpriseDialog(index: Int, onDismiss: () -> Unit) {
    val messages = listOf(
        atlasText("✨ Merci de faire partie de l’aventure Atlas !", "✨ Thanks for being part of the Atlas journey!", "✨ شكراً لكونك جزءاً من رحلة Atlas!"),
        atlasText("✨ L’assistant Atlas s’est appelé Paul autrefois.", "✨ Atlas Agent was once named Paul.", "✨ كان اسم مساعد Atlas في يوم من الأيام بول."),
        atlasText("✨ Plot twist : l’organisation Etherealmber a commencé avec un seul membre.", "✨ Plot twist: the Etherealmber organization started with one member.", "✨ مفاجأة: بدأت منظمة Etherealmber بعضو واحد."),
        atlasText("✨ Etherealmber était un compte de montage vidéo AMV sur Instagram et TikTok.", "✨ Etherealmber was an AMV video-editing account on Instagram and TikTok.", "✨ كان Etherealmber حساباً لتحرير فيديوهات AMV على Instagram وTikTok."),
        atlasText("✨ Une étoile Atlas pour illuminer votre journée !", "✨ An Atlas star to brighten your day!", "✨ نجمة Atlas لتنير يومك!"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(atlasText("Une petite surprise !", "A little surprise!", "مفاجأة صغيرة!")) },
        text = { Text(messages[index.coerceIn(messages.indices)]) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(atlasText("Fermer", "Close", "إغلاق")) } },
    )
}
