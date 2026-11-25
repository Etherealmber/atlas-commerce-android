package ma.etherealmber.atlascommerce.ui.icons.lucide.all

import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIcons
import ma.etherealmber.atlascommerce.ui.icons.lucide.LucideIconDefinition
import ma.etherealmber.atlascommerce.ui.icons.lucide.lucidePath

private val moonDefinition = LucideIconDefinition("Moon") { strokeWidth ->
    lucidePath(strokeWidth) {
        moveTo(20.985f, 12.486f)
        arcToRelative(9f, 9f, 0f, true, true, -9.473f, -9.472f)
        curveToRelative(0.405f, -0.022f, 0.617f, 0.46f, 0.402f, 0.803f)
        arcToRelative(6.5f, 6.5f, 0f, false, false, 8.268f, 8.268f)
        curveToRelative(0.344f, -0.215f, 0.825f, -0.003f, 0.803f, 0.401f)
    }
}

val LucideIcons.All.MoonDefinition: LucideIconDefinition get() = moonDefinition
