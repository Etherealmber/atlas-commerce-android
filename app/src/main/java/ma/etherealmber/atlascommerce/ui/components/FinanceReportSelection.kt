package ma.etherealmber.atlascommerce.ui.components

import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

/** Report paths and the server-issued HMAC ticket are kept separate; only the period selection is user data. */
enum class FinanceReportSection(val key: String, val label: String, val path: String) {
    BALANCE("balance", "Balance", "balance"),
    STOCK("stock", "Stock", "stock"),
    RECEIVABLES("receivables", "Créances clients", "receivables"),
    PAYABLES("payables", "Fournisseurs", "payables"),
    EMPLOYEES("employees", "Employés & paie", "employees"),
    AGING("aging", "Balance âgée", "aging.html"),
    TVA("tva", "TVA", "tva.html"),
}

data class FinanceReportSelection(
    val period: String = "month",
    val from: String? = null,
    val to: String? = null,
    val sectionKeys: List<String> = FinanceReportSection.entries.map { it.key },
) {
    val isCustom: Boolean get() = period == "custom"
    val shortLabel: String get() = when (period) {
        "month" -> "Mois en cours"
        "3m" -> "3 derniers mois"
        "6m" -> "6 derniers mois"
        "12m" -> "12 derniers mois"
        "year" -> "Année en cours"
        "custom" -> if (from != null && to != null) "$from → $to" else "Période personnalisée"
        else -> period
    }

    fun selectedSections(): List<FinanceReportSection> =
        FinanceReportSection.entries.filter { it.key in sectionKeys }
}

@Composable
fun FinanceReportSelectionDialog(
    initial: FinanceReportSelection = FinanceReportSelection(),
    onDismiss: () -> Unit,
    onGenerate: (FinanceReportSelection) -> Unit,
) {
    val context = LocalContext.current
    var period by remember(initial) { mutableStateOf(initial.period) }
    var from by remember(initial) { mutableStateOf(initial.from ?: LocalDate.now().withDayOfMonth(1).toString()) }
    var to by remember(initial) { mutableStateOf(initial.to ?: LocalDate.now().toString()) }
    val selectedSections = remember(initial) { mutableStateListOf<String>().apply { addAll(initial.sectionKeys) } }
    val today = remember { LocalDate.now() }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val presets = listOf(
        "month" to atlasText("Ce mois", "This month", "هذا الشهر"),
        "3m" to atlasText("3 mois", "3 months", "3 أشهر"),
        "6m" to atlasText("6 mois", "6 months", "6 أشهر"),
        "12m" to atlasText("12 mois", "12 months", "12 شهراً"),
        "year" to atlasText("Année en cours", "Current year", "السنة الحالية"),
        "custom" to atlasText("Du… au…", "From… to…", "من… إلى…"),
    )
    val customRangeValid = runCatching {
        val start = LocalDate.parse(from)
        val end = LocalDate.parse(to)
        !end.isBefore(start) && !end.isAfter(today)
    }.getOrDefault(false)
    val canGenerate = selectedSections.isNotEmpty() && (period != "custom" || customRangeValid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(atlasText("Personnaliser le rapport"), style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.58f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(atlasText("Choisissez une période et les rubriques à consulter."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(atlasText("PÉRIODE", "PERIOD", "الفترة"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, label) ->
                                FilterChip(
                                    selected = period == key,
                                    onClick = { period = key },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (period == "custom") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateChoiceButton(atlasText("Du", "From", "من"), from, Modifier.weight(1f)) {
                                showDatePicker(context, from, today, null) { picked -> from = picked; if (to < picked) to = picked }
                            }
                            DateChoiceButton(atlasText("Au", "To", "إلى"), to, Modifier.weight(1f)) {
                                val minDate = runCatching { LocalDate.parse(from) }.getOrNull()
                                showDatePicker(context, to, today, minDate) { picked -> to = picked }
                            }
                        }
                        if (!customRangeValid) {
                            Text(atlasText("Vérifiez les dates : début avant fin, et aucune date future.", "Check the dates: the start must precede the end and neither date can be in the future.", "تحقق من التواريخ: يجب أن يسبق تاريخ البداية النهاية وألا يكون أي تاريخ في المستقبل."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Text(
                        text = when (period) {
                            "year" -> atlasText("Du 1er janvier à aujourd'hui, selon le fuseau financier du serveur.")
                            "month" -> atlasText("Mois en cours jusqu'à aujourd'hui ; les périodes passées couvrent le mois complet.")
                            else -> atlasText("Période calculée par le serveur dans le fuseau financier configuré.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(atlasText("RUBRIQUES", "SECTIONS", "الأقسام"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FinanceReportSection.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { report ->
                            FilterChip(
                                selected = report.key in selectedSections,
                                onClick = {
                                    if (report.key in selectedSections) selectedSections.remove(report.key) else selectedSections.add(report.key)
                                },
                                label = { Text(report.localizedLabel(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text(
                    atlasText("Le lien d'accès est temporaire (30 min). Les rubriques sélectionnées s'ouvrent dans Atlas.", "The access link is temporary (30 min). Selected sections open in Atlas.", "رابط الوصول مؤقت (30 دقيقة). تُفتح الأقسام المحددة في Atlas."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGenerate(FinanceReportSelection(period, from.takeIf { period == "custom" }, to.takeIf { period == "custom" }, selectedSections.toList())) }, enabled = canGenerate) {
                Text(atlasText("Préparer"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(atlasText("Annuler")) } },
    )
}

@Composable
private fun DateChoiceButton(label: String, date: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun FinanceReportSection.localizedLabel(): String = when (this) {
    FinanceReportSection.BALANCE -> atlasText("Balance", "Balance", "الرصيد")
    FinanceReportSection.STOCK -> atlasText("Stock", "Inventory", "المخزون")
    FinanceReportSection.RECEIVABLES -> atlasText("Créances clients", "Customer receivables", "مستحقات العملاء")
    FinanceReportSection.PAYABLES -> atlasText("Fournisseurs", "Suppliers", "الموردون")
    FinanceReportSection.EMPLOYEES -> atlasText("Employés & paie", "Employees & payroll", "الموظفون والرواتب")
    FinanceReportSection.AGING -> atlasText("Balance âgée", "Aging report", "تقرير أعمار الديون")
    FinanceReportSection.TVA -> atlasText("TVA", "VAT", "ضريبة القيمة المضافة")
}

private fun showDatePicker(
    context: android.content.Context,
    current: String,
    today: LocalDate,
    minimum: LocalDate?,
    onPicked: (String) -> Unit,
) {
    val date = runCatching { LocalDate.parse(current) }.getOrElse { today }
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day).toString()) },
        date.year,
        date.monthValue - 1,
        date.dayOfMonth,
    ).apply {
        datePicker.maxDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        minimum?.let { datePicker.minDate = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    }.show()
}

@Composable
fun FinanceReportActionsDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(max = maxHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(atlasText("Rapport prêt"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = atlasText("Fermer")) }
                }
                Text(
                    atlasText(
                        "${fileName}\nPDF temporaire conservé en privé sur cet appareil pendant 7 jours maximum.",
                        "${fileName}\nTemporary PDF is stored privately on this device for up to 7 days.",
                        "${fileName}\nيُحفظ ملف PDF مؤقتاً وبشكل خاص على هذا الجهاز لمدة تصل إلى 7 أيام.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onRead, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) {
                    Text(atlasText("Lire dans Atlas"), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(atlasText("Partager le PDF"))
                }
                OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(atlasText("Enregistrer une copie"))
                }
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(atlasText("Supprimer le rapport temporaire"))
                }
            }
        }
    }
}

