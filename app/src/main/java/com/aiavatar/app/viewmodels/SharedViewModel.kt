package com.aiavatar.app.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    @Deprecated("move to repo")
    private val appDatabase: AppDatabase,
    private val accountsRepository: AccountsRepository,
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
    @Deprecated("not used")
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

}