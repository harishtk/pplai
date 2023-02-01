package com.aiavatar.app.feature.home.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiavatar.app.core.data.source.local.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appDatabase: AppDatabase
) : ViewModel() {

    fun logout() = viewModelScope.launch(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }
}