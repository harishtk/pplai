package com.aiavatar.app.feature.home.presentation.catalog

import androidx.lifecycle.ViewModel
import com.aiavatar.app.feature.home.domain.model.Category
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CatalogDetailViewModel @Inject constructor(
    private val homeRepository: HomeRepository
): ViewModel() {

    private val _uiState = MutableStateFlow<CatalogDetailState>(CatalogDetailState())
    val uiState: StateFlow<CatalogDetailState> = _uiState.asStateFlow()

    fun setCatalog(category: Category) {
        _uiState.update { state ->
            state.copy(
                category = category
            )
        }
    }
}

data class CatalogDetailState(
    val category: Category? = null
)