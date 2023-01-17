package com.aiavatar.app.feature.home.presentation.create

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.feature.home.domain.model.SelectedMediaItem
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.toggle
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadStep2ViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadStep2State())
    val uiState: StateFlow<UploadStep2State> = _uiState.asStateFlow()

    private val selectedItems: HashMap<String, Boolean> = HashMap()
    private val selectedToggleFlow = MutableStateFlow(false)

    init {
        /*val placeholders: List<UploadPreviewUiModel> = (0 until MAX_IMAGE_COUNT).mapIndexed { index, _ ->
            UploadPreviewUiModel.Placeholder(position = index)
        }*/

        val pickedUrisFlow = uiState.map { it.pickedUris }

        combine(
            selectedToggleFlow,
            pickedUrisFlow,
            ::Pair
        ).map { (selectedToggle, pickedUris) ->
            val newPickedList = (0 until pickedUris.size + 1).mapIndexed { index, _ ->
                if (index < pickedUris.size) {
                    val model = pickedUris[index]
                    UploadPreviewUiModel.Item(
                        SelectedMediaItem(model),
                        selected = selectedItems[model.toString()] == true)
                } else {
                    UploadPreviewUiModel.Placeholder(position = index)
                }
            }
            newPickedList
        }.onEach { pickedUriModelList ->
            _uiState.update { state ->
                state.copy(
                    previewModelList = pickedUriModelList
                )
            }
        }.launchIn(viewModelScope)
    }


    fun setPickedUris(pickedUris: List<Uri>) {
        pickedUris.forEach { selectedItems[it.toString()] = true }
        _uiState.update { state ->
            state.copy(
                pickedUris = pickedUris
            )
        }
    }

    fun toggleSelectionInternal(uri: Uri) = viewModelScope.launch {
        val selected = selectedItems[uri.toString()] ?: false
        selectedItems[uri.toString()] = selected.toggle()
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

    fun clearSelectionInternal() = viewModelScope.launch {
        selectedItems.clear()
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

}

data class UploadStep2State(
    val loadState: LoadStates = LoadStates.IDLE,
    val pickedUris: List<Uri> = emptyList(),
    val previewModelList: List<UploadPreviewUiModel> = emptyList()
)

interface UploadPreviewUiModel {
    data class Item(val selectedMediaItem: SelectedMediaItem, val selected: Boolean) : UploadPreviewUiModel
    data class Placeholder(val position: Int) : UploadPreviewUiModel
}

private const val MAX_IMAGE_COUNT = 20