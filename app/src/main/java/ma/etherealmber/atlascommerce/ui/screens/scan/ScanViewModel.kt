package ma.etherealmber.atlascommerce.ui.screens.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.Roles
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.persist.AtlasPreferences
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.CartStore
import ma.etherealmber.atlascommerce.data.model.CreateProductRequest
import ma.etherealmber.atlascommerce.data.model.ProductCreationRequestDto
import ma.etherealmber.atlascommerce.data.model.ProductDto
import ma.etherealmber.atlascommerce.data.model.SubmitProductRequestBody
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import ma.etherealmber.atlascommerce.data.repo.ProductSubmissionOutcome
import ma.etherealmber.atlascommerce.ui.screens.devices.QrTokenParser
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
    private val cartStore: CartStore,
    private val sessionManager: SessionManager,
    preferences: AtlasPreferences,
) : ViewModel() {

    data class UiState(
        /**
         * Camera is OFF (`false`) by default — entering Scan or jumping from Products
         * NEVER opens the camera automatically unless the user starts it explicitly.
         */
        val cameraEnabled: Boolean = false,
        val lookingUp: Boolean = false,
        val creating: Boolean = false,
        val lastCode: String? = null,
        val detectedPcQrToken: String? = null,
        val product: ProductDto? = null,
        val notFound: Boolean = false,
        val pendingApproval: Boolean = false,
        val added: Boolean = false,
        val error: String? = null,
        val neededItems: List<ProductDto> = emptyList(),
        val openRequests: List<ProductCreationRequestDto> = emptyList(),
    ) {
        val hasActiveScan: Boolean
            get() = lookingUp ||
                product != null ||
                notFound ||
                detectedPcQrToken != null ||
                error != null ||
                lastCode != null
    }

    val autoOpenCamera: StateFlow<Boolean> = preferences.autoOpenCamera
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val scanHapticEnabled: StateFlow<Boolean> = preferences.scanHapticEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val scanSoundEnabled: StateFlow<Boolean> = preferences.scanSoundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val autoAddCartOnScan: StateFlow<Boolean> = preferences.autoAddCartOnScan
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _scanFeedbackEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val scanFeedbackEvents: SharedFlow<Unit> = _scanFeedbackEvents.asSharedFlow()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var lastHandledCode: String? = null
    private var lastHandledAt: Long = 0L
    private var lookupJob: Job? = null
    private var currentScanProductId: Long? = null
    private var currentScanAddedQty: Double = 0.0

    init {
        viewModelScope.launch {
            sessionManager.state
                .map { (it as? SessionState.Active)?.user?.scopeIdentity }
                .distinctUntilChanged()
                .collect { key ->
                    reset()
                    if (key != null) loadNeededContext()
                }
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _state.update { it.copy(cameraEnabled = enabled) }
    }

    fun loadNeededContext() {
        viewModelScope.launch {
            val prods = commerceRepository.products(limit = 200)
            val reqs = commerceRepository.productRequests("open")
            val needed = (prods as? ApiResult.Success)?.data
                ?.filter { it.isLowStock || it.isOutOfStock }
                ?.sortedBy { it.stock }
                ?.take(6)
                .orEmpty()
            val openReqs = (reqs as? ApiResult.Success)?.data?.take(6).orEmpty()
            _state.update { it.copy(neededItems = needed, openRequests = openReqs) }
        }
    }

    /** Called by the camera analyser or manual search; duplicate reads are throttled. */
    fun onBarcode(raw: String) {
        val code = raw.trim()
        if (code.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        if (code == lastHandledCode && now - lastHandledAt < THROTTLE_MS) return
        lastHandledCode = code
        lastHandledAt = now
        currentScanProductId = null
        currentScanAddedQty = 0.0
        _scanFeedbackEvents.tryEmit(Unit)

        // Guard against scanning a PC QR security link inside Stock Scan:
        val maybeQrToken = QrTokenParser.extractQrToken(code)
        if (maybeQrToken != null &&
            (code.startsWith("http", ignoreCase = true) || code.startsWith("atlascommerce://", ignoreCase = true))
        ) {
            _state.update {
                it.copy(
                    lookingUp = false,
                    lastCode = code,
                    detectedPcQrToken = maybeQrToken,
                    product = null,
                    notFound = false,
                    pendingApproval = false,
                    added = false,
                    error = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                lastCode = code,
                detectedPcQrToken = null,
                lookingUp = true,
                product = null,
                notFound = false,
                pendingApproval = false,
                added = false,
                error = null,
            )
        }

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            when (val result = commerceRepository.scan(code)) {
                is ApiResult.Failure -> _state.update {
                    when (result.code) {
                        "NOT_FOUND" -> it.copy(
                            lookingUp = false,
                            notFound = true,
                            pendingApproval = false,
                            error = null,
                        )
                        "PENDING_APPROVAL" -> it.copy(
                            lookingUp = false,
                            notFound = true,
                            pendingApproval = true,
                            error = null,
                        )
                        else -> it.copy(lookingUp = false, error = result.message)
                    }
                }

                is ApiResult.Success -> {
                    val product = result.data.displayName?.takeIf { it.isNotBlank() }
                        ?.let { merged -> result.data.copy(name = merged) }
                        ?: result.data
                    val activeRole = (sessionManager.state.value as? SessionState.Active)?.user?.role
                    val canSell = Roles.canCreateDocuments(activeRole)
                    val autoAdded = canSell && autoAddCartOnScan.value
                    currentScanProductId = product.id
                    if (autoAdded) {
                        cartStore.add(product, 1.0)
                        currentScanAddedQty = 1.0
                        _messages.tryEmit("${product.name} · ajouté automatiquement au panier")
                    }
                    _state.update {
                        it.copy(
                            lookingUp = false,
                            product = product,
                            notFound = false,
                            pendingApproval = false,
                            added = autoAdded,
                            error = null,
                        )
                    }
                }
            }
        }
    }

    fun addToCart(quantity: Double = 1.0) {
        val activeRole = (sessionManager.state.value as? SessionState.Active)?.user?.role
        if (!Roles.canCreateDocuments(activeRole)) {
            _messages.tryEmit("Votre rôle ne permet pas d'ajouter au panier de vente.")
            return
        }
        val product = _state.value.product ?: return
        cartStore.add(product, quantity)
        currentScanProductId = product.id
        currentScanAddedQty += quantity
        _state.update { it.copy(added = true) }
        _messages.tryEmit("${product.name} · ajouté au panier")
    }

    /**
     * Immediately cancels and clears the currently scanned object without saving it,
     * rolling back any cart unit added from this scan session if the user changed their mind.
     */
    fun cancelCurrentScan(silent: Boolean = false) {
        lookupJob?.cancel()
        lookupJob = null
        val addedId = currentScanProductId
        val addedQty = currentScanAddedQty
        if (addedId != null && addedQty > 0.0) {
            cartStore.decrement(addedId, addedQty)
        }
        currentScanProductId = null
        currentScanAddedQty = 0.0
        lastHandledCode = null
        lastHandledAt = 0L
        _state.update {
            it.copy(
                lookingUp = false,
                creating = false,
                lastCode = null,
                detectedPcQrToken = null,
                product = null,
                notFound = false,
                pendingApproval = false,
                added = false,
                error = null,
            )
        }
        if (!silent) {
            _messages.tryEmit(
                if (addedQty > 0.0) {
                    "Objet scanné annulé et retiré du panier."
                } else {
                    "Scan annulé — rien n'a été enregistré."
                },
            )
        }
    }

    /**
     * Strict role-gated submission from Scanner:
     * - Admin (`owner` / `admin`): creates product immediately in stock (`POST /api/products`).
     * - Non-Admin: immediately forwards full product fiche to Admin (`POST /api/mobile-actions` + `product-requests`).
     */
    fun createProductFromScan(request: CreateProductRequest) {
        viewModelScope.launch {
            val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
            _state.update { it.copy(creating = true) }
            when (
                val res = commerceRepository.submitProductForRole(
                    request = request,
                    role = activeUser?.role,
                    username = activeUser?.username,
                    note = request.description,
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(creating = false) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    when (val outcome = res.data) {
                        is ProductSubmissionOutcome.CreatedDirectly -> {
                            _state.update {
                                it.copy(
                                    creating = false,
                                    product = outcome.product,
                                    notFound = false,
                                    pendingApproval = false,
                                )
                            }
                            _messages.tryEmit("Produit « ${outcome.product.name} » créé au catalogue.")
                        }

                        is ProductSubmissionOutcome.SentToAdmin -> {
                            _state.update {
                                it.copy(
                                    creating = false,
                                    pendingApproval = true,
                                )
                            }
                            _messages.tryEmit(outcome.summary)
                        }
                    }
                    loadNeededContext()
                }
            }
        }
    }

    fun submitRequestFromScan(
        code: String,
        name: String,
        sku: String?,
        unit: String,
        quantity: Double,
        category: String?,
        note: String?,
    ) {
        val activeUser = (sessionManager.state.value as? SessionState.Active)?.user
        if (!Roles.canCreateProductDirectly(activeUser?.role)) {
            createProductFromScan(
                CreateProductRequest(
                    name = name,
                    sku = sku ?: "SKU-${code.takeLast(6)}",
                    barcode = code,
                    category = category,
                    unit = unit,
                    price = 0.0,
                    stockQuantity = quantity,
                    description = note,
                ),
            )
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            when (
                val res = commerceRepository.submitProductRequest(
                    SubmitProductRequestBody(
                        code = code,
                        name = name,
                        sku = sku,
                        unit = unit,
                        quantity = quantity,
                        category = category,
                        note = note,
                    ),
                )
            ) {
                is ApiResult.Failure -> {
                    _state.update { it.copy(creating = false) }
                    _messages.tryEmit(res.message)
                }

                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            creating = false,
                            pendingApproval = true,
                        )
                    }
                    _messages.tryEmit("Demande produit enregistrée.")
                    loadNeededContext()
                }
            }
        }
    }

    fun clearPcQrRedirect() {
        _state.update { it.copy(detectedPcQrToken = null) }
    }

    fun retry() {
        val code = _state.value.lastCode ?: return
        lastHandledCode = null
        onBarcode(code)
    }

    fun reset() {
        lookupJob?.cancel()
        lookupJob = null
        currentScanProductId = null
        currentScanAddedQty = 0.0
        lastHandledCode = null
        _state.value = UiState()
    }

    private companion object {
        const val THROTTLE_MS = 2_500L
    }
}
