package com.aiavatar.app.feature.home.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.core.domain.util.BuenoCacheException
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogState>(CatalogState())
    val uiState: StateFlow<CatalogState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CatalogUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (CatalogUiAction) -> Unit

    private var catalogFetchJob: Job? = null

    init {

        accept = { uiAction -> onUiAction(action = uiAction) }

        refreshInternal(false)
    }

    private fun onUiAction(action: CatalogUiAction) {
        when (action) {
            is CatalogUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun refresh() {
        refreshInternal(forceRefresh = true)
    }

    private fun refreshInternal(forceRefresh: Boolean) {
        getCatalogs(forceRefresh)
    }

    private fun getCatalogs(forceRefresh: Boolean = false) {
        /*if (catalogFetchJob?.isActive == true) {
            val t = IllegalStateException("A fetch request is already in progress. Ignoring request")
            Timber.d(t)
            return
        }*/
        catalogFetchJob?.cancel(CancellationException("New request")) // just in case
        catalogFetchJob = viewModelScope.launch {
            homeRepository.getCatalog2(forceRefresh).collectLatest { result ->
                Timber.d("Catalog result: $result")
                when (result) {
                    is Result.Loading -> {
                        setLoadState(LoadType.REFRESH, LoadState.Loading())
                    }
                    is Result.Error -> {
                        when (result.exception) {
                            is BuenoCacheException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is ApiException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorText = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoadState(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoadState(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        _uiState.update { state ->
                            state.copy(
                                catalogList = result.data.map { AvatarUiModel.AvatarItem(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setLoadState(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state ->
            state.copy(
                loadState = newLoadState
            )
        }
    }

    private fun sendEvent(newEvent: CatalogUiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class CatalogState(
    val loadState: LoadStates = LoadStates.IDLE,
    val catalogList: List<AvatarUiModel>? = null,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface CatalogUiAction {
    data class ErrorShown(val e: Exception) : CatalogUiAction
}

interface CatalogUiEvent {
    data class ShowToast(val message: UiText) : CatalogUiEvent
}

interface CatalogUiModel {
    data class Catalog(val category: Category) : CatalogUiModel
}

interface AvatarUiModel {
    data class AvatarItem(val category: Category) : AvatarUiModel
}