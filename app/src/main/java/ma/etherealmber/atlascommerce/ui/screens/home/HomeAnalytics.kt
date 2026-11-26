package ma.etherealmber.atlascommerce.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import ma.etherealmber.atlascommerce.data.model.DailyMetricsDto
import ma.etherealmber.atlascommerce.data.model.MonthlyPointDto
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.LocalAtlasAnimationsEnabled
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasLocale
import ma.etherealmber.atlascommerce.ui.theme.Atlas

internal data class MetricsValue(val value: Double, val points: List<Double>, val deltaPercent: Double?, val isNew: Boolean)

internal fun metricsValue(metrics: DailyMetricsDto, key: String, window: Int = 7): MetricsValue? {
    val raw = metrics.series[key] ?: return null
    val periodDays = metrics.days.takeIf { it > 0 } ?: window
    val points = raw.map { value -> value?.takeIf { it.isFinite() } }
    val nonNull = points.mapIndexedNotNull { index, value -> value?.let { index to it } }
    if (nonNull.isEmpty()) return null
    val kind = metrics.kinds[key] ?: return null
    if (kind != "flow" && kind != "level") return null
    val isFlow = kind == "flow"
    val currentStart = (points.size - periodDays).coerceAtLeast(0)
    val previousStart = (currentStart - periodDays).coerceAtLeast(0)
    val currentValues = points.subList(currentStart, points.size).filterNotNull()
    val previousValues = points.subList(previousStart, currentStart).filterNotNull()
    if (isFlow && currentValues.isEmpty()) return null
    val value = if (isFlow) currentValues.sum() else nonNull.last().second
    val previous = if (isFlow) previousValues.takeIf { it.isNotEmpty() }?.sum()
    else nonNull.lastOrNull { it.first < currentStart }?.second
    val delta = previous?.takeIf { it != 0.0 }?.let { (value - it) / abs(it) * 100.0 }
    val chartPoints = points.takeLast(periodDays).map { it ?: 0.0 }
    return MetricsValue(value, chartPoints, delta, previous == 0.0)
}

internal fun latestMetric(metrics: DailyMetricsDto, key: String): Double? =
    metrics.series[key]?.lastOrNull { it != null && it.isFinite() }

private fun chartNumber(value: Double, decimals: Int = 0, locale: Locale = Locale.US): String {
    val symbols = DecimalFormatSymbols.getInstance(locale).apply {
        groupingSeparator = '\u00a0'
        zeroDigit = '0'
    }
    val decimalPattern = if (decimals > 0) "." + "0".repeat(decimals) else ""
    return DecimalFormat("#,##0$decimalPattern", symbols).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        isGroupingUsed = true
        groupingSize = 3
    }.format(if (value.isFinite()) value else 0.0)
}

private fun chartMoney(value: Double, currency: String, locale: Locale = Locale.US): String =
    "${chartNumber(value, locale = locale)}\u00a0$currency"

@Composable
internal fun homeMoney(value: Double, currency: String): String = chartMoney(value, currency, atlasLocale())

@Composable
internal fun homeInteger(value: Double): String = chartNumber(value, locale = atlasLocale())

@Composable
internal fun homeMoneyCompact(value: Double, currency: String): String =
    "${chartCompact(value, currency, atlasLocale())}\u00a0$currency"
