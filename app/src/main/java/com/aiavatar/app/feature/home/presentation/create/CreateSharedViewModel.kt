package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.runtimetest.MockApiDataProvider
import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

@HiltViewModel
class CreateSharedViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val mockApiDataProvider: MockApiDataProvider,
) : ViewModel() {

    val createCheckDataFlow = accountsRepository.createCheck(
        CreateCheckRequest(timestamp = System.currentTimeMillis()),
    ).shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5000,
                replayExpirationMillis = 1000
            ),
            replay = 1
        )

}