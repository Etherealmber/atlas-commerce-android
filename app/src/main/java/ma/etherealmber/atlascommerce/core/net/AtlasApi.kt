package ma.etherealmber.atlascommerce.core.net

import ma.etherealmber.atlascommerce.data.model.ActionAckDto
import ma.etherealmber.atlascommerce.data.model.AgentPolicyDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadDetailDto
import ma.etherealmber.atlascommerce.data.model.AgentThreadSummaryDto
import ma.etherealmber.atlascommerce.data.model.AlertItemDto
import ma.etherealmber.atlascommerce.data.model.AndroidUpdateDto
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
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.LoginRequest
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.MobileActionStatsDto
import ma.etherealmber.atlascommerce.data.model.OtpRequest
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.ProfileUpdateRequest
import ma.etherealmber.atlascommerce.data.model.PutAgentThreadRequest
import ma.etherealmber.atlascommerce.data.model.QrClaimRequest
import ma.etherealmber.atlascommerce.data.model.QrCompleteDto
import ma.etherealmber.atlascommerce.data.model.QrCompleteRequest
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.model.RejectMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.RevokeDeviceRequest
import ma.etherealmber.atlascommerce.data.model.StockAdjustRequest
import ma.etherealmber.atlascommerce.data.model.SubmitProductRequestBody
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.model.VerifyLinkRequest
import ma.etherealmber.atlascommerce.push.DeviceTokenAckDto
import ma.etherealmber.atlascommerce.push.DeviceTokenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Streaming
import retrofit2.http.Path
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

    // --- Push (FCM) device-token registration ---
    @POST("api/assist/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ApiEnvelope<DeviceTokenAckDto>

    @GET("api/app/android-update")
    suspend fun androidUpdate(
        @Query("versionCode") versionCode: Int,
        @Query("versionName") versionName: String,
    ): AndroidUpdateDto

    // --- Auth, profile & connected devices / QR pairing ---

    @GET("api/auth/me")
    suspend fun me(): ApiEnvelope<MeDto>

    @PATCH("api/auth/me")
    suspend fun updateMe(@Body body: ProfileUpdateRequest): ActionAckDto

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: OtpRequest): LoginResponse

    @POST("api/auth/verify-link")
    suspend fun verifyLink(@Body body: VerifyLinkRequest): LoginResponse

    @GET("api/auth/devices")
    suspend fun devices(): ApiEnvelope<List<TrustedDeviceDto>>

    @POST("api/auth/devices/revoke")
    suspend fun revokeDevice(@Body body: RevokeDeviceRequest): ActionAckDto

    @GET("api/auth/qr/session/{token}")
    suspend fun qrStatus(@Path("token") token: String): ApiEnvelope<QrSessionStatusDto>

    @POST("api/auth/qr/session/{token}/claim")
    suspend fun qrClaim(
        @Path("token") token: String,
        @Body body: QrClaimRequest,
    ): ApiEnvelope<QrSessionStatusDto>

    @POST("api/auth/qr/session/{token}/phone-bind")
    suspend fun qrPhoneBind(
        @Path("token") token: String,
        @Body body: QrClaimRequest,
    ): ApiEnvelope<QrSessionStatusDto>

    @POST("api/auth/qr/session/{token}/deny")
    suspend fun qrDeny(@Path("token") token: String): ApiEnvelope<QrSessionStatusDto>

    @POST("api/auth/qr/session/{token}/complete")
    suspend fun qrComplete(
        @Path("token") token: String,
        @Body body: QrCompleteRequest,
    ): ApiEnvelope<QrCompleteDto>

    // --- Core commerce ---

    @GET("api/dashboard")
    suspend fun dashboard(): ApiEnvelope<DashboardDto>

    @GET("api/settings")
    suspend fun settings(): ApiEnvelope<CompanySettingsDto>

    @GET("api/products")
    suspend fun products(
        @Query("q") query: String?,
        @Query("limit") limit: Int = 300,
        @Query("fields") fields: String = "mobile",
    ): ApiEnvelope<List<ProductDto>>

    @POST("api/products")
    suspend fun createProduct(@Body body: CreateProductRequest): ApiEnvelope<ProductDto>

    @POST("api/products/{id}/adjust-stock")
    suspend fun adjustStock(
        @Path("id") id: Long,
        @Body body: StockAdjustRequest,
    ): ApiEnvelope<ProductDto>

    @GET("api/scan/barcode/{code}")
    suspend fun scanBarcode(@Path("code") code: String): ApiEnvelope<ProductDto>

    @GET("api/stock/product-requests")
    suspend fun productRequests(
        @Query("status") status: String = "open",
    ): ApiEnvelope<List<ProductCreationRequestDto>>

    @POST("api/stock/product-requests")
    suspend fun createProductRequest(
        @Body body: SubmitProductRequestBody,
    ): ApiEnvelope<ProductCreationRequestDto>

    @POST("api/stock/product-requests/{id}/close")
    suspend fun closeProductRequest(
        @Path("id") id: Long,
        @Body body: CloseProductRequestBody,
    ): ActionAckDto

    // --- Notifications, alerts & mobile actions ---

    @GET("api/alerts")
    suspend fun alerts(): ApiEnvelope<List<AlertItemDto>>

    @GET("api/mobile-actions")
    suspend fun mobileActions(
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 40,
        @Query("compact") compact: Int = 1,
    ): ApiEnvelope<List<MobileActionDto>>

    @GET("api/mobile-actions/stats")
    suspend fun mobileActionStats(): ApiEnvelope<MobileActionStatsDto>

    @POST("api/mobile-actions")
    suspend fun createMobileAction(@Body body: CreateMobileActionRequest): ActionAckDto

    @POST("api/mobile-actions/{id}/complete")
    suspend fun completeMobileAction(
        @Path("id") id: Long,
        @Body body: CompleteMobileActionRequest,
    ): ActionAckDto

    @POST("api/mobile-actions/{id}/reject")
    suspend fun rejectMobileAction(
        @Path("id") id: Long,
        @Body body: RejectMobileActionRequest,
    ): ActionAckDto

    // --- Atlas Agent IA ---

    @GET("api/agent/policy")
    suspend fun agentPolicy(): ApiEnvelope<AgentPolicyDto>

    @GET("api/admin/agent/threads")
    suspend fun listAgentThreads(): ApiEnvelope<List<AgentThreadSummaryDto>>

    @GET("api/admin/agent/threads/{id}")
    suspend fun getAgentThread(@Path("id") id: String): ApiEnvelope<AgentThreadDetailDto>

    @PUT("api/admin/agent/threads/{id}")
    suspend fun putAgentThread(
        @Path("id") id: String,
        @Body body: PutAgentThreadRequest,
    ): ActionAckDto

    @DELETE("api/admin/agent/threads/{id}")
    suspend fun deleteAgentThread(@Path("id") id: String): ActionAckDto

    @GET("api/assist/status")
    suspend fun assistStatus(): ApiEnvelope<AssistStatusDto>

    @GET("api/assist/next-actions")
    suspend fun assistNextActions(): ApiEnvelope<AssistNextActionsDto>

    @POST("api/assist/chat")
    suspend fun assistChat(@Body body: AssistChatRequest): ApiEnvelope<AssistChatResponseDto>

    @POST("api/assist/approve")
    suspend fun assistApprove(@Body body: AssistApproveRequest): ApiEnvelope<AssistApproveDto>

    // --- Clients & Quick Sales ---

    @GET("api/clients")
    suspend fun clients(): ApiEnvelope<List<ClientDto>>

    // Admin-only finance APIs: the worker re-validates the active owner/admin role.
    @GET("api/admin/finance/monthly")
    suspend fun financeMonthly(
        @Query("period") period: String? = null,
    ): ApiEnvelope<FinanceMonthlyDto>

    @GET("api/admin/employees/payroll")
    suspend fun employeePayroll(
        @Query("period") period: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("due_before") dueBefore: String? = null,
    ): ApiEnvelope<PayrollPageDto>

    @Streaming
    @GET("api/admin/finance/monthly/report.pdf")
    suspend fun financeMonthlyPdf(
        @Query("period") period: String,
    ): Response<ResponseBody>

    @GET("api/invoices")
    suspend fun invoices(@Query("kind") kind: String = "all"): ApiEnvelope<List<InvoiceDto>>

    @POST("api/invoices")
    suspend fun createInvoice(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickSaleRequest,
    ): ApiEnvelope<CreatedInvoiceDto>
}
