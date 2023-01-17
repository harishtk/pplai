package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.feature.home.presentation.util.GenderModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UploadStep3ViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<Step3State>(Step3State())
    val uiState: StateFlow<Step3State> = _uiState.asStateFlow()

    private var selectedGenderPosition: Int = 0
    private val selectedToggleFlow = MutableStateFlow(false)

    init {
        val genderModelList = mutableListOf<GenderModel>(
            GenderModel("Male"),
            GenderModel("Female"),
            GenderModel("Others")
        )

        val genderListFlow = uiState.map { it.genderList }

        _uiState.update { state ->
            state.copy(
                genderList = genderModelList
            )
        }

        combine(
            selectedToggleFlow,
            genderListFlow,
            ::Pair
        ).map {(selectedToggle, genderList) ->
            val newGenderList = genderList.mapIndexed { index, genderModel ->
                genderModel.copy(selected = index == selectedGenderPosition) }
            newGenderList
        }.onEach { genderList ->
            _uiState.update { state ->
                state.copy(
                    genderList = genderList
                )
            }
        }.launchIn(viewModelScope)
    }

    fun toggleSelection(position: Int) {
        selectedGenderPosition = position
        // Signals the flow
        selectedToggleFlow.update { selectedToggleFlow.value.not() }
    }

}

data class Step3State(
    val selectedGenderPosition: Int = 0,
    val genderList: List<GenderModel> = emptyList()
)