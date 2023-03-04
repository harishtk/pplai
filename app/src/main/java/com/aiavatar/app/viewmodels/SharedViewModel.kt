package com.aiavatar.app.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.net.ConnectivityManagerLiveData
import com.aiavatar.app.commons.util.runtimetest.MockApiDataProvider
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SharedViewModel @Inject constructor(
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val connectivityManagerLiveData: ConnectivityManagerLiveData,
    private val accountsRepository: AccountsRepository,
    private val mockApiDataProvider: MockApiDataProvider,
) : ViewModel() {

    init {
        viewModelScope.launch {
            val currentSession = appDatabase.uploadSessionDao().getCurrentUploadSessionSync()
            if (currentSession.isNotEmpty()) {
                setCurrentUploadSessionId(currentSession[0].uploadSessionEntity._id!!)
            }
        }
    }

    /* Currently uploading session id */
    private var _currentUploadSessionId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val currentUploadSessionId = _currentUploadSessionId
        .shareIn(
            scope = viewModelScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed(5000)
        )

    fun setCurrentUploadSessionId(sessionId: Long) {
        _currentUploadSessionId.update { sessionId }
    }

    val shouldShowStatus: SharedFlow<Boolean> = appDatabase.uploadSessionDao().getCurrentUploadSession()
        .map { sessions ->
            if (sessions.isNotEmpty()) {
                setCurrentUploadSessionId(sessions[0].uploadSessionEntity._id!!)
                return@map true
            }
            false
        }
        .distinctUntilChanged()
        .shareIn(
            scope = viewModelScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed(5000)
        )

    private val _jumpToDestination = MutableSharedFlow<Int>()
    val jumpToDestination = _jumpToDestination
        .shareIn(
            scope = viewModelScope,
            replay = 0,
            started = SharingStarted.WhileSubscribed(5000)
        )

    fun setJumpToDestination(@IdRes destinationId: Int) = viewModelScope.launch {
        _jumpToDestination.emit(destinationId)
    }

    /* Connection Listener */
    val connectionStateSharedFlow = connectivityManagerLiveData
        .asFlow()
        .debounce(1500L)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1,
        )

    val createCheckDataFlow = accountsRepository.createCheck(
        CreateCheckRequest(timestamp = System.currentTimeMillis())
    )
        .map { result ->
            when (result) {
                is Result.Success -> result.data
                else -> null
            }
        }
        .conflate() /* We only care 'bout the latest value */
}