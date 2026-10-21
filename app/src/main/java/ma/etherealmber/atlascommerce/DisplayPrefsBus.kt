package ma.etherealmber.atlascommerce

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DisplayPrefsBus {
    private val _ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val ticks: SharedFlow<Unit> = _ticks.asSharedFlow()
    fun notifyChanged() { _ticks.tryEmit(Unit) }
}
