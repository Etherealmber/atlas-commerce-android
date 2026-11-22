package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val _ActivityDefinition = LucideIconDefinition("TriangleAlert") { strokeWidth ->
    lucidePath(strokeWidth) {
                moveToRelative(21.73f, 18f)
                lineToRelative(-8f, -14f)
                arcToRelative(2f, 2f, 0f, false, false, -3.48f, 0f)
                lineToRelative(-8f, 14f)
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                horizontalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f, false, false, 1.73f, -3f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 9f)
                verticalLineToRelative(4f)
            }
            lucidePath(strokeWidth) {
                moveTo(12f, 17f)
                horizontalLineToRelative(.01f)
            }
}

public val LucideIcons.All.TriangleAlertDefinition: LucideIconDefinition
    get() = _ActivityDefinition

