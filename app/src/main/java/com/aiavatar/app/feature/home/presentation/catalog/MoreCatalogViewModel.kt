package com.aiavatar.app.feature.home.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.feature.home.domain.model.CatalogList
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class MoreCatalogViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoreCatalogState>(MoreCatalogState())
    val uiState: StateFlow<MoreCatalogState> = _uiState.asStateFlow()

    val accept: (MoreCatalogUiAction) -> Unit

    private var catalogDetailFetchJob: Job? = null

    init {
        uiState.map { it.catalogName }
            .distinctUntilChanged()
            .onEach { catalogName ->
                if (catalogName != null) {
                    getCatalogDetailInternal(catalogName, false)
                }
            }
            .launchIn(viewModelScope)

        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: MoreCatalogUiAction) {
        when (action) {
            is MoreCatalogUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorText = null
                    )
                }
            }
        }
    }

    fun retry() {
        uiState.value.catalogName?.let {
            getCatalogDetailInternal(it, true)
        }
    }

    fun setCatalogName(catalogName: String) {
        _uiState.update { state ->
            state.copy(
                catalogName = catalogName
            )
        }
    }

    private fun getCatalogDetailInternal(catalogName: String, forceRefresh: Boolean = false) {
        val request = CatalogDetailRequest(category = catalogName)
        getCatalogDetail(request, forceRefresh)
    }

    private fun getCatalogDetail(
        request: CatalogDetailRequest,
        forceRefresh: Boolean
    ) {
        /*if (catalogDetailFetchJob?.isActive == true) {
            val t = IllegalStateException("A fetch job is already active. Ignoring request")
            Timber.e(t)
            return
        }*/
        catalogDetailFetchJob?.cancel(CancellationException("New request")) // just in case
        catalogDetailFetchJob = viewModelScope.launch {
            setLoading(LoadType.REFRESH, LoadState.Loading())
            homeRepository.getCatalogList2(request, forceRefresh).collectLatest { result ->
                Timber.d("Catalog list: result $result")
                when (result) {
                    is Result.Loading -> setLoading(LoadType.REFRESH, LoadState.Loading())
                    is Result.Error -> {
                        when (result.exception) {
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
                        setLoading(LoadType.REFRESH, LoadState.Error(result.exception))
                    }
                    is Result.Success -> {
                        setLoading(LoadType.REFRESH, LoadState.NotLoading.Complete)
                        _uiState.update { state ->
                            state.copy(
                                avatarList = result.data.avatars.map { MoreCatalogUiModel.Item(it, false) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun setLoading(
        loadType: LoadType,
        loadState: LoadState
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { it.copy(loadState = newLoadState) }
    }

}

data class MoreCatalogState(
    val loadState: LoadStates = LoadStates.IDLE,
    val catalogName: String? = null,
    val avatarList: List<MoreCatalogUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface MoreCatalogUiAction {
    data class ErrorShown(val e: Exception) : MoreCatalogUiAction
}

interface MoreCatalogUiModel {
    data class Item(val catalogList: CatalogList, val selected: Boolean) : MoreCatalogUiModel
}