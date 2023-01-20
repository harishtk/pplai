package com.aiavatar.app.feature.home.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // TODO: fetch user models

}

data class ProfileState(
    val loadState: LoadStates = LoadStates.IDLE,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface ProfileUiAction {
    data class ErrorShown(val e: Exception) : ProfileUiAction
}

interface ProfileUiEvent {
    data class ShowToast(val message: UiText) : ProfileUiEvent
}