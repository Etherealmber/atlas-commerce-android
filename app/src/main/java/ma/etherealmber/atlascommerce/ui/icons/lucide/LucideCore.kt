package ma.etherealmber.atlascommerce.ui.icons.lucide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path

/** Minimal local runtime for the generated Lucide vectors used by the Android app. */
object LucideIcons {
    object All
}

data class LucideIconDefinition(
    val name: String,
    val paths: ImageVector.Builder.(strokeWidth: Float) -> Unit,
)

fun ImageVector.Builder.lucidePath(
    strokeWidth: Float,
    pathBuilder: PathBuilder.() -> Unit,
) = path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = strokeWidth,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathBuilder,
)
