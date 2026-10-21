package ma.etherealmber.atlascommerce.data.repo

import ma.etherealmber.atlascommerce.core.net.ApiProvider
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.net.ApiRunner
import ma.etherealmber.atlascommerce.data.OfflineCache
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.CompanySettingsDto
import ma.etherealmber.atlascommerce.data.model.CreatedInvoiceDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
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

    /** Exact barcode/SKU lookup used by the scanner. */
    suspend fun scan(code: String): ApiResult<ProductDto> =
        runner.envelope { apiProvider.api().scanBarcode(code) }

    suspend fun createQuickSale(request: QuickSaleRequest): ApiResult<CreatedInvoiceDto> =
        runner.envelope { apiProvider.api().createInvoice(request) }

    /** Last catalogue seen while online — used when the network is unavailable. */
    fun cachedProducts(): List<ProductDto> = offlineCache.readProducts()
}
