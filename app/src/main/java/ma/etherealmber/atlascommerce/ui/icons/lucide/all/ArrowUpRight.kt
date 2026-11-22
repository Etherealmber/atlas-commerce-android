package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ArrowUpRight") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(7f, 7f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(10f)
            }
            lucidePath(strokeWidth) {
                moveTo(7f, 17f)
                lineTo(17f, 7f)
            }
}

public val LucideIcons.All.ArrowUpRightDefinition: LucideIconDefinition
    get() = _ActivityDefinition

