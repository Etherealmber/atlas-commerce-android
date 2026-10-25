package ma.etherealmber.atlascommerce.data.repo

import android.net.Uri
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import ma.etherealmber.atlascommerce.core.RoleTarget
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.format.AtlasFormat
import ma.etherealmber.atlascommerce.core.net.ApiErrorPayload
import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadDetailDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadMessageDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.AssistApproveDto
import ma.etherealmber.atlascommerce.data.model.AssistApproveRequest
import ma.etherealmber.atlascommerce.data.model.AssistChatRequest
import ma.etherealmber.atlascommerce.data.model.AssistChatResponseDto
import ma.etherealmber.atlascommerce.data.model.AssistNextActionsDto
import ma.etherealmber.atlascommerce.data.model.AssistStatusDto
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.CloseProductRequestBody
import ma.etherealmber.atlascommerce.data.model.CompanySettingsDto
import ma.etherealmber.atlascommerce.data.model.CompleteMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.CreateMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.CreateFinanceEntryRequest
import ma.etherealmber.atlascommerce.data.model.FinanceEntryDto
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import ma.etherealmber.atlascommerce.data.model.CreatedInvoiceDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.MobileActionMetaDto
import ma.etherealmber.atlascommerce.data.model.MobileActionStatsDto
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.PutAgentThreadRequest
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.model.RejectMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.StockAdjustRequest
import ma.etherealmber.atlascommerce.data.model.SubmitProductRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of submitting a product: Admins create immediately; all other roles notify Admin immediately. */
sealed interface ProductSubmissionOutcome {
    data class CreatedDirectly(val product: ProductDto) : ProductSubmissionOutcome
    data class SentToAdmin(val summary: String) : ProductSubmissionOutcome
}

