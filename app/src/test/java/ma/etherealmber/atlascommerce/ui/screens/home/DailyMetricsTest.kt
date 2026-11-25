package ma.etherealmber.atlascommerce.ui.screens.home

import ma.etherealmber.atlascommerce.data.model.DailyMetricsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyMetricsTest {
    @Test
    fun flowValueSumsLastSevenAndComparesPreviousSeven() {
        val metrics = DailyMetricsDto(
            days = 7,
            series = mapOf("sales" to (List(7) { 10.0 } + List(7) { 20.0 })),
            kinds = mapOf("sales" to "flow"),
        )
        val result = metricsValue(metrics, "sales")!!
        assertEquals(140.0, result.value, 0.001)
        assertEquals(100.0, result.deltaPercent!!, 0.001)
        assertFalse(result.isNew)
    }

    @Test
    fun zeroPreviousFlowIsReportedAsNew() {
        val metrics = DailyMetricsDto(
            series = mapOf("sales" to (List(7) { 0.0 } + List(7) { 2.0 })),
            kinds = mapOf("sales" to "flow"),
        )
        val result = metricsValue(metrics, "sales")!!
        assertEquals(14.0, result.value, 0.001)
        assertTrue(result.isNew)
        assertNull(result.deltaPercent)
    }

    @Test
    fun missingOrEmptyRoleRestrictedSeriesIsNotShownAsZero() {
        assertNull(metricsValue(DailyMetricsDto(), "sales"))
        val emptyCurrentWindow = DailyMetricsDto(
            series = mapOf("sales" to (List(7) { 3.0 } + List(7) { null })),
            kinds = mapOf("sales" to "flow"),
        )
        assertNull(metricsValue(emptyCurrentWindow, "sales"))
    }

    @Test
    fun missingKindIsNotGuessed() {
        val metrics = DailyMetricsDto(series = mapOf("sales" to List(14) { 1.0 }))
        assertNull(metricsValue(metrics, "sales"))
    }

    @Test
    fun levelUsesLatestNonNullRatherThanSumming() {
        val metrics = DailyMetricsDto(
            series = mapOf("products" to (List(7) { 20.0 } + listOf(21.0, null, 23.0, null, null, null, null))),
            kinds = mapOf("products" to "level"),
        )
        assertEquals(23.0, metricsValue(metrics, "products")!!.value, 0.001)
    }
}
