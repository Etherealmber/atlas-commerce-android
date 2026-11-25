package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val sunDefinition = LucideIconDefinition("Sun") { strokeWidth ->
    lucidePath(strokeWidth) {
        moveTo(12f, 8f)
        arcToRelative(4f, 4f, 0f, true, true, 0f, 8f)
        arcToRelative(4f, 4f, 0f, true, true, 0f, -8f)
        moveTo(12f, 2f); verticalLineTo(4f)
        moveTo(12f, 20f); verticalLineTo(22f)
        moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f)
        moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f)
        moveTo(2f, 12f); horizontalLineTo(4f)
        moveTo(20f, 12f); horizontalLineTo(22f)
        moveTo(4.93f, 19.07f); lineTo(6.34f, 17.66f)
        moveTo(17.66f, 6.34f); lineTo(19.07f, 4.93f)
    }
}

val LucideIcons.All.SunDefinition: LucideIconDefinition get() = sunDefinition
