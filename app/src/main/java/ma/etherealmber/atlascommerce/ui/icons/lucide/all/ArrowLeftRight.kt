package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ArrowLeftRight") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(8f, 3f)
                lineTo(4f, 7f)
                lineToRelative(4f, 4f)
            }
            lucidePath(strokeWidth) {
                moveTo(4f, 7f)
                horizontalLineToRelative(16f)
            }
            lucidePath(strokeWidth) {
                moveToRelative(16f, 21f)
                lineToRelative(4f, -4f)
                lineToRelative(-4f, -4f)
            }
            lucidePath(strokeWidth) {
                moveTo(20f, 17f)
                horizontalLineTo(4f)
            }
}

public val LucideIcons.All.ArrowLeftRightDefinition: LucideIconDefinition
    get() = _ActivityDefinition

