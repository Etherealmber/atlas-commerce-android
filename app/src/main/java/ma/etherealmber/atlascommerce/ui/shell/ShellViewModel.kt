package ma.etherealmber.atlascommerce.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ma.etherealmber.atlascommerce.data.CartStore
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    cartStore: CartStore,
) : ViewModel() {

    val cartCount: StateFlow<Int> = cartStore.lines
        .map { lines -> lines.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
