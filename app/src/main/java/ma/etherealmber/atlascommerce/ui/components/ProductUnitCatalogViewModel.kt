package ma.etherealmber.atlascommerce.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.data.model.ProductUnitDefaults
import ma.etherealmber.atlascommerce.data.repo.CommerceRepository
import javax.inject.Inject

@HiltViewModel
class ProductUnitCatalogViewModel @Inject constructor(
    private val commerceRepository: CommerceRepository,
) : ViewModel() {
    private val _units = MutableStateFlow(ProductUnitDefaults.names)
    val units: StateFlow<List<String>> = _units.asStateFlow()

    private var loadStarted = false

    fun load() {
        if (loadStarted) return
        loadStarted = true
        viewModelScope.launch {
            when (val result = commerceRepository.productUnits()) {
                is ApiResult.Success -> _units.value = ProductUnitDefaults.merge(result.data)
                is ApiResult.Failure -> _units.value = ProductUnitDefaults.names
            }
        }
    }
}