@Singleton
class CommerceRepository @Inject constructor(
    private val apiProvider: ApiProvider,
    private val runner: ApiRunner,
    private val offlineCache: OfflineCache,
    private val moshi: Moshi,
) {

    suspend fun dashboard(): ApiResult<DashboardDto> =
        runner.envelope { apiProvider.api().dashboard() }

    suspend fun settings(): ApiResult<CompanySettingsDto> =
        runner.envelope { apiProvider.api().settings() }

    suspend fun clients(): ApiResult<List<ClientDto>> =
        runner.envelope { apiProvider.api().clients() }

    suspend fun invoices(kind: String = "all"): ApiResult<List<InvoiceDto>> =
        runner.envelope { apiProvider.api().invoices(kind) }

    suspend fun financeMonthly(period: String? = null): ApiResult<FinanceMonthlyDto> =
        runner.envelope { apiProvider.api().financeMonthly(period) }

    suspend fun createFinanceEntry(request: CreateFinanceEntryRequest): ApiResult<FinanceEntryDto> =
        runner.envelope { apiProvider.api().createFinanceEntry(request) }

    suspend fun financeStatement(
        period: String,
        page: Int = 1,
        pageSize: Int = 50,
    ): ApiResult<FinanceStatementDto> =
        runner.envelope { apiProvider.api().financeStatement(period, page, pageSize) }

    /** Opens the selected-month combined report using a short-lived HMAC ticket. */
    suspend fun createCombinedReportUrl(period: String): ApiResult<String> {
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) {
            return ApiResult.Failure("BAD_PERIOD", "Période invalide — format attendu AAAA-MM.")
        }
        return when (val result = runner.envelope { apiProvider.api().reportLinkTicket() }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val ticket = result.data
                if (ticket.expiresIn !in 1..1800 ||
                    !ticket.rt.matches(Regex("\\d{9,}\\.[A-Za-z0-9_-]{20,}"))
                ) {
                    ApiResult.Failure("INVALID_REPORT_TICKET", "Le serveur n'a pas fourni de lien temporaire valide.")
                } else {
                    val url = Uri.parse(apiProvider.baseUrl())
                        .buildUpon()
                        .appendPath("api")
                        .appendPath("reports")
                        .appendPath("combined")
                        .appendQueryParameter("rt", ticket.rt)
                        .appendQueryParameter("period", period)
                        .appendQueryParameter("sections", "balance,stock,receivables,payables,employees")
                        .build()
                        .toString()
                    ApiResult.Success(url)
                }
            }
        }
    }

    suspend fun employeePayroll(
        period: String,
        page: Int = 1,
        pageSize: Int = 50,
        dueBefore: String? = null,
    ): ApiResult<PayrollPageDto> = runner.envelope {
        apiProvider.api().employeePayroll(
            period = period,
            page = page,
            pageSize = pageSize,
            dueBefore = dueBefore,
        )
    }

    /** Streams an authenticated PDF response into app-private cache (never a public URL). */
    suspend fun downloadFinanceReport(period: String, destination: File): ApiResult<File> {
        if (!period.matches(Regex("\\d{4}-(0[1-9]|1[0-2])"))) {
            return ApiResult.Failure("BAD_PERIOD", "Période invalide — format attendu AAAA-MM.")
        }
        return try {
            val response = apiProvider.api().financeMonthlyPdf(period)
            if (!response.isSuccessful) {
                val rawError = response.errorBody()?.string()
                val payload = rawError?.let {
                    runCatching { moshi.adapter(ApiErrorPayload::class.java).fromJson(it) }.getOrNull()
                }
                val code = payload?.code ?: "HTTP_${response.code()}"
                val message = if (code == "FIN_FORBIDDEN") {
                    "Réservé aux administrateurs (owner/admin)."
                } else {
                    payload?.error ?: payload?.message ?: "Téléchargement du rapport refusé (${response.code()})."
                }
                ApiResult.Failure(code, message, response.code())
            } else {
                val body = response.body()
                val contentType = body?.contentType()?.let { "${it.type}/${it.subtype}" }
                if (body == null || !contentType.equals("application/pdf", ignoreCase = true)) {
                    body?.close()
                    ApiResult.Failure("INVALID_PDF", "Le serveur n'a pas renvoyé un rapport PDF valide.")
                } else {
                    withContext(Dispatchers.IO) {
                        destination.parentFile?.mkdirs()
                        try {
                            body.byteStream().buffered().use { input ->
                                input.mark(5)
                                val header = ByteArray(5)
                                val headerLength = input.read(header)
                                input.reset()
                                if (headerLength != 5 || !header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) {
                                    destination.delete()
                                    ApiResult.Failure("INVALID_PDF", "Le fichier reçu n'est pas un PDF valide.")
                                } else {
                                    destination.outputStream().buffered().use { output -> input.copyTo(output) }
                                    if (destination.length() < 8L) {
                                        destination.delete()
                                        ApiResult.Failure("INVALID_PDF", "Le rapport PDF reçu est vide ou incomplet.")
                                    } else {
                                        ApiResult.Success(destination)
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            destination.delete()
                            throw t
                        }
                    }
                }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: IOException) {
            ApiResult.Failure(ApiRunner.CODE_NETWORK, "Téléchargement du rapport interrompu. Vérifiez votre connexion et réessayez.")
        } catch (t: Throwable) {
            ApiResult.Failure(ApiRunner.CODE_UNKNOWN, t.message?.takeIf { it.isNotBlank() } ?: "Impossible de télécharger le rapport PDF.")
        }
    }

    suspend fun products(query: String? = null, limit: Int = 300): ApiResult<List<ProductDto>> {
        val result = runner.envelope {
            apiProvider.api().products(query?.takeIf { it.isNotBlank() }, limit)
        }
        if (result is ApiResult.Success && query.isNullOrBlank()) {
            offlineCache.saveProducts(result.data)
        }
        return result
    }

    suspend fun createProduct(request: CreateProductRequest): ApiResult<ProductDto> =
        runner.envelope { apiProvider.api().createProduct(request) }

    /**
     * Authoritative role-gated product submission:
     * - **Admin / Owner (`Roles.canCreateProductDirectly(role)`)**: Creates immediately (`POST /api/products`).
     * - **Non-Admin (`secretaire`, `stock`, `user`)**: NEVER mutates catalogue directly; immediately
     *   dispatches a full Admin notification (`POST /api/mobile-actions` targeted to `admin` with full
     *   `CreateProductRequest` payload) AND registers the stock product request (`POST /api/stock/product-requests`).
     */
    suspend fun submitProductForRole(
        request: CreateProductRequest,
        role: String?,
        username: String?,
        note: String? = null,
    ): ApiResult<ProductSubmissionOutcome> {
        if (Roles.canCreateProductDirectly(role)) {
            return when (val created = createProduct(request)) {
                is ApiResult.Success -> ApiResult.Success(ProductSubmissionOutcome.CreatedDirectly(created.data))
                is ApiResult.Failure -> created
            }
        }

        val sender = username?.takeIf { it.isNotBlank() } ?: "opérateur"
        val roleLabel = Roles.label(role)
        val code = request.barcode?.takeIf { it.isNotBlank() }
            ?: request.sku.ifBlank { "AUTO-${System.currentTimeMillis() % 100_000}" }
        val detailParts = buildList {
            add("Demandé par $sender ($roleLabel)")
            if (request.sku.isNotBlank()) add("Réf: ${request.sku}")
            if (!request.barcode.isNullOrBlank()) add("Code: ${request.barcode}")
            add("Prix vente: ${request.price} DH")
            if (request.stockQuantity > 0.0) add("Qté initiale: ${request.stockQuantity} ${request.unit}")
            if (!note.isNullOrBlank()) add("Note: ${note.trim()}")
        }
        val detailText = detailParts.joinToString(" · ")

        val actionRes = createMobileAction(
            CreateMobileActionRequest(
                type = "new_product",
                title = "Nouveau produit à créer : ${request.name}",
                detail = detailText,
                barcode = request.barcode?.takeIf { it.isNotBlank() } ?: code,
                createdBy = sender,
                meta = MobileActionMetaDto(
                    targetRole = RoleTarget.ADMIN.key,
                    senderRole = Roles.normalize(role),
                    priority = "high",
                    category = "product_creation_request",
                    proposedStock = request.stockQuantity,
                    createProductBody = request,
                ),
            ),
        )

        val stockReqRes = submitProductRequest(
            SubmitProductRequestBody(
                code = code,
                sku = request.sku.takeIf { it.isNotBlank() },
                name = request.name,
                category = request.category,
                unit = request.unit.ifBlank { "unité" },
                quantity = if (request.stockQuantity > 0.0) request.stockQuantity else 1.0,
                note = detailText,
            ),
        )

        return if (actionRes is ApiResult.Success || stockReqRes is ApiResult.Success) {
            ApiResult.Success(
                ProductSubmissionOutcome.SentToAdmin(
                    "Demande envoyée immédiatement à l'Administrateur pour « ${request.name} ».",
                ),
            )
        } else {
            (actionRes as? ApiResult.Failure) ?: (stockReqRes as ApiResult.Failure)
        }
    }

    /** Sends a real cross-device notification targeted to a specific role group. */
    suspend fun sendRoleNotification(
        senderUsername: String,
        senderRole: String?,
        target: RoleTarget,
        title: String,
        message: String,
        urgent: Boolean = false,
        barcode: String? = null,
    ): ApiResult<Unit> {
        // Worker RBAC permits 'scan', 'stock_check', and 'new_product' for every authenticated role;
        // 'invoice' is restricted to sales roles, so we always pick a role-safe channel type.
        val wireType = when (target) {
            RoleTarget.ADMIN -> "new_product"
            RoleTarget.STOCK -> "stock_check"
            RoleTarget.SECRETARY, RoleTarget.ALL -> "scan"
        }
        val cleanTitle = if (urgent && !title.startsWith("[URGENT]", ignoreCase = true)) {
            "[URGENT] ${title.trim()}"
        } else {
            title.trim()
        }
        val senderLabel = Roles.label(senderRole)
        val fullDetail = buildString {
            append("De $senderUsername ($senderLabel) → ${target.label}")
            if (message.isNotBlank()) {
                append("\n")
                append(message.trim())
            }
        }
        return createMobileAction(
            CreateMobileActionRequest(
                type = wireType,
                title = cleanTitle,
                detail = fullDetail,
                barcode = barcode?.takeIf { it.isNotBlank() },
                createdBy = senderUsername.ifBlank { "mobile" },
                meta = MobileActionMetaDto(
                    targetRole = target.key,
                    senderRole = Roles.normalize(senderRole),
                    priority = if (urgent) "urgent" else "normal",
                    category = "role_notification",
                ),
            ),
        )
    }

    /** Parses structured `MobileActionMetaDto` stored in `meta_json`, if present. */
    fun parseActionMeta(action: MobileActionDto): MobileActionMetaDto? {
        val raw = action.metaJson?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            moshi.adapter(MobileActionMetaDto::class.java).fromJson(raw)
        }.getOrNull()
    }

    /**
     * Admin 1-tap approval on a staff product request inside `MobileActionDto`:
     * creates the real product if `createProductBody` is present, then marks the action completed.
     */
    suspend fun approveActionAsAdmin(
        action: MobileActionDto,
        adminUsername: String,
    ): ApiResult<ProductDto?> {
        val meta = parseActionMeta(action)
        val pendingProduct = meta?.createProductBody
        var createdProduct: ProductDto? = null
        if (pendingProduct != null && action.productId == null) {
            when (val prodRes = createProduct(pendingProduct)) {
                is ApiResult.Success -> createdProduct = prodRes.data
                is ApiResult.Failure -> return prodRes
            }
        }
        return when (
            val completeRes = completeMobileAction(
                id = action.id,
                applyStock = true,
                completedBy = adminUsername,
                productId = createdProduct?.id ?: action.productId,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(createdProduct)
            is ApiResult.Failure -> completeRes
        }
    }

    suspend fun productRequests(status: String = "open"): ApiResult<List<ProductCreationRequestDto>> =
        runner.envelope { apiProvider.api().productRequests(status) }

    suspend fun submitProductRequest(body: SubmitProductRequestBody): ApiResult<ProductCreationRequestDto> =
        runner.envelope { apiProvider.api().createProductRequest(body) }

    suspend fun closeProductRequest(
        id: Long,
        status: String = "done",
        productId: Long? = null,
    ): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().closeProductRequest(
                id = id,
                body = CloseProductRequestBody(status = status, productId = productId),
            )
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    /** Exact barcode/SKU lookup used by the scanner. */
    suspend fun scan(code: String): ApiResult<ProductDto> =
        runner.envelope { apiProvider.api().scanBarcode(code) }

    /** Manual stock movement: `type` = "in" | "out" | "adjustment". */
    suspend fun adjustStock(
        productId: Long,
        quantity: Double,
        type: String,
        reason: String?,
    ): ApiResult<ProductDto> = runner.envelope {
        apiProvider.api().adjustStock(
            productId,
            StockAdjustRequest(quantity = quantity, type = type, reason = reason?.takeIf { it.isNotBlank() }),
        )
    }

    suspend fun alerts(): ApiResult<List<AlertItemDto>> =
        runner.envelope { apiProvider.api().alerts() }

    suspend fun mobileActions(status: String = "all"): ApiResult<List<MobileActionDto>> =
        runner.envelope { apiProvider.api().mobileActions(status = status) }

    suspend fun mobileActionStats(): ApiResult<MobileActionStatsDto> =
        runner.envelope { apiProvider.api().mobileActionStats() }

    suspend fun createMobileAction(body: CreateMobileActionRequest): ApiResult<Unit> {
        val res = runner.status { apiProvider.api().createMobileAction(body) }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun completeMobileAction(
        id: Long,
        applyStock: Boolean = true,
        completedBy: String? = null,
        productId: Long? = null,
    ): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().completeMobileAction(
                id,
                CompleteMobileActionRequest(
                    applyStock = applyStock,
                    completedBy = completedBy?.takeIf { it.isNotBlank() },
                    productId = productId,
                ),
            )
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun rejectMobileAction(id: Long, reason: String): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().rejectMobileAction(id, RejectMobileActionRequest(reason))
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun agentPolicy(): ApiResult<AgentPolicyDto> =
        runner.envelope { apiProvider.api().agentPolicy() }

    suspend fun listAgentThreads(): ApiResult<List<AgentThreadSummaryDto>> =
        runner.envelope { apiProvider.api().listAgentThreads() }

    suspend fun getAgentThread(id: String): ApiResult<AgentThreadDetailDto> =
        runner.envelope { apiProvider.api().getAgentThread(id) }

    suspend fun putAgentThread(
        id: String,
        title: String,
        messages: List<AgentThreadMessageDto>,
    ): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().putAgentThread(
                id = id,
                body = PutAgentThreadRequest(
                    title = title.trim().ifBlank { "Discussion" }.take(48),
                    messages = messages.takeLast(40),
                ),
            )
        }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun deleteAgentThread(id: String): ApiResult<Unit> {
        val res = runner.status { apiProvider.api().deleteAgentThread(id) }
        return when (res) {
            is ApiResult.Failure -> res
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    suspend fun assistStatus(): ApiResult<AssistStatusDto> =
        runner.envelope { apiProvider.api().assistStatus() }

    suspend fun assistNextActions(): ApiResult<AssistNextActionsDto> =
        runner.envelope { apiProvider.api().assistNextActions() }

    /**
     * Sends a prompt to `/api/assist/chat` grounded on real-time live D1 metrics,
     * products, stock levels, invoices, and pending actions so the LLM NEVER
     * hallucinates random figures when asked about the business on mobile.
     */
    suspend fun assistChat(
        message: String,
        role: String? = Roles.USER,
        username: String? = null,
        threadId: String = "live",
        images: List<String> = emptyList(),
    ): ApiResult<AssistChatResponseDto> {
        val cleanQuestion = message.trim()
        val isGreeting = Regex(
            "^(bonjour|bonsoir|salut|hello|hi|hey|coucou|salam|bonne\\s*(journée|soirée)|merci)[\\s!.?]*$",
            RegexOption.IGNORE_CASE,
        ).matches(cleanQuestion)

        val outboundPrompt = if (isGreeting) {
            cleanQuestion
        } else {
            val snapshot = runCatching { buildLiveBusinessSnapshot(cleanQuestion, role, username) }.getOrDefault("")
            if (snapshot.isBlank()) {
                cleanQuestion
            } else {
                buildString {
                    appendLine("RÈGLE STRICTE : Appuie-toi UNIQUEMENT sur les données réelles en direct de la boutique ci-dessous. N'invente JAMAIS aucun chiffre, stock, prix, facture ou client.")
                    appendLine(snapshot)
                    appendLine("Réponds clairement et naturellement en français (tableaux ou listes claires si pertinent) sans mentionner ce bloc technique.")
                    append("Question de l'utilisateur : $cleanQuestion")
                }.take(2450)
            }
        }

        return runner.envelope {
            apiProvider.api().assistChat(
                AssistChatRequest(
                    message = outboundPrompt,
                    userMessage = cleanQuestion,
                    threadId = threadId,
                    images = images.take(4),
                ),
            )
        }
    }

    private suspend fun buildLiveBusinessSnapshot(userQuestion: String, role: String?, username: String?): String = coroutineScope {
        val isAdmin = Roles.isAdmin(role)
        val canViewClientsAndInvoices = Roles.canCreateDocuments(role)
        val dashDef = async { runCatching { dashboard() }.getOrNull() }
        val financeDef = if (isAdmin) async { runCatching { financeMonthly() }.getOrNull() } else null
        val prodsDef = async { runCatching { products(query = null, limit = 250) }.getOrNull() }
        val clientsDef = if (canViewClientsAndInvoices) async { runCatching { clients() }.getOrNull() } else null
        val actsDef = async { runCatching { mobileActions(status = "pending") }.getOrNull() }
        val alertsDef = async { runCatching { alerts() }.getOrNull() }

        val dash = (dashDef.await() as? ApiResult.Success)?.data
        val finance = (financeDef?.await() as? ApiResult.Success)?.data
        val prods = (prodsDef.await() as? ApiResult.Success)?.data
            ?: runCatching { cachedProducts() }.getOrDefault(emptyList())
        val cliList = (clientsDef?.await() as? ApiResult.Success)?.data.orEmpty()
        val pendingActions = (actsDef.await() as? ApiResult.Success)?.data.orEmpty()
            .filter { Roles.isActionVisibleForUser(role, username, it) }
        val activeAlerts = Roles.filterAlertsForRole(
            role,
            (alertsDef.await() as? ApiResult.Success)?.data.orEmpty(),
        )

        val cur = finance?.currency?.takeIf { it.isNotBlank() } ?: dash?.activeCurrency ?: "MAD"
        val sb = StringBuilder()
        sb.appendLine("[DONNÉES TEMPS RÉEL ATLAS COMMERCE]")
        if (dash != null) {
            val permittedKpis = buildList {
                add("Produits=${dash.productCount}")
                add("StockBas=${dash.lowStockCount}")
                if (canViewClientsAndInvoices) add("Clients=${cliList.size}")
                if (isAdmin && finance != null) {
                    add("SoldeClôture=${AtlasFormat.money(finance.closingBalance, finance.currency)}")
                    add("VentesEncaissées=${AtlasFormat.money(finance.salesReceived, finance.currency)}")
                    add("Dépenses=${AtlasFormat.money(finance.expenses, finance.currency)}")
                    add("ÀRecevoir=${AtlasFormat.money(finance.receivables.totalOutstanding, finance.currency)}")
                    add("ÀPayerFournisseurs=${AtlasFormat.money(finance.payables.totalOutstanding, finance.currency)}")
                    add("PaieÀVerser=${AtlasFormat.money(finance.payroll.totalDue, finance.currency)}")
                }
            }
            sb.appendLine("KPIs : ${permittedKpis.joinToString()}")
            if (isAdmin && finance != null && dash.monthlySeries.isNotEmpty()) {
                sb.appendLine(
                    "Mensuel : " + dash.monthlySeries.takeLast(3).joinToString(" | ") { m ->
                        "${m.label.ifBlank { m.key }}: ventes=${AtlasFormat.money(m.sales, cur)}, dépenses=${AtlasFormat.money(m.expenses, cur)}, profit=${AtlasFormat.money(m.profit, cur)}"
                    },
                )
            }
            if (canViewClientsAndInvoices && dash.unpaidInvoiceList.isNotEmpty()) {
                sb.appendLine(
                    "FacturesImpayées : " + dash.unpaidInvoiceList.take(5).joinToString(" | ") { inv ->
                        "${inv.number ?: "#${inv.id}"} (${inv.clientName ?: "Client"}: reste=${AtlasFormat.money(inv.remaining, cur)})"
                    },
                )
            }
            if (canViewClientsAndInvoices && dash.recentInvoices.isNotEmpty()) {
                sb.appendLine(
                    "FacturesRécentes : " + dash.recentInvoices.take(5).joinToString(" | ") { inv ->
                        "${inv.number ?: "#${inv.id}"} (${inv.clientName ?: "Client"}: ${AtlasFormat.money(inv.total, cur)}, ${inv.status ?: "-"})"
                    },
                )
            }
        }
        val tokens = userQuestion.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 }
        if (canViewClientsAndInvoices && cliList.isNotEmpty() && tokens.isNotEmpty()) {
            val matchedClients = cliList.filter { c ->
                val hay = "${c.name} ${c.phone.orEmpty()} ${c.ice.orEmpty()}".lowercase()
                tokens.any { hay.contains(it) }
            }.take(5)
            if (matchedClients.isNotEmpty()) {
                sb.appendLine(
                    "ClientsCorrespondants : " + matchedClients.joinToString(" | ") { c ->
                        "${c.name} (tél:${c.phone ?: "-"}, soldeDû:${AtlasFormat.money(c.balanceDue, cur)})"
                    },
                )
            }
        }
        if (prods.isNotEmpty()) {
            val matched = if (tokens.isNotEmpty()) {
                prods.filter { p ->
                    val hay = "${p.name} ${p.sku.orEmpty()} ${p.barcode.orEmpty()} ${p.category.orEmpty()}".lowercase()
                    tokens.any { tok -> hay.contains(tok) }
                }.take(8)
            } else {
                emptyList()
            }
            if (matched.isNotEmpty()) {
                sb.appendLine(
                    "ProduitsCorrespondants : " + matched.joinToString(" | ") { p ->
                        "${p.name} [SKU:${p.sku ?: "-"}, Qté:${AtlasFormat.quantity(p.stock)}, Prix:${AtlasFormat.money(p.price, cur)}]"
                    },
                )
            }
            val lowOrOut = prods.filter { it.isOutOfStock || it.isLowStock }.sortedBy { it.stock }.take(7)
            if (lowOrOut.isNotEmpty()) {
                sb.appendLine(
                    "RupturesEtStockBas : " + lowOrOut.joinToString(" | ") { p ->
                        "${p.name} (Qté:${AtlasFormat.quantity(p.stock)} / seuil:${AtlasFormat.quantity(p.lowStockThreshold)}, ${AtlasFormat.money(p.price, cur)})"
                    },
                )
            } else {
                sb.appendLine("CatalogueAperçu (${prods.size} réf.) : " + prods.take(5).joinToString(" | ") { p ->
                    "${p.name} (Qté:${AtlasFormat.quantity(p.stock)}, ${AtlasFormat.money(p.price, cur)})"
                })
            }
        }
        if (pendingActions.isNotEmpty()) {
            sb.appendLine(
                "DemandesEnAttente (${pendingActions.size}) : " + pendingActions.take(5).joinToString(" | ") { a ->
                    "#${a.id} ${a.title} (${a.type}, par ${a.createdBy ?: "-"})"
                },
            )
        }
        if (activeAlerts.isNotEmpty()) {
            sb.appendLine(
                "Alertes : " + activeAlerts.take(4).joinToString(" | ") { al -> al.label },
            )
        }
        sb.toString().take(1850)
    }

    suspend fun assistApprove(proposalId: String): ApiResult<AssistApproveDto> =
        runner.envelope { apiProvider.api().assistApprove(AssistApproveRequest(proposalId)) }

    suspend fun createQuickSale(
        request: QuickSaleRequest,
        idempotencyKey: String,
    ): ApiResult<CreatedInvoiceDto> =
        runner.envelope { apiProvider.api().createInvoice(idempotencyKey, request) }

    suspend fun cachedProducts(): List<ProductDto> = offlineCache.readProducts()
}
