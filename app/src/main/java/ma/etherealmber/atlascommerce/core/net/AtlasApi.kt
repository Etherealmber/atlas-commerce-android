package ma.etherealmber.atlascommerce.core.net

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
import retrofit2.http.Body
import retrofit2.http.GET
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

    @GET("api/products")
    suspend fun products(
        @Query("q") query: String?,
        @Query("limit") limit: Int = 200,
    ): ApiEnvelope<List<ProductDto>>

    @GET("api/clients")
    suspend fun clients(): ApiEnvelope<List<ClientDto>>

    @GET("api/invoices")
    suspend fun invoices(@Query("kind") kind: String = "all"): ApiEnvelope<List<InvoiceDto>>

    @POST("api/invoices")
    suspend fun createInvoice(@Body body: QuickSaleRequest): ApiEnvelope<CreatedInvoiceDto>
}
