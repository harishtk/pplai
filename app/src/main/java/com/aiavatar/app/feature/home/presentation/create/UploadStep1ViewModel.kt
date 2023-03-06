package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.runtimetest.MockApiDataProvider
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.home.presentation.create.util.CreateCreditExhaustedException
import com.aiavatar.app.feature.onboard.domain.model.CreateCheckData
import com.aiavatar.app.feature.onboard.domain.model.request.CreateCheckRequest
import com.aiavatar.app.feature.onboard.domain.repository.AccountsRepository
import com.aiavatar.app.ifDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


/**
 * @author Hariskumar Kubendran
 * @date 04/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@HiltViewModel
class UploadStep1ViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val accountsRepository: AccountsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val mockApiDataProvider: MockApiDataProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadStep1State>(UploadStep1State())
    val uiState: StateFlow<UploadStep1State> = _uiState.asStateFlow()

    private var createCheckJob: Job? = null

    init {
        // setLoadingInternal(LoadType.ACTION, LoadState.Loading())

        homeRepository.getMyModels(forceRefresh = true)
            .map { result ->
                when (result) {
                    is com.aiavatar.app.commons.util.Result.Success -> {
                        result.data.count { it.model != null }
                    }
                    else -> 0
                }
            }
            .onEach { modelCount ->
                _uiState.update { state ->
                    state.copy(
                        myModelCount = modelCount
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setLoading(loading: Boolean) {
        if (loading) {
            setLoadingInternal(LoadType.ACTION, LoadState.Loading())
        } else {
            setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.Complete)
        }
    }

    fun checkCreate(onCompletion: (kotlin.Result<CreateCheckData>) -> Unit) {
        val request = CreateCheckRequest(
            timestamp = System.currentTimeMillis()
        )
        checkCreateInternal(request, onCompletion)
    }

    private fun checkCreateInternal(
        request: CreateCheckRequest,
        onCompletion: (kotlin.Result<CreateCheckData>) -> Unit
    ) {
        if (createCheckJob?.isActive == true) {
            val cause = IllegalStateException("A create check job is already active. Ignoring request.")
            ifDebug { Timber.w(cause) }
            return
        }

        createCheckJob?.cancel(CancellationException("New request")) // just in case
        createCheckJob = viewModelScope.launch {
            accountsRepository.createCheck(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> {
                        setLoadingInternal(LoadType.ACTION, LoadState.Loading())
                    }
                    is Result.Error -> {
                        onCompletion(kotlin.Result.failure(result.exception))
                        setLoadingInternal(LoadType.ACTION, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.Complete)
                        onCompletion(kotlin.Result.success(result.data))
                        /*if (result.data.allowModelCreate) {

                        } else {
                            setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.InComplete)
                            val t = CreateCreditExhaustedException()
                            onCompletion(kotlin.Result.failure(t))
                            _uiState.update { state ->
                                state.copy(
                                    exception = t,
                                )
                            }
                        }*/
                    }
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun setLoadingInternal(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

}

data class UploadStep1State(
    val loadState: LoadStates = LoadStates.IDLE,
    val myModelCount: Int = 0,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)