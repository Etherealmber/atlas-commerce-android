package ma.etherealmber.atlascommerce.ui.screens.notifications

import ma.etherealmber.atlascommerce.ui.i18n.LocalAtlasLanguage
import ma.etherealmber.atlascommerce.ui.i18n.atlasNotice
import ma.etherealmber.atlascommerce.ui.i18n.atlasText
import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleLabel
import ma.etherealmber.atlascommerce.ui.i18n.atlasRoleScopeSummary
import ma.etherealmber.atlascommerce.ui.i18n.atlasInteger
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ma.etherealmber.atlascommerce.core.RoleTarget
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.ui.components.AtlasButton
import ma.etherealmber.atlascommerce.ui.components.AtlasCard
import ma.etherealmber.atlascommerce.ui.components.AtlasChip
import ma.etherealmber.atlascommerce.ui.components.AtlasEmptyState
import ma.etherealmber.atlascommerce.ui.components.AtlasEntrance
import ma.etherealmber.atlascommerce.ui.components.AtlasErrorCard
import ma.etherealmber.atlascommerce.ui.components.AtlasSkeletonList
import ma.etherealmber.atlascommerce.ui.components.AtlasSectionHeader
import ma.etherealmber.atlascommerce.ui.components.AtlasTone
import ma.etherealmber.atlascommerce.ui.components.AtlasTopBar
import ma.etherealmber.atlascommerce.ui.theme.Atlas

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateHref: (String?) -> Unit,
    onToast: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = Atlas.density
    val roleLabel = atlasRoleLabel(state.role)
    var showComposeDialog by remember { mutableStateOf(false) }
    val systemNotificationsEnabled = runCatching {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(true)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg -> onToast(msg) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AtlasTopBar(
                title = atlasText("Info Box & Notifications", "Info Box & Notifications", "صندوق المعلومات والإشعارات"),
                subtitle = atlasText(
                    "${roleLabel} · ${state.alerts.size} alerte(s) · ${state.pendingCount} en attente",
                    "${roleLabel} · ${state.alerts.size} alert(s) · ${state.pendingCount} pending",
                    "${roleLabel} · ${state.alerts.size} تنبيه · ${state.pendingCount} معلّق",
                ),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showComposeDialog = true }) {
                        Icon(Icons.Outlined.Send, contentDescription = atlasText("Envoyer une notification"))
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = atlasText("Actualiser"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = density.screenPadding,
                end = density.screenPadding,
                top = density.cardMargin,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(density.itemGap),
        ) {
            // Role-Based Info Box Banner + Send Role Notification CTA
            if (!systemNotificationsEnabled) {
                item {
                    AtlasCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = Atlas.palette.warning,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = atlasText("Notifications Android désactivées"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = atlasText("Activez les notifications système dans les Paramètres Android pour recevoir immédiatement les alertes et suivre les téléchargements."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        AtlasButton(
                            text = atlasText("Activer les notifications dans Paramètres", "Enable notifications in Settings", "تفعيل الإشعارات في الإعدادات"),
                            onClick = {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            icon = Icons.Outlined.NotificationsActive,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                AtlasEntrance {
                    AtlasCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = atlasText("Info Box · ${roleLabel}", "Info Box · ${roleLabel}", "صندوق المعلومات · ${roleLabel}"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = atlasRoleScopeSummary(state.role),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AtlasChip(
                                text = if (state.isAdmin) atlasText("Admin") else atlasText("Filtré rôle"),
                                tone = if (state.isAdmin) AtlasTone.POSITIVE else AtlasTone.INFO,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        AtlasButton(
                            text = atlasText("Envoyer une notification par rôle"),
                            onClick = { showComposeDialog = true },
                            icon = Icons.Outlined.Send,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            when {
                state.loading && state.alerts.isEmpty() && state.actions.isEmpty() -> {
                    item {
                        AtlasSkeletonList(Modifier.fillMaxWidth(), rows = 4, avatarSize = 38.dp)
                    }
                }

                !state.error.isNullOrBlank() && state.alerts.isEmpty() && state.actions.isEmpty() -> {
                    item {
                        AtlasErrorCard(message = atlasNotice(state.error.orEmpty(), LocalAtlasLanguage.current), onRetry = viewModel::refresh)
                    }
                }

                state.alerts.isEmpty() && state.actions.isEmpty() -> {
                    item {
                        AtlasEmptyState(
                            title = atlasText("Aucune notification pour votre rôle"),
                            message = atlasText(
                                "Votre flux ${roleLabel} est à jour. Vous pouvez envoyer un message ciblé à une équipe ci-dessus.",
                                "Your ${roleLabel} feed is up to date. You can send a targeted message to a team above.",
                                "تدفّق ${roleLabel} محدّث. يمكنك إرسال رسالة موجهة إلى فريق أعلاه.",
                            ),
                            icon = Icons.Outlined.NotificationsNone,
                            actionLabel = atlasText("Actualiser"),
                            onAction = viewModel::refresh,
                        )
                    }
                }

                else -> {
                    if (state.alerts.isNotEmpty()) {
                        item {
                            AtlasSectionHeader(
                                title = atlasText("Alertes ${roleLabel} (${state.alerts.size})", "${roleLabel} alerts (${state.alerts.size})", "تنبيهات ${roleLabel} (${state.alerts.size})"),
                                subtitle = atlasText("Filtrées automatiquement selon vos permissions"),
                            )
                        }
                        items(state.alerts) { alert ->
                            AlertRow(
                                alert = alert,
                                onClick = { onNavigateHref(alert.href) },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        AtlasSectionHeader(
                            title = atlasText("Demandes & notifications par rôle"),
                            subtitle = if (state.isAdmin) {
                                atlasText("Validez en 1 clic les demandes produits envoyées par l'équipe")
                            } else {
                                atlasText("Notifications reçues pour votre rôle et suivi de vos envois Admin")
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !state.pendingOnly,
                                onClick = { viewModel.togglePendingOnly(false) },
                                label = { Text(atlasText("Toutes (${state.actions.size})", "All (${state.actions.size})", "الكل (${state.actions.size})")) },
                            )
                            FilterChip(
                                selected = state.pendingOnly,
                                onClick = { viewModel.togglePendingOnly(true) },
                                label = { Text(atlasText("En attente (${state.pendingCount})", "Pending (${state.pendingCount})", "معلّق (${state.pendingCount})")) },
                            )
                        }
                    }

                    items(state.visibleActions, key = { it.id }) { action ->
                        MobileActionRow(
                            action = action,
                            isAdmin = state.isAdmin,
                            currentUsername = state.username,
                            busy = state.busyId == action.id,
                            onComplete = { viewModel.completeAction(action) },
                            onReject = { viewModel.rejectAction(action) },
                        )
                    }
                }
            }
        }
    }

    if (showComposeDialog) {
        SendRoleNotificationDialog(
            availableTargets = state.availableTargets,
            sending = state.sendingNotification,
            onDismiss = { showComposeDialog = false },
            onSend = { target, title, message, urgent ->
                viewModel.sendNotificationToRole(
                    target = target,
                    title = title,
                    message = message,
                    urgent = urgent,
                    onSent = { showComposeDialog = false },
                )
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SendRoleNotificationDialog(
    availableTargets: List<RoleTarget>,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSend: (target: RoleTarget, title: String, message: String, urgent: Boolean) -> Unit,
) {
    var selectedTarget by remember(availableTargets) {
        mutableStateOf(availableTargets.firstOrNull() ?: RoleTarget.ADMIN)
    }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = {
            Text(
                text = atlasText("Envoyer une notification par rôle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = atlasText("Destinataires (rôle cible)"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableTargets.forEach { target ->
                        FilterChip(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            label = { Text(localizedTargetLabel(target)) },
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(atlasText("Objet de la notification *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(atlasText("Détails / Instructions")) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = atlasText("Priorité urgente"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = atlasText("Déclenche une alerte haute priorité sur les appareils du rôle", "Triggers a high-priority alert on devices for this role", "يُطلق تنبيهاً عالي الأولوية على أجهزة هذا الدور"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = urgent, onCheckedChange = { urgent = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.trim().isNotEmpty() && !sending,
                onClick = { onSend(selectedTarget, title, message, urgent) },
            ) {
                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(atlasText("Envoyer"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !sending) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(atlasText("Annuler"))
            }
        },
    )
}

@Composable
private fun AlertRow(
    alert: AlertItemDto,
    onClick: () -> Unit,
) {
    val count = alert.count?.let { atlasInteger(it.toDouble()) }
    val localizedLabel = when {
        alert.kind.contains("low_stock", ignoreCase = true) || alert.kind.contains("stock_low", ignoreCase = true) ->
            atlasText("${count ?: alert.label} article(s) sous le seuil de stock", "${count ?: alert.label} low-stock item(s)", "${count ?: alert.label} منتجات دون حد المخزون")
        alert.kind.contains("overdue", ignoreCase = true) || alert.kind.contains("unpaid_invoice", ignoreCase = true) ->
            atlasText("${count ?: alert.label} facture(s) impayée(s)", "${count ?: alert.label} unpaid invoice(s)", "${count ?: alert.label} فواتير غير مدفوعة")
        else -> atlasText(alert.label)
    }
    val isHigh = alert.severity.equals("high", ignoreCase = true) ||
        alert.severity.equals("danger", ignoreCase = true) ||
        alert.kind.contains("stock", ignoreCase = true)
    AtlasCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isHigh) Icons.Outlined.Warning else Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = if (isHigh) Atlas.palette.warning else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = localizedLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            if ((alert.count ?: 0) > 0) {
                Spacer(Modifier.width(8.dp))
                AtlasChip(
                    text = alert.count.toString(),
                    tone = if (isHigh) AtlasTone.WARNING else AtlasTone.INFO,
                )
            }
        }
    }
}

@Composable
private fun localizedTargetLabel(target: RoleTarget): String = when (target) {
    RoleTarget.ADMIN -> atlasText("Administrateurs", "Administrators", "المسؤولون")
    RoleTarget.SECRETARY -> atlasText("Secrétariat & Ventes", "Sales secretaries", "السكرتارية والمبيعات")
    RoleTarget.STOCK -> atlasText("Magasiniers & Stock", "Warehouse & Stock", "المخازن والمخزون")
    RoleTarget.ALL -> atlasText("Tous les rôles", "All roles", "كل الأدوار")
}

@Composable
private fun MobileActionRow(
    action: MobileActionDto,
    isAdmin: Boolean,
    currentUsername: String,
    busy: Boolean,
    onComplete: () -> Unit,
    onReject: () -> Unit,
) {
    val isProductRequest = action.type.equals("new_product", ignoreCase = true)
    val isOwnSubmission = !currentUsername.isBlank() &&
        action.createdBy.equals(currentUsername, ignoreCase = true)

    val (statusKey, statusTone) = when {
        action.isCompleted -> "Validée ✓" to AtlasTone.POSITIVE
        action.isRejectedOrCancelled -> "Refusée" to AtlasTone.DANGER
        isProductRequest && !isAdmin -> "Transmis à l'Admin" to AtlasTone.INFO
        action.isUrgent -> "Urgent" to AtlasTone.DANGER
        else -> "En attente" to AtlasTone.WARNING
    }
    val statusLabel = when (statusKey) {
        "Validée ✓" -> atlasText("Validée ✓", "Approved ✓", "تم الاعتماد ✓")
        "Refusée" -> atlasText("Refusée", "Rejected", "مرفوض")
        "Transmis à l'Admin" -> atlasText("Transmis à l'Admin", "Sent to Admin", "أُرسل إلى المسؤول")
        "Urgent" -> atlasText("Urgent", "Urgent", "عاجل")
        else -> atlasText("En attente", "Pending", "قيد الانتظار")
    }

    val targetBadge = action.resolvedTargetRole?.let { localizedTargetLabel(RoleTarget.fromKey(it)) }

    AtlasCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AtlasChip(text = statusLabel, tone = statusTone)
                    if (targetBadge != null) {
                        AtlasChip(text = "→ $targetBadge", tone = AtlasTone.NEUTRAL)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = action.title.ifBlank {
                        atlasText("Notification #${action.id}", "Notification #${action.id}", "إشعار رقم ${action.id}")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!action.detail.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = action.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val footerMeta = listOfNotNull(
                    action.barcode?.takeIf { it.isNotBlank() }?.let { atlasText("Code : $it", "Code: $it", "الرمز: $it") },
                    action.createdBy?.takeIf { it.isNotBlank() }?.let { atlasText("Par $it", "By $it", "بواسطة $it") },
                    action.completedBy?.takeIf { it.isNotBlank() }?.let { atlasText("Validé par $it", "Approved by $it", "اعتمده $it") },
                ).joinToString(" · ")
                if (footerMeta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = footerMeta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Action controls strictly adapted to role:
        // - `new_product`: ONLY Admins can create & approve (or reject).
        // - Standard role notifications: recipients (or Admins) can mark handled / dismiss.
        val canAct = action.isPending && (isAdmin || (!isProductRequest && !isOwnSubmission))
        if (canAct) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onComplete, enabled = !busy) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isAdmin && isProductRequest) {
                            atlasText("Créer & Valider")
                        } else {
                            atlasText("Marquer traité")
                        },
                    )
                }
                if (isAdmin) {
                    TextButton(onClick = onReject, enabled = !busy) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(atlasText("Refuser"))
                    }
                }
            }
        }
    }
}
