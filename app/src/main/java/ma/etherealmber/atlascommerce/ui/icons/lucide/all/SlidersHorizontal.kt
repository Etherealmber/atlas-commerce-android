package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("SlidersHorizontal") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveTo(10f, 5f)
                horizontalLineTo(3f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 19f)
                horizontalLineTo(3f)
            }
            lucidePath(strokeWidth) {
                moveTo(14f, 3f)
                verticalLineToRelative(4f)
            }
            lucidePath(strokeWidth) {
                moveTo(16f, 17f)
                verticalLineToRelative(4f)
            }
            lucidePath(strokeWidth) {
                moveTo(21f, 12f)
                horizontalLineToRelative(-9f)
            }
            lucidePath(strokeWidth) {
                moveTo(21f, 19f)
                horizontalLineToRelative(-5f)
            }
            lucidePath(strokeWidth) {
                moveTo(21f, 5f)
                horizontalLineToRelative(-7f)
            }
            lucidePath(strokeWidth) {
                moveTo(8f, 10f)
                verticalLineToRelative(4f)
            }
            lucidePath(strokeWidth) {
                moveTo(8f, 12f)
                horizontalLineTo(3f)
            }
}

public val LucideIcons.All.SlidersHorizontalDefinition: LucideIconDefinition
    get() = _ActivityDefinition

