package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val compassDefinition = LucideIconDefinition("Compass") { strokeWidth ->
    lucidePath(strokeWidth) {
        moveTo(12f, 2f)
        arcToRelative(10f, 10f, 0f, true, true, 0f, 20f)
        arcToRelative(10f, 10f, 0f, true, true, 0f, -20f)
        moveTo(16.24f, 7.76f)
        lineTo(14.12f, 14.12f)
        lineTo(7.76f, 16.24f)
        lineTo(9.88f, 9.88f)
        close()
    }
}

val LucideIcons.All.CompassDefinition: LucideIconDefinition get() = compassDefinition
