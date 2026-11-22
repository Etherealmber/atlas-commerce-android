package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("ZoomIn") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(3.0f, 11.0f)
                arcToRelative(8.0f, 8.0f, 0f, true, false, 16.0f, 0f)
                arcToRelative(8.0f, 8.0f, 0f, true, false, -16.0f, 0f)
            }
            lucidePath(strokeWidth) {
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
            lucidePath(strokeWidth) {
                moveTo(11f, 8f)
                lineTo(11f, 14f)
            }
            lucidePath(strokeWidth) {
                moveTo(8f, 11f)
                lineTo(14f, 11f)
            }
}

public val LucideIcons.All.ZoomInDefinition: LucideIconDefinition
    get() = _ActivityDefinition

