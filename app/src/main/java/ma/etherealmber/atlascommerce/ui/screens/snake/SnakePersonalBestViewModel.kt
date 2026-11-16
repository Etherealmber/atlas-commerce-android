package ma.etherealmber.atlascommerce.ui.screens.snake

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ma.etherealmber.atlascommerce.core.net.ApiResult
import ma.etherealmber.atlascommerce.core.session.SessionManager
import ma.etherealmber.atlascommerce.core.session.SessionState
import ma.etherealmber.atlascommerce.data.repo.SnakeScoreRepository
import java.security.MessageDigest
import javax.inject.Inject

enum class SnakeScoreSyncStatus { GUEST, CHECKING, PENDING, SYNCED, UNAVAILABLE }

data class SnakePersonalBestState(
    val personalBest: Int = 0,
    val syncStatus: SnakeScoreSyncStatus = SnakeScoreSyncStatus.CHECKING,
)

@HiltViewModel
class SnakePersonalBestViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionManager: SessionManager,
    private val repository: SnakeScoreRepository,
) : ViewModel() {
    private val preferences = context.getSharedPreferences("atlas_snake_progress", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(SnakePersonalBestState())
    val state: StateFlow<SnakePersonalBestState> = _state.asStateFlow()

    // The raw bearer token is never retained by this ViewModel or written to storage.
    private var activeSession: ActiveSession? = null
    private var accountLoadJob: Job? = null
    private var scoreSyncJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state
                .map {
                    val token = sessionManager.authToken?.takeIf(String::isNotBlank)
                        ?: return@map null
                    if (sessionManager.state.value !is SessionState.Active) return@map null
                    ActiveSession(storageKey = storageKey(token))
                }
                .distinctUntilChanged()
                .collect { nextSession ->
                    activeSession = nextSession
                    accountLoadJob?.cancel()
                    scoreSyncJob?.cancel()
                    val localBest = readLocalBest(nextSession)
                    _state.value = SnakePersonalBestState(
                        personalBest = localBest,
                        syncStatus = if (nextSession == null) SnakeScoreSyncStatus.GUEST else SnakeScoreSyncStatus.CHECKING,
                    )
                    if (nextSession != null) {
                        accountLoadJob = viewModelScope.launch { reconcileWithServer(nextSession) }
                    }
                }
        }
    }

    /** Save each new best locally at once, then sync the active authenticated session after a debounce. */
    fun recordScore(score: Int) {
        if (score <= _state.value.personalBest) return
        val session = activeSession
        persistBest(session, score)
        _state.value = SnakePersonalBestState(
            personalBest = score,
            syncStatus = if (session == null) SnakeScoreSyncStatus.GUEST else SnakeScoreSyncStatus.PENDING,
        )
        if (session == null) return

        scoreSyncJob?.cancel()
        scoreSyncJob = viewModelScope.launch {
            delay(SCORE_SYNC_DEBOUNCE_MS)
            if (activeSession == session) pushBestToServer(session, score)
        }
    }

    private suspend fun reconcileWithServer(session: ActiveSession) {
        when (val result = repository.getPersonalBest()) {
            is ApiResult.Failure -> {
                if (activeSession == session) {
                    _state.value = _state.value.copy(syncStatus = SnakeScoreSyncStatus.UNAVAILABLE)
                }
            }
            is ApiResult.Success -> {
                if (activeSession != session) return
                val highest = maxOf(readLocalBest(session), _state.value.personalBest, result.data)
                persistBest(session, highest)
                _state.value = SnakePersonalBestState(
                    highest,
                    if (highest > result.data) SnakeScoreSyncStatus.PENDING else SnakeScoreSyncStatus.SYNCED,
                )
                if (highest > result.data) pushBestToServer(session, highest)
            }
        }
    }

    private suspend fun pushBestToServer(session: ActiveSession, requestedBest: Int) {
        if (activeSession != session) return
        var candidate = maxOf(requestedBest, readLocalBest(session), _state.value.personalBest)
        repeat(MAX_SYNC_ATTEMPTS) { attempt ->
            if (activeSession != session) return
            when (val result = repository.putPersonalBest(candidate)) {
                is ApiResult.Success -> {
                    if (activeSession != session) return
                    val highest = maxOf(candidate, result.data, readLocalBest(session), _state.value.personalBest)
                    persistBest(session, highest)
                    _state.value = SnakePersonalBestState(highest, SnakeScoreSyncStatus.SYNCED)
                    if (result.data >= highest) return
                    candidate = highest
                }
                is ApiResult.Failure -> {
                    if (activeSession == session) {
                        _state.value = _state.value.copy(syncStatus = SnakeScoreSyncStatus.UNAVAILABLE)
                    }
                    if (attempt == MAX_SYNC_ATTEMPTS - 1) return
                    val delayMs = result.retryAfterSeconds
                        ?.coerceIn(1L, MAX_RETRY_AFTER_SECONDS)
                        ?.times(1_000L)
                        ?: RETRY_DELAYS_MS[attempt]
                    delay(delayMs)
                    candidate = maxOf(candidate, readLocalBest(session), _state.value.personalBest)
                }
            }
        }
    }

    private fun readLocalBest(session: ActiveSession?): Int =
        preferences.getInt(session?.storageKey ?: GUEST_SCORE_KEY, 0).coerceAtLeast(0)

    private fun persistBest(session: ActiveSession?, score: Int) {
        val key = session?.storageKey ?: GUEST_SCORE_KEY
        val highest = maxOf(preferences.getInt(key, 0), score).coerceAtLeast(0)
        preferences.edit().putInt(key, highest).apply()
    }

    /** Storage key depends solely on the exact active session token; only its SHA-256 digest is used. */
    private fun storageKey(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        val suffix = buildString(digest.size * 2) {
            digest.forEach { byte -> append((byte.toInt() and 0xff).toString(16).padStart(2, '0')) }
        }
        return "best_score_session_$suffix"
    }

    private data class ActiveSession(val storageKey: String)

    private companion object {
        const val GUEST_SCORE_KEY = "best_score"
        const val SCORE_SYNC_DEBOUNCE_MS = 900L
        const val MAX_SYNC_ATTEMPTS = 2
        const val MAX_RETRY_AFTER_SECONDS = 30L
        val RETRY_DELAYS_MS = longArrayOf(3_000L, 8_000L)
    }
}
