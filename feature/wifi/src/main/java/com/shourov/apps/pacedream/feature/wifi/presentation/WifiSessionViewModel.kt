package com.shourov.apps.pacedream.feature.wifi.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wifi.WifiSessionRouter
import com.shourov.apps.pacedream.feature.wifi.data.WifiSessionRepository
import com.shourov.apps.pacedream.feature.wifi.data.WifiSessionResponse
import com.shourov.apps.pacedream.feature.wifi.util.WifiTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the persistent Wi-Fi session pill + sheets.
 *
 * Source-of-truth contract:
 * - `expiresAt` is owned by the server. We accept it from a push (instant) or
 *   a `GET /wifi/sessions/{id}` (canonical), and reconcile every 30 seconds.
 * - The local 1-second ticker only updates `secondsRemaining` for display.
 *   It never advances `expiresAt`.
 * - On reconcile failure we keep the last server `expiresAt` and trust the
 *   ticker — but we never auto-mark the session expired without a server
 *   confirmation.
 *
 * The session is "expired" only when:
 * - server returns status=expired/ended OR
 * - server returns the session no longer exists (404) AND
 * - local ticker has gone past `expiresAt + graceSeconds`.
 */
@HiltViewModel
class WifiSessionViewModel @Inject constructor(
    private val repository: WifiSessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WifiSessionUiState())
    val state: StateFlow<WifiSessionUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            WifiSessionRouter.flow.collect { intent -> handle(intent) }
        }
    }

    private fun handle(intent: WifiSessionRouter.Intent) {
        when (intent) {
            is WifiSessionRouter.Intent.Start -> {
                val expiresAt = WifiTime.parseInstant(intent.expiresAtIso)
                if (intent.sessionId == null || expiresAt == null) {
                    // Push without enough info — fall back to a refetch
                    intent.sessionId?.let { startSession(it) }
                    return
                }
                seedSession(
                    sessionId = intent.sessionId,
                    bookingId = intent.bookingId,
                    ssid = intent.ssid,
                    expiresAt = expiresAt
                )
                // Reconcile to backfill password / canExtend / graceSeconds.
                refresh(intent.sessionId)
            }
            is WifiSessionRouter.Intent.ShowExtend ->
                _state.update { it.copy(sheet = WifiSessionUiState.Sheet.Extend) }
            is WifiSessionRouter.Intent.ShowSheet ->
                _state.update { it.copy(sheet = WifiSessionUiState.Sheet.Session) }
            is WifiSessionRouter.Intent.ShowExpired ->
                _state.update {
                    it.copy(
                        phase = WifiSessionUiState.Phase.Expired,
                        sheet = WifiSessionUiState.Sheet.Expired,
                        secondsRemaining = 0L
                    )
                }
            is WifiSessionRouter.Intent.Refresh ->
                intent.sessionId?.let { refresh(it) }
            WifiSessionRouter.Intent.Clear -> clear()
        }
    }

    /** Open a session by id when we don't yet have an expiresAt locally. */
    fun startSession(sessionId: String) {
        viewModelScope.launch {
            repository.getSession(sessionId).onSuccess { applyServerSession(it) }
        }
    }

    fun dismissSheet() {
        _state.update { it.copy(sheet = WifiSessionUiState.Sheet.None) }
    }

    fun openSheet() {
        _state.update { it.copy(sheet = WifiSessionUiState.Sheet.Session) }
    }

    fun openExtendSheet() {
        _state.update { it.copy(sheet = WifiSessionUiState.Sheet.Extend) }
    }

    fun extend(minutes: Int) {
        val sessionId = _state.value.sessionId ?: return
        if (_state.value.extensionInProgress) return
        _state.update { it.copy(extensionInProgress = true, errorMessage = null) }
        viewModelScope.launch {
            repository.extend(sessionId, minutes).fold(
                onSuccess = { resp ->
                    val expiresAt = WifiTime.parseInstant(resp.expiresAt)
                    _state.update {
                        it.copy(
                            extensionInProgress = false,
                            sheet = WifiSessionUiState.Sheet.None,
                            expiresAt = expiresAt ?: it.expiresAt,
                            canExtend = resp.canExtend
                        )
                    }
                    recomputePhase()
                    restartTicker()
                },
                onFailure = { e ->
                    Timber.w(e, "Wi-Fi extend failed")
                    _state.update {
                        it.copy(
                            extensionInProgress = false,
                            errorMessage = "Couldn't extend. Try again."
                        )
                    }
                }
            )
        }
    }

    fun reconnect() {
        val sessionId = _state.value.sessionId ?: return
        if (_state.value.extensionInProgress) return
        _state.update { it.copy(extensionInProgress = true, errorMessage = null) }
        viewModelScope.launch {
            repository.reconnect(sessionId).fold(
                onSuccess = { resp ->
                    applyServerSession(resp)
                    _state.update {
                        it.copy(
                            extensionInProgress = false,
                            sheet = WifiSessionUiState.Sheet.None
                        )
                    }
                },
                onFailure = { e ->
                    Timber.w(e, "Wi-Fi reconnect failed")
                    _state.update {
                        it.copy(
                            extensionInProgress = false,
                            errorMessage = "Reconnect failed. Please book again."
                        )
                    }
                }
            )
        }
    }

    fun clear() {
        tickerJob?.cancel(); tickerJob = null
        pollJob?.cancel(); pollJob = null
        _state.value = WifiSessionUiState()
    }

    // ── internals ──────────────────────────────────────

    private fun seedSession(
        sessionId: String,
        bookingId: String?,
        ssid: String?,
        expiresAt: Instant
    ) {
        _state.update { current ->
            current.copy(
                sessionId = sessionId,
                bookingId = bookingId ?: current.bookingId,
                ssid = ssid ?: current.ssid,
                expiresAt = expiresAt,
                errorMessage = null
            )
        }
        recomputePhase()
        restartTicker()
        restartPoll(sessionId)
    }

    private fun refresh(sessionId: String) {
        viewModelScope.launch {
            repository.getSession(sessionId).onSuccess { applyServerSession(it) }
        }
    }

    private fun applyServerSession(resp: WifiSessionResponse) {
        val expiresAt = WifiTime.parseInstant(resp.expiresAt)
        if (expiresAt == null) {
            Timber.w("Wi-Fi session has unparseable expiresAt: %s", resp.expiresAt)
            return
        }
        val isExpiredOnServer = resp.status.equals("expired", true)
            || resp.status.equals("ended", true)
        _state.update {
            it.copy(
                sessionId = resp.sessionId,
                bookingId = resp.bookingId ?: it.bookingId,
                ssid = resp.ssid ?: it.ssid,
                password = resp.password ?: it.password,
                expiresAt = expiresAt,
                canExtend = resp.canExtend,
                graceSeconds = resp.graceSeconds,
                phase = if (isExpiredOnServer) WifiSessionUiState.Phase.Expired else it.phase,
                sheet = if (isExpiredOnServer) WifiSessionUiState.Sheet.Expired else it.sheet
            )
        }
        recomputePhase()
        if (!isExpiredOnServer) {
            restartTicker()
            restartPoll(resp.sessionId)
        }
    }

    private fun restartTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                tick()
                delay(1_000L)
            }
        }
    }

    private fun restartPoll(sessionId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                repository.getSession(sessionId).onSuccess { applyServerSession(it) }
            }
        }
    }

    private fun tick() {
        val expiresAt = _state.value.expiresAt ?: return
        val now = Instant.now()
        val seconds = Duration.between(now, expiresAt).seconds
        _state.update { it.copy(secondsRemaining = seconds.coerceAtLeast(0L)) }
        recomputePhase()
        // Auto-present extension sheet at the 3-min critical threshold
        // exactly once (when phase first transitions to Critical).
    }

    private fun recomputePhase() {
        val s = _state.value
        if (s.expiresAt == null) {
            _state.update { it.copy(phase = WifiSessionUiState.Phase.Idle) }
            return
        }
        val seconds = s.secondsRemaining
        val newPhase = when {
            seconds <= 0L && s.phase != WifiSessionUiState.Phase.Expired ->
                // Local timer ran out, but server has not confirmed. We still
                // mark the pill as Critical (red) and wait for server confirm
                // — never destroy session state on local time alone.
                WifiSessionUiState.Phase.Critical
            seconds <= 180L -> WifiSessionUiState.Phase.Critical
            seconds <= 900L -> WifiSessionUiState.Phase.Warning
            else -> WifiSessionUiState.Phase.Active
        }
        if (newPhase != s.phase) {
            val sheet = if (
                newPhase == WifiSessionUiState.Phase.Critical &&
                s.sheet == WifiSessionUiState.Sheet.None &&
                s.canExtend
            ) WifiSessionUiState.Sheet.Extend else s.sheet
            _state.update { it.copy(phase = newPhase, sheet = sheet) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        pollJob?.cancel()
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }
}