@Composable
internal fun DailyKpiCarousel(
    metrics: DailyMetricsDto?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    currency: String,
) {
    val definitions = listOf(
        Triple("sales", "Ventes", "Sales"),
        Triple("money_in", "Encaissements", "Receipts"),
        Triple("stock_moves", "Mouvements de stock", "Stock movements"),
        Triple("low_stock", "Stock bas", "Low stock"),
        Triple("pending_actions", "Demandes en attente", "Pending requests"),
    )
    if (metrics == null) {
        when {
            loading -> {
                KpiGridSkeleton()
                return
            }
            error != null -> {
                AtlasErrorCard(
                    title = atlasText("Indicateurs indisponibles", "Metrics unavailable", "المؤشرات غير متاحة"),
                    message = error,
                    onRetry = onRetry,
                )
                return
            }
        }
    }

    val locale = atlasLocale()
    val cards = definitions.map { (key, french, english) ->
        val arabic = when (key) {
            "sales" -> "المبيعات"
            "money_in" -> "المقبوضات"
            "stock_moves" -> "حركات المخزون"
            "low_stock" -> "مخزون منخفض"
            else -> "الطلبات المعلقة"
        }
        Triple(key, atlasText(french, english, arabic), metrics?.let { metricsValue(it, key) })
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { rowCards ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCards.forEach { (key, label, data) ->
                    val isCurrency = key in setOf("sales", "money_in")
                    val unfavorableRise = key in setOf("low_stock", "pending_actions")
                    val positive = when {
                        data == null -> true
                        data.isNew && unfavorableRise -> data.value <= 0.0
                        data.deltaPercent == null -> true
                        unfavorableRise -> data.deltaPercent <= 0.0
                        else -> data.deltaPercent >= 0.0
                    }
                    val palette = Atlas.palette
                    val trendColor = if (data == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (positive) palette.positive else palette.danger
                    val trendContainer = if (data == null) MaterialTheme.colorScheme.surfaceVariant
                        else if (positive) palette.positiveSoft else palette.dangerSoft
                    val deltaLabel = when {
                        data == null -> atlasText("—", "No data", "لا توجد بيانات")
                        data.isNew -> atlasText("Nouveau", "New", "جديد")
                        data.deltaPercent == null -> atlasText("—", "—", "—")
                        else -> "${if ((data.deltaPercent ?: 0.0) >= 0) "↑" else "↓"} ${chartNumber(abs(data.deltaPercent ?: 0.0), 0, locale)}%"
                    }
                    val valueLabel = data?.let {
                        if (isCurrency) chartMoney(it.value, currency, locale) else chartNumber(it.value, locale = locale)
                    } ?: "—"
                    val cardDescription = if (data == null) "$label, ${atlasText("aucune donnée", "no data yet", "لا توجد بيانات بعد")}" else "$label, $valueLabel, $deltaLabel, ${atlasText("tendance sur 7 jours", "7-day trend", "اتجاه 7 أيام")}"
                    AtlasCard(
                        modifier = Modifier.weight(1f).height(128.dp).semantics { contentDescription = cardDescription },
                        padding = 10.dp,
                    ) {
                        Text(
                            "$label · ${atlasText("7 j", "7 d", "7 أيام")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            valueLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = trendContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    deltaLabel,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = trendColor,
                                    maxLines = 1,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.weight(1f))
                        Sparkline(data?.points.orEmpty(), trendColor, LocalAtlasAnimationsEnabled.current)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiGridSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(if (row == 2) 1 else 2) {
                    Surface(
                        Modifier.weight(1f).height(128.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {}
                }
                if (row == 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Double>, color: Color, animate: Boolean) {
    var revealed by remember(values) { mutableStateOf(false) }
    LaunchedEffect(values) { revealed = true }
    val target = if (values.size < 2) 0f else if (!animate || revealed) 1f else 0f
    val progress by animateFloatAsState(target, animationSpec = tween(if (animate) 250 else 0), label = "sparkline")
    Canvas(Modifier.fillMaxWidth().height(32.dp)) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.22f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())
            return@Canvas
        }
        val min = values.minOrNull() ?: 0.0
        val span = (values.maxOrNull() ?: 0.0) - min
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) size.width else size.width * index / (values.size - 1)
            val y = size.height - if (span == 0.0) size.height / 2 else ((value - min) / span).toFloat() * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = 0.22f + progress * 0.78f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun EmptyAnalyticsChart(title: String) {
    AtlasCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
            Text(
                atlasText("Aucune donnée pour le moment", "No chart data yet", "لا توجد بيانات للرسم بعد"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MoneyFlowChart(metrics: DailyMetricsDto?, currency: String) {
    if (metrics == null) {
        EmptyAnalyticsChart(
            title = atlasText("Entrées & sorties", "Money in & out", "المقبوضات والمدفوعات"),
        )
        return
    }
    val inSeries = metrics.series["money_in"]
    val outSeries = metrics.series["money_out"]
    val count = minOf(14, inSeries?.size ?: 0, outSeries?.size ?: 0)
    if (count == 0) {
        EmptyAnalyticsChart(
            title = atlasText("Entrées & sorties", "Money in & out", "المقبوضات والمدفوعات"),
        )
        return
    }
    val availableInSeries = inSeries ?: emptyList()
    val availableOutSeries = outSeries ?: emptyList()
    val incoming = availableInSeries.takeLast(count).map { it?.takeIf { value -> value.isFinite() } ?: 0.0 }
    val outgoing = availableOutSeries.takeLast(count).map { it?.takeIf { value -> value.isFinite() } ?: 0.0 }
    val inTotal = incoming.sum()
    val outTotal = outgoing.sum()
    val dates = metrics.dates.takeLast(count)
    val net = inTotal - outTotal
    var selected by remember(metrics.dates) { mutableStateOf<Int?>(null) }
    var barsRevealed by remember(metrics.dates) { mutableStateOf(false) }
    LaunchedEffect(metrics.dates) { barsRevealed = true }
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val barsProgress by animateFloatAsState(if (!motionEnabled || barsRevealed) 1f else 0f, tween(if (motionEnabled) 250 else 0), label = "daily-flow-bars")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val locale = atlasLocale()
    val palette = Atlas.palette
    val incomingColor = palette.chartIncome
    val outgoingColor = palette.chartExpense
    val description = atlasText(
        "Encaissé ${chartMoney(inTotal, currency, locale)}, décaissé ${chartMoney(outTotal, currency, locale)} sur 14 jours",
        "Received ${chartMoney(inTotal, currency, locale)}, paid out ${chartMoney(outTotal, currency, locale)} over 14 days",
        "المقبوض ${chartMoney(inTotal, currency, locale)}، المصروف ${chartMoney(outTotal, currency, locale)} خلال 14 يوماً",
    )
    AtlasCard(Modifier.semantics { contentDescription = description }, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(atlasText("Entrées & sorties", "Money in & out", "المقبوضات والمدفوعات"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Surface(color = if (net >= 0) palette.positiveSoft else palette.dangerSoft, shape = CircleShape) {
                Text("${if (net >= 0) "+" else "−"}${chartMoney(abs(net), currency, locale)}", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = if (net >= 0) palette.positive else palette.danger, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(incomingColor, "${atlasText("Encaissé", "Received", "مقبوض")}: ${chartCompact(inTotal, currency, locale)}")
            LegendDot(outgoingColor, "${atlasText("Décaissé", "Paid out", "مدفوع")}: ${chartCompact(outTotal, currency, locale)}")
        }
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().height(108.dp).pointerInput(count, rtl) {
            detectTapGestures { point ->
                val section = (point.x / size.width * count).toInt().coerceIn(0, count - 1)
                val index = if (rtl) count - 1 - section else section
                selected = if (selected == index) null else index
            }
        }) {
            val maxValue = max(1.0, (incoming + outgoing).maxOrNull() ?: 1.0)
            val groupWidth = size.width / count
            val barWidth = groupWidth * 0.28f
            val visibleIndex = { slot: Int -> if (rtl) count - 1 - slot else slot }
            repeat(count) { slot ->
                val i = visibleIndex(slot)
                val x0 = slot * groupWidth
                val alpha = if (selected == null || selected == i) 1f else 0.24f
                val hIn = (incoming[i] / maxValue).toFloat() * size.height * 0.88f * barsProgress
                val hOut = (outgoing[i] / maxValue).toFloat() * size.height * 0.88f * barsProgress
                drawRoundRect(incomingColor.copy(alpha = alpha), Offset(x0 + groupWidth * .18f, size.height - hIn), Size(barWidth, hIn), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                drawRoundRect(outgoingColor.copy(alpha = alpha), Offset(x0 + groupWidth * .55f, size.height - hOut), Size(barWidth, hOut), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val tickIndices = if (count <= 5) (0 until count).toList() else listOf(0, (count - 1) / 4, (count - 1) / 2, 3 * (count - 1) / 4, count - 1).distinct()
            tickIndices.forEach { slot ->
                val index = if (rtl) count - 1 - slot else slot
                Text(formatTick(dates.getOrNull(index).orEmpty()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        selected?.let { index ->
            val date = dates.getOrNull(index)?.let(::formatTick).orEmpty()
            Text(
                atlasText("$date · encaissé ${chartMoney(incoming[index], currency, locale)} · décaissé ${chartMoney(outgoing[index], currency, locale)}", "$date · received ${chartMoney(incoming[index], currency, locale)} · paid out ${chartMoney(outgoing[index], currency, locale)}", "$date · المقبوض ${chartMoney(incoming[index], currency, locale)} · المدفوع ${chartMoney(outgoing[index], currency, locale)}"),
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

private fun formatTick(date: String): String = runCatching {
    val day = LocalDate.parse(date.take(10))
    "%02d/%02d".format(Locale.US, day.dayOfMonth, day.monthValue)
}.getOrElse { date.takeLast(5) }

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
internal fun StockHealthChart(metrics: DailyMetricsDto?, onViewAlerts: () -> Unit) {
    if (metrics == null) {
        EmptyAnalyticsChart(atlasText("Santé du stock", "Stock health", "حالة المخزون"))
        return
    }
    val explicitInStock = latestMetric(metrics, "in_stock")
    val low = latestMetric(metrics, "low_stock")
    val out = latestMetric(metrics, "out_of_stock")
    if (low == null || out == null) {
        EmptyAnalyticsChart(atlasText("Santé du stock", "Stock health", "حالة المخزون"))
        return
    }
    val products = latestMetric(metrics, "products")
    if (explicitInStock == null && products == null) {
        EmptyAnalyticsChart(atlasText("Santé du stock", "Stock health", "حالة المخزون"))
        return
    }
    val inStock = explicitInStock ?: ((products ?: 0.0) - low - out).coerceAtLeast(0.0)
    val values = listOf(inStock.coerceAtLeast(0.0), low.coerceAtLeast(0.0), out.coerceAtLeast(0.0))
    val total = values.sum()
    if (total <= 0) {
        EmptyAnalyticsChart(atlasText("Santé du stock", "Stock health", "حالة المخزون"))
        return
    }
    val palette = Atlas.palette
    val colors = listOf(palette.positive, palette.warning, palette.danger)
    val names = listOf(atlasText("En stock", "In stock", "متوفر"), atlasText("Stock bas", "Low stock", "مخزون منخفض"), atlasText("Rupture", "Out of stock", "نفد المخزون"))
    var selected by remember(metrics.dates) { mutableStateOf<Int?>(null) }
    val percentage = (values[0] / total * 100).roundToInt()
    var donutRevealed by remember(metrics.dates) { mutableStateOf(false) }
    LaunchedEffect(metrics.dates) { donutRevealed = true }
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    val donutProgress by animateFloatAsState(if (!motionEnabled || donutRevealed) 1f else 0f, tween(if (motionEnabled) 250 else 0), label = "stock-donut")
    val locale = atlasLocale()
    val stockDescription = atlasText(
        "Stock : ${chartNumber(values[0], locale = locale)} en stock, ${chartNumber(values[1], locale = locale)} bas, ${chartNumber(values[2], locale = locale)} en rupture",
        "Stock: ${chartNumber(values[0], locale = locale)} in stock, ${chartNumber(values[1], locale = locale)} low, ${chartNumber(values[2], locale = locale)} out",
        "المخزون: ${chartNumber(values[0], locale = locale)} متوفر، ${chartNumber(values[1], locale = locale)} منخفض، ${chartNumber(values[2], locale = locale)} نافد",
    )
    AtlasCard(Modifier.semantics { contentDescription = stockDescription }, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(atlasText("Santé du stock", "Stock health", "حالة المخزون"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onViewAlerts, modifier = Modifier.height(48.dp)) { Text(atlasText("Voir", "View", "عرض")) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(132.dp).pointerInput(values) {
                    detectTapGestures { point ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = Math.toDegrees(kotlin.math.atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble())).toFloat() + 90f
                        val normalized = (angle + 360f) % 360f
                        val clicked = values.indices.firstOrNull { i ->
                            val start = values.take(i).sum() / total * 360f
                            val end = start + values[i] / total * 360f
                            normalized in start..end
                        }
                        if (clicked != null) selected = if (selected == clicked) null else clicked
                    }
                }) {
                    val strokeWidth = 16.dp.toPx()
                    val diameter = (size.minDimension - strokeWidth).coerceAtLeast(0f)
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val nonZeroSlices = values.indices.filter { values[it] > 0.0 }
                    if (nonZeroSlices.size == 1 && donutProgress >= 1f) {
                        drawCircle(
                            color = colors[nonZeroSlices.single()],
                            radius = diameter / 2f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = strokeWidth),
                        )
                    } else {
                        var start = -90f
                        values.forEachIndexed { index, value ->
                            val sweep = (value / total * 360.0 * donutProgress.toDouble()).toFloat()
                            if (sweep > 0f) {
                                drawArc(
                                    color = colors[index],
                                    startAngle = start,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                )
                            }
                            start += sweep
                        }
                    }
                }
                val centerText = selected?.let { chartNumber(values[it], locale = locale) } ?: atlasText("$percentage %\nen stock", "$percentage %\nin stock", "$percentage %\nمتوفر")
                Text(centerText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), lineHeight = 16.sp, maxLines = 2)
            }
            Column(Modifier.weight(1f)) {
                names.forEachIndexed { index, name ->
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(10.dp)).clickable { selected = if (selected == index) null else index }.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(colors[index]))
                        Spacer(Modifier.width(7.dp))
                        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Clip)
                        Text(chartNumber(values[index], locale = locale), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                    }
                }
            }
        }
    }
}

@Composable
internal fun MonthlySalesTrendCard(points: List<MonthlyPointDto>, currency: String) {
    val recent = points.takeLast(6)
    if (recent.isEmpty()) {
        EmptyAnalyticsChart(atlasText("Tendance des ventes", "Sales trend", "اتجاه المبيعات"))
        return
    }
    val maxValue = recent.maxOfOrNull { it.sales.coerceAtLeast(0.0) }?.coerceAtLeast(1.0) ?: 1.0
    val palette = Atlas.palette
    val motion = LocalAtlasAnimationsEnabled.current
    val locale = atlasLocale()
    val description = recent.joinToString("; ") { "${it.label.ifBlank { it.key }} ${chartMoney(it.sales, currency, locale)}" }
    var barsRevealed by remember(recent) { mutableStateOf(false) }
    LaunchedEffect(recent) { barsRevealed = true }
    AtlasCard(Modifier.semantics { contentDescription = description }, padding = 14.dp) {
        Text(atlasText("Tendance des ventes", "Sales trend", "اتجاه المبيعات"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(126.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            recent.forEach { point ->
                val target = (point.sales.coerceAtLeast(0.0) / maxValue).toFloat()
                val fraction by animateFloatAsState(if (!motion || barsRevealed) target else 0f, tween(if (motion) 250 else 0), label = "monthly-bar-${point.key}")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(chartCompact(point.sales, currency, locale), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                        modifier = Modifier.semantics { contentDescription = chartMoney(point.sales, currency, locale) })
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth(.56f).fillMaxHeight(fraction.coerceAtLeast(.025f)).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(palette.chartSales))
                    }
                    val tick = runCatching {
                        YearMonth.parse(point.key).month.getDisplayName(TextStyle.SHORT, locale)
                    }.getOrDefault(point.label.ifBlank { point.key })
                    Text(tick, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}

private fun chartCompact(value: Double, currency: String, locale: Locale = Locale.US): String {
    val absolute = abs(value)
    val (divisor, suffix) = when {
        absolute >= 1_000_000 -> 1_000_000.0 to "M"
        absolute >= 1_000 -> 1_000.0 to "k"
        else -> 1.0 to ""
    }
    val scaled = value / divisor
    val decimals = if (divisor == 1.0 || abs(scaled - scaled.toLong()) < 0.0001) 0 else 1
    val result = chartNumber(scaled, decimals, locale)
    return if (suffix.isEmpty()) result else "$result\u00a0$suffix"
}

@Composable
internal fun HomeMetricsChartSkeleton(height: androidx.compose.ui.unit.Dp) {
    Surface(
        Modifier.fillMaxWidth().height(height),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
    ) {}
}

@Composable
internal fun FinanceMovementFlowChart(summary: ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto) {
    val palette = Atlas.palette
    val entries = listOf(
        Triple(atlasText("Ventes encaissées", "Sales received", "مبيعات محصلة"), summary.salesReceived, palette.chartSales),
        Triple(atlasText("Dépenses", "Expenses", "المصروفات"), summary.expenses, palette.chartExpense),
        Triple(atlasText("Salaires versés", "Payroll paid", "رواتب مدفوعة"), summary.payrollPaid, palette.chartReceivable),
        Triple(atlasText("Fournisseurs réglés", "Suppliers paid", "الموردون المدفوعون"), summary.supplierPaid, palette.chartPayable),
    )
    val maximum = entries.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val net = summary.salesReceived - (summary.expenses + summary.payrollPaid + summary.supplierPaid)
    val locale = atlasLocale()
    val motionEnabled = LocalAtlasAnimationsEnabled.current
    var barsRevealed by remember(summary.period) { mutableStateOf(false) }
    LaunchedEffect(summary.period) { barsRevealed = true }
    AtlasCard(padding = 12.dp) {
        Text(atlasText("Mouvements du mois", "Monthly movements", "حركات الشهر"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        entries.forEach { (label, value, color) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                Text(chartMoney(value, summary.currency, locale), style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
            }
            val targetFraction = (value.coerceAtLeast(0.0) / maximum).toFloat().coerceIn(0f, 1f)
            val fraction by animateFloatAsState(
                targetValue = if (!motionEnabled || barsRevealed) targetFraction else 0f,
                animationSpec = tween(if (motionEnabled) 250 else 0),
                label = "finance-flow-${label.hashCode()}",
            )
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(6.dp)).background(color))
            }
            Spacer(Modifier.height(5.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(atlasText("Résultat de trésorerie", "Net cash flow", "صافي التدفق النقدي"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                chartMoney(net, summary.currency, locale),
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = if (net >= 0) palette.positive else palette.danger,
            )
        }
    }
}
