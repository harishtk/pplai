package com.aiavatar.app.feature.home.presentation.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import com.aiavatar.app.commons.util.loadstate.LoadType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject


/**
 * @author Hariskumar Kubendran
 * @date 04/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
@HiltViewModel
class UploadStep1ViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadStep1State>(UploadStep1State())
    val uiState: StateFlow<UploadStep1State> = _uiState.asStateFlow()

    init {
        setLoadingInternal(LoadType.ACTION, LoadState.Loading())
    }

    fun setLoading(loading: Boolean) {
        if (loading) {
            setLoadingInternal(LoadType.ACTION, LoadState.Loading())
        } else {
            setLoadingInternal(LoadType.ACTION, LoadState.NotLoading.Complete)
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
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)