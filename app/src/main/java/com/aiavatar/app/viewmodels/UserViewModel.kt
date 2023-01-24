package com.aiavatar.app.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.core.domain.repository.AppRepository
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
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
class UserViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    val userId = _userId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = readFromPrefs()
        )

    private fun readFromPrefs(): String {
        return ApplicationDependencies.getPersistentStore().userId
    }

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
}