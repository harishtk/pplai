package com.aiavatar.app.feature.home.presentation.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.feature.home.presentation.profile.ProfileListUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject


/**
 * @author Hariskumar Kubendran
 * @date 06/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@HiltViewModel
class ModelPickerViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
) : ViewModel() {

    // TODO: expose a model list without status
    val uiState: StateFlow<ModelPickerState> = homeRepository.getMyModels(forceRefresh = false)
        .map { result ->
            Timber.d("Result: $result")
            when (result) {
                is Result.Loading -> {
                    ModelPickerState.Loading
                }
                is Result.Error -> {
                    ModelPickerState.NotLoading
                }
                is Result.Success -> {
                    val uiModelList = result.data
                        .filterNot { it.model == null }
                        .map { ProfileListUiModel.Item(it) }
                    ModelPickerState(
                        loading = false,
                        modelsUiModelList = uiModelList
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ModelPickerState.Loading
        )
}

data class ModelPickerState(
    val loading: Boolean = false,
    val modelsUiModelList: List<ProfileListUiModel> = emptyList()
) {
    companion object {
        internal val Loading = ModelPickerState(loading = true)
        internal val NotLoading = ModelPickerState(loading = false)
    }
}