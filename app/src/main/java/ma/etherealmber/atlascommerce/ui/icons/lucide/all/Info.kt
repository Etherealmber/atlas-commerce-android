package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("Info") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(2.0f, 12.0f)
                arcToRelative(10.0f, 10.0f, 0f, true, false, 20.0f, 0f)
                arcToRelative(10.0f, 10.0f, 0f, true, false, -20.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 16f)
                verticalLineToRelative(-4f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 8f)
                horizontalLineToRelative(.01f)
            }
}

public val LucideIcons.All.InfoDefinition: LucideIconDefinition
    get() = _ActivityDefinition

