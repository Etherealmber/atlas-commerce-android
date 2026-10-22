package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.OfflineCache
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
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import ma.etherealmber.atlascommerce.data.model.CreatedInvoiceDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.MobileActionStatsDto
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.model.RejectMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.StockAdjustRequest
import ma.etherealmber.atlascommerce.data.model.SubmitProductRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommerceRepository @Inject constructor(
    private val apiProvider: ApiProvider,
    private val runner: ApiRunner,
    private val offlineCache: OfflineCache,
) {

    suspend fun dashboard(): ApiResult<DashboardDto> =
        runner.envelope { apiProvider.api().dashboard() }

    suspend fun settings(): ApiResult<CompanySettingsDto> =
        runner.envelope { apiProvider.api().settings() }

    suspend fun clients(): ApiResult<List<ClientDto>> =
        runner.envelope { apiProvider.api().clients() }

    suspend fun invoices(kind: String = "all"): ApiResult<List<InvoiceDto>> =
        runner.envelope { apiProvider.api().invoices(kind) }

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

    suspend fun completeMobileAction(id: Long, applyStock: Boolean = true): ApiResult<Unit> {
        val res = runner.status {
            apiProvider.api().completeMobileAction(id, CompleteMobileActionRequest(applyStock))
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

    suspend fun assistStatus(): ApiResult<AssistStatusDto> =
        runner.envelope { apiProvider.api().assistStatus() }

    suspend fun assistNextActions(): ApiResult<AssistNextActionsDto> =
        runner.envelope { apiProvider.api().assistNextActions() }

    suspend fun assistChat(
        message: String,
        threadId: String = "mobile-live",
    ): ApiResult<AssistChatResponseDto> = runner.envelope {
        apiProvider.api().assistChat(
            AssistChatRequest(
                message = message,
                userMessage = message,
                threadId = threadId,
            ),
        )
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
