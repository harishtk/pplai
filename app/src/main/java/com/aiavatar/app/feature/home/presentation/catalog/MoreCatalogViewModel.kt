package com.aiavatar.app.feature.home.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.commons.util.net.ApiException
import com.aiavatar.app.commons.util.net.NoInternetException
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.model.ListAvatar
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
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

    val accept: (CatalogDetailUiAction) -> Unit

    private var catalogDetailFetchJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: CatalogDetailUiAction) {
        when (action) {
            is CatalogDetailUiAction.ErrorShown -> {
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
        uiState.value.category?.let {
            getCatalogDetailInternal(it.categoryName!!)
        }
    }

    fun setCategory(category: Category) {
        _uiState.update { state ->
            state.copy(
                category = category
            )
        }
        getCatalogDetailInternal(category.categoryName!!)
    }

    private fun getCatalogDetailInternal(category: String) {
        val request = CatalogDetailRequest(category)
        getCatalogDetail(request)
    }

    private fun getCatalogDetail(request: CatalogDetailRequest) {
        if (catalogDetailFetchJob?.isActive == true) {
            val t = IllegalStateException("A fetch job is already active. Ignoring request")
            Timber.e(t)
            return
        }
        catalogDetailFetchJob?.cancel(CancellationException("New request")) // just in case
        catalogDetailFetchJob = viewModelScope.launch {
            homeRepository.getCatalogDetail(request).collectLatest { result ->
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
    val category: Category? = null,
    val avatarList: List<MoreCatalogUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface MoreCatalogUiAction {
    data class ErrorShown(val e: Exception) : MoreCatalogUiAction
}

interface MoreCatalogUiModel {
    data class Item(val listAvatar: ListAvatar, val selected: Boolean) : MoreCatalogUiModel
}