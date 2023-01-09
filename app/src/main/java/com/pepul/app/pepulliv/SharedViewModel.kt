package com.pepul.app.pepulliv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepul.app.pepulliv.feature.onboard.domain.model.request.AutoLoginRequest
import com.pepul.app.pepulliv.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository
) : ViewModel() {

    private var autoLoginJob: Job? = null

    fun autoLogin() {
        autoLoginJob?.cancel(CancellationException("New Request"))
        val autoLoginRequest = AutoLoginRequest(System.currentTimeMillis())
        autoLoginJob = viewModelScope.launch {
            accountsRepository.autoLogin(autoLoginRequest).collectLatest { result ->
                Timber.d("Auto Login: $result")
            }
        }
    }

}