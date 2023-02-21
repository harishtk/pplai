package com.aiavatar.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.toLoginUser
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.onboard.domain.model.request.AutoLoginRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    @Deprecated("use repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val loginUser = appDatabase.loginUserDao().getLoginUser()
        .map { it?.toLoginUser() }

    /**
     * Holds the current authentication state of the user.
     */
    val authenticationState = appDatabase.loginUserDao()
        .getLoginUser()
        .map {
            if (it?.userId?.isNotBlank() == true) {
                AuthenticationState.AUTHENTICATED
            } else {
                AuthenticationState.NOT_AUTHENTICATED
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthenticationState.UNKNOWN
        )

    private var autoLoginJob: Job? = null

    fun autoLogin() {
        autoLoginJob?.cancel(CancellationException("New Request"))
        val autoLoginRequest = AutoLoginRequest(System.currentTimeMillis())
        autoLoginJob = viewModelScope.launch {
            accountsRepository.autoLogin(autoLoginRequest).collectLatest { result ->
                Timber.d("Auto Login: $result")
                when (result) {
                    is Result.Loading -> { /* Noop */ }
                    is Result.Error -> {
                        /* TODO: Retry */
                    }
                    is Result.Success -> {
                        // TODO: -partially_done- parse result
                        _forceUpdate.update { result.data.forceUpdate }
                    }
                }
            }
        }
    }

    private var _forceUpdate: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val forceUpdate = _forceUpdate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }
}

enum class AuthenticationState {
    UNKNOWN, AUTHENTICATED, NOT_AUTHENTICATED;

    fun isAuthenticated(): Boolean {
        return this == AUTHENTICATED
    }
}