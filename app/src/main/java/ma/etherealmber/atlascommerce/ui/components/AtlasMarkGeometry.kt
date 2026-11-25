package ma.etherealmber.atlascommerce.ui.components

import kotlin.math.cos
import kotlin.math.sin

/** Geometry shared by the app mark, splash icon, and inline activity indicator. */
object AtlasMarkGeometry {
    const val VIEWPORT_SIZE = 252f
    const val CENTER = 126f
    const val INNER_RADIUS = 79f
    const val OUTER_RADIUS = 125f
    const val STROKE_UNITS = 9f
    const val TICK_COUNT = 15

    data class Point(val x: Float, val y: Float)
    data class Segment(val start: Point, val end: Point)

    /** Fifteen rays, ordered clockwise from the replaced three-o'clock beam. */
    val ticks: List<Segment> = (1..TICK_COUNT).map { k ->
        val radians = Math.toRadians(k * 22.5).toFloat()
        val inner = Point(CENTER + INNER_RADIUS * cos(radians), CENTER + INNER_RADIUS * sin(radians))
        val outer = Point(CENTER + OUTER_RADIUS * cos(radians), CENTER + OUTER_RADIUS * sin(radians))
        Segment(inner, outer)
    }

    val beam: Segment = Segment(Point(CENTER, CENTER), Point(CENTER + OUTER_RADIUS, CENTER))

    /** SVG-like stroke width before mapping the 252-unit viewport to dp. */
    fun opticalStrokeUnits(markSizeDp: Float): Float = when {
        markSizeDp <= 24f -> 14f
        markSizeDp <= 40f -> 13f
        else -> STROKE_UNITS
    }
}
