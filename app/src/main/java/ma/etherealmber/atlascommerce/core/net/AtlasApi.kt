package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
import ma.etherealmber.atlascommerce.data.model.ClientDto
import ma.etherealmber.atlascommerce.data.model.CompanySettingsDto
import ma.etherealmber.atlascommerce.data.model.CreatedInvoiceDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.LoginRequest
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import ma.etherealmber.atlascommerce.data.model.OtpRequest
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.model.StockAdjustRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Atlas worker REST surface used by the Android app.
 *
 * Paths are relative: the Retrofit instance is rebuilt whenever the user
 * changes the server, so a shop can point the app at its own deployment.
 */
interface AtlasApi {

    @GET("api/health")
    suspend fun health(): ApiEnvelope<HealthDto>

    @GET("api/app/android-update")
    suspend fun androidUpdate(
        @Query("versionCode") versionCode: Int,
        @Query("versionName") versionName: String,
    ): AndroidUpdateDto

    @GET("api/auth/me")
    suspend fun me(): ApiEnvelope<MeDto>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: OtpRequest): LoginResponse

    @GET("api/dashboard")
    suspend fun dashboard(): ApiEnvelope<DashboardDto>

    @GET("api/settings")
    suspend fun settings(): ApiEnvelope<CompanySettingsDto>

    /**
     * Catalogue query.
     *
     * `fields=mobile` asks the worker for its phone-shaped projection: the same
     * columns the app uses, minus images and heavy joins, and without the schema
     * checks the desk client needs. It is markedly faster on mobile networks.
     */
    @GET("api/products")
    suspend fun products(
        @Query("q") query: String?,
        @Query("limit") limit: Int = 300,
        @Query("fields") fields: String = "mobile",
    ): ApiEnvelope<List<ProductDto>>

    /**
     * Exact barcode / SKU lookup, the worker's dedicated scan hot path.
     * Returns 404 with `code = NOT_FOUND` (unknown code) or `PENDING_APPROVAL`
     * (already proposed from this phone, waiting on the desktop).
     */
    /** Manual stock movement (entrée / sortie), audited server-side. */
    @POST("api/products/{id}/adjust-stock")
    suspend fun adjustStock(
        @Path("id") id: Long,
        @Body body: StockAdjustRequest,
    ): ApiEnvelope<ProductDto>

    @GET("api/scan/barcode/{code}")
    suspend fun scanBarcode(@Path("code") code: String): ApiEnvelope<ProductDto>

    @GET("api/clients")
    suspend fun clients(): ApiEnvelope<List<ClientDto>>

    @GET("api/invoices")
    suspend fun invoices(@Query("kind") kind: String = "all"): ApiEnvelope<List<InvoiceDto>>

    @POST("api/invoices")
    suspend fun createInvoice(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickSaleRequest,
    ): ApiEnvelope<CreatedInvoiceDto>
}
