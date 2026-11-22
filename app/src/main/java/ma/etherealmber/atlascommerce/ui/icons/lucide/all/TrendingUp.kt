package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("TrendingUp") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(16f, 7f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(6f)
            }
            lucidePath(strokeWidth) {
                moveToRelative(22f, 7f)
                lineToRelative(-8.5f, 8.5f)
                lineToRelative(-5f, -5f)
                lineTo(2f, 17f)
            }
}

public val LucideIcons.All.TrendingUpDefinition: LucideIconDefinition
    get() = _ActivityDefinition

