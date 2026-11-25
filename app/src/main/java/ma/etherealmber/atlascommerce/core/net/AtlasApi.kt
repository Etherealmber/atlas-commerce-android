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
import ma.etherealmber.atlascommerce.data.model.CreateFinanceEntryRequest
import ma.etherealmber.atlascommerce.data.model.FinanceEntryDto
import ma.etherealmber.atlascommerce.data.model.DashboardDto
import ma.etherealmber.atlascommerce.data.model.DailyMetricsDto
import ma.etherealmber.atlascommerce.data.model.HealthDto
import ma.etherealmber.atlascommerce.data.model.InvoiceDto
import ma.etherealmber.atlascommerce.data.model.FinanceMonthlyDto
import ma.etherealmber.atlascommerce.data.model.EmployeeDto
import ma.etherealmber.atlascommerce.data.model.EmployeeUpdateRequest
import ma.etherealmber.atlascommerce.data.model.EmployeeResignRequest
import ma.etherealmber.atlascommerce.data.model.EmployeePayrollActionRequest
import ma.etherealmber.atlascommerce.data.model.ExpenseDeleteRequest
import ma.etherealmber.atlascommerce.data.model.ProductDeleteRequest
import ma.etherealmber.atlascommerce.data.model.FinancePeriodDto
import ma.etherealmber.atlascommerce.data.model.FinanceStatementDto
import ma.etherealmber.atlascommerce.data.model.PayrollPageDto
import ma.etherealmber.atlascommerce.data.model.LoginRequest
import ma.etherealmber.atlascommerce.data.model.LoginResponse
import ma.etherealmber.atlascommerce.data.model.MeDto
import ma.etherealmber.atlascommerce.data.model.MobileActionDto
import ma.etherealmber.atlascommerce.data.model.MobileActionStatsDto
import ma.etherealmber.atlascommerce.data.model.MobileWorkStatusDto
import ma.etherealmber.atlascommerce.data.model.OtpRequest
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.ProductUnitDto
import ma.etherealmber.atlascommerce.data.model.UpdateProductRequest
import ma.etherealmber.atlascommerce.data.model.ProductImageDto
import ma.etherealmber.atlascommerce.data.model.ProductImageUploadRequest
import ma.etherealmber.atlascommerce.data.model.ProductMovementDto
import ma.etherealmber.atlascommerce.data.model.ProfileUpdateRequest
import ma.etherealmber.atlascommerce.data.model.PutAgentThreadRequest
import ma.etherealmber.atlascommerce.data.model.QrClaimRequest
import ma.etherealmber.atlascommerce.data.model.QrCompleteDto
import ma.etherealmber.atlascommerce.data.model.QrCompleteRequest
import ma.etherealmber.atlascommerce.data.model.QrSessionStatusDto
import ma.etherealmber.atlascommerce.data.model.QuickSaleRequest
import ma.etherealmber.atlascommerce.data.model.RejectMobileActionRequest
import ma.etherealmber.atlascommerce.data.model.RevokeDeviceRequest
import ma.etherealmber.atlascommerce.data.model.StartupRoleRefreshRequest
import ma.etherealmber.atlascommerce.data.model.StartupRoleRefreshDto
import ma.etherealmber.atlascommerce.data.model.StockAdjustRequest
import ma.etherealmber.atlascommerce.data.model.StockAdjustmentRequestBody
import ma.etherealmber.atlascommerce.data.model.StockAdjustmentRequestDto
import ma.etherealmber.atlascommerce.data.model.SnakePersonalBestDto
import ma.etherealmber.atlascommerce.data.model.SubmitProductRequestBody
import ma.etherealmber.atlascommerce.data.model.TrustedDeviceDto
import ma.etherealmber.atlascommerce.data.model.UserReportRequest
import ma.etherealmber.atlascommerce.data.model.UserReportReceiptDto
import ma.etherealmber.atlascommerce.data.model.VerifyLinkRequest
import ma.etherealmber.atlascommerce.push.DeviceTokenAckDto
import ma.etherealmber.atlascommerce.push.DeviceTokenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
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

    @Headers("Cache-Control: no-store")
    @GET("api/mobile/preferences")
    suspend fun getMobilePreferences(): ma.etherealmber.atlascommerce.data.model.MobilePreferencesDto

    @Headers("Cache-Control: no-store")
    @PUT("api/mobile/preferences")
    suspend fun putMobilePreferences(
        @Body body: ma.etherealmber.atlascommerce.data.model.MobilePreferencesPutRequest,
    ): ma.etherealmber.atlascommerce.data.model.MobilePreferencesDto

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

    /** Legacy v2 role refresh is invoked only after the exact HTTP 409 / ROLE_CHANGED response. */
    @POST("api/auth/role-refresh")
    suspend fun refreshLegacyRole(@Body body: StartupRoleRefreshRequest): ApiEnvelope<StartupRoleRefreshDto>

    /** Read-only status for the signed-in account; no tracker credentials are sent by the app. */
    @GET("api/mobile/work-status")
    suspend fun mobileWorkStatus(
        @Query("in_app_cursor") inAppCursor: String? = null,
        @Query("github_cursor") githubCursor: String? = null,
    ): MobileWorkStatusDto

    /** Private per-account Snake record; the Worker returns this resource unwrapped. */
    @GET("api/mobile/snake-score")
    suspend fun getSnakePersonalBest(@Header("Authorization") authorization: String): SnakePersonalBestDto

    /** Monotonic max write; request and response use the same unwrapped one-field shape. */
    @PUT("api/mobile/snake-score")
    suspend fun putSnakePersonalBest(
        @Header("Authorization") authorization: String,
        @Body body: SnakePersonalBestDto,
    ): SnakePersonalBestDto

    /** Authenticated user report; GitHub delivery is handled by the Worker (never by the app). */
    @POST("api/reports")
    suspend fun submitUserReport(@Body body: UserReportRequest): ApiEnvelope<UserReportReceiptDto>

    @PATCH("api/auth/me")
    suspend fun updateMe(@Body body: ProfileUpdateRequest): ActionAckDto

    /** v0.7.5 — voluntary logout marks this device resumable for password-only return. */
    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String? = null): ActionAckDto

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

    // Desktop native-auth v2 phone approval is intentionally separate from legacy QR routes.
    @POST("api/auth/desktop/v2/phone/prepare")
    suspend fun desktopPhonePrepare(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: ma.etherealmber.atlascommerce.data.model.DesktopPhonePrepareRequest,
    ): ApiEnvelope<ma.etherealmber.atlascommerce.data.model.DesktopPhonePrepareDto>

    @POST("api/auth/desktop/v2/phone/decision")
    suspend fun desktopPhoneDecision(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionRequest,
    ): ApiEnvelope<ma.etherealmber.atlascommerce.data.model.DesktopPhoneDecisionDto>

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

    @GET("api/units")
    suspend fun productUnits(): ApiEnvelope<List<ProductUnitDto>>

    @GET("api/dashboard")
    suspend fun dashboard(): ApiEnvelope<DashboardDto>

    @GET("api/metrics/daily")
    suspend fun dailyMetrics(): ApiEnvelope<DailyMetricsDto>

    @GET("api/settings")
    suspend fun settings(): ApiEnvelope<CompanySettingsDto>

    /** Android catalog cards render cached thumbnails, so `full` is needed for image_url; the server still scrubs role-restricted fields. */
    @GET("api/products")
    suspend fun products(
        @Query("q") query: String?,
        @Query("limit") limit: Int = 300,
        @Query("fields") fields: String = "full",
    ): ApiEnvelope<List<ProductDto>>

    @POST("api/products")
    suspend fun createProduct(@Body body: CreateProductRequest): ApiEnvelope<ProductDto>

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body body: UpdateProductRequest,
    ): ApiEnvelope<ProductDto>

    @HTTP(method = "DELETE", path = "api/products/{id}", hasBody = true)
    suspend fun deleteProduct(
        @Path("id") id: Long,
        @Body body: ProductDeleteRequest,
    ): ApiEnvelope<ActionAckDto>

    @GET("api/products/{id}/movements")
    suspend fun productMovements(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("full") full: Int = 1,
    ): ApiEnvelope<List<ProductMovementDto>>

    @POST("api/catalog/products/{id}/images")
    suspend fun addProductImage(
        @Path("id") id: Long,
        @Body body: ProductImageUploadRequest,
    ): ApiEnvelope<ProductImageDto>

    @POST("api/products/{id}/adjust-stock")
    suspend fun adjustStock(
        @Path("id") id: Long,
        @Body body: StockAdjustRequest,
    ): ApiEnvelope<ProductDto>

    @POST("api/stock-adjustment-requests")
    suspend fun submitStockAdjustmentRequest(
        @Body body: StockAdjustmentRequestBody,
    ): ApiEnvelope<StockAdjustmentRequestDto>

    @GET("api/stock-adjustment-requests")
    suspend fun stockAdjustmentRequests(
        @Query("status") status: String? = null,
    ): ApiEnvelope<List<StockAdjustmentRequestDto>>

    @POST("api/stock-adjustment-requests/{id}/approve")
    suspend fun approveStockAdjustmentRequest(@Path("id") id: Long): ApiEnvelope<StockAdjustmentRequestDto>

    @POST("api/stock-adjustment-requests/{id}/reject")
    suspend fun rejectStockAdjustmentRequest(@Path("id") id: Long): ApiEnvelope<StockAdjustmentRequestDto>

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

    @GET("api/mobile-actions/{id}")
    suspend fun mobileAction(@Path("id") id: Long): ApiEnvelope<MobileActionDto>

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

    @Streaming
    @Headers("Accept: text/event-stream")
    @POST("api/assist/chat/stream")
    suspend fun assistChatStream(@Body body: AssistChatRequest): Response<ResponseBody>

    @POST("api/assist/approve")
    suspend fun assistApprove(@Body body: AssistApproveRequest): ApiEnvelope<AssistApproveDto>

    // --- Clients & Quick Sales ---

    @GET("api/clients")
    suspend fun clients(): ApiEnvelope<List<ClientDto>>

    // Admin-only finance APIs: the worker re-validates the active owner/admin role.
    @GET("api/admin/finance/statement")
    suspend fun financeStatement(
        @Query("period") period: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): ApiEnvelope<FinanceStatementDto>

    @GET("api/admin/finance/monthly")
    suspend fun financeMonthly(
        @Query("period") period: String? = null,
    ): ApiEnvelope<FinanceMonthlyDto>

    @GET("api/admin/finance/period")
    suspend fun financePeriod(@Query("period") period: String): ApiEnvelope<FinancePeriodDto>

    @POST("api/expenses")
    suspend fun createFinanceEntry(@Body body: CreateFinanceEntryRequest): ApiEnvelope<FinanceEntryDto>

    @HTTP(method = "DELETE", path = "api/expenses/{id}", hasBody = true)
    suspend fun deleteFinanceEntry(
        @Path("id") id: Long,
        @Body body: ExpenseDeleteRequest,
    ): ApiEnvelope<ActionAckDto>

    @GET("api/admin/employees/payroll")
    suspend fun employeePayroll(
        @Query("period") period: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("due_before") dueBefore: String? = null,
    ): ApiEnvelope<PayrollPageDto>

    @GET("api/employees")
    suspend fun employees(@Query("status") status: String = "active"): ApiEnvelope<List<EmployeeDto>>

    @GET("api/employees/{id}")
    suspend fun employee(@Path("id") id: Long): ApiEnvelope<EmployeeDto>

    @PUT("api/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") id: Long,
        @Body body: EmployeeUpdateRequest,
    ): ApiEnvelope<EmployeeDto>

    @POST("api/employees/{id}/resign")
    suspend fun markEmployeeFormer(
        @Path("id") id: Long,
        @Body body: EmployeeResignRequest,
    ): ApiEnvelope<EmployeeDto>

    @POST("api/employees/{id}/pay")
    suspend fun payEmployee(
        @Path("id") id: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: EmployeePayrollActionRequest,
    ): ApiEnvelope<EmployeeDto>

    @POST("api/employees/{id}/advance")
    suspend fun advanceEmployee(
        @Path("id") id: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: EmployeePayrollActionRequest,
    ): ApiEnvelope<EmployeeDto>

    @DELETE("api/employees/{id}")
    suspend fun deleteEmployee(@Path("id") id: Long): ApiEnvelope<ActionAckDto>

    @Streaming
    @GET("api/admin/finance/monthly/report.pdf")
    suspend fun financeMonthlyPdf(
        @Query("period") period: String,
    ): Response<ResponseBody>

    @GET("api/invoices")
    suspend fun invoices(
        @Query("kind") kind: String = "all",
        @Query("client") clientId: Long? = null,
    ): ApiEnvelope<List<InvoiceDto>>

    @POST("api/invoices")
    suspend fun createInvoice(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: QuickSaleRequest,
    ): ApiEnvelope<CreatedInvoiceDto>
}
