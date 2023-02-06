package com.aiavatar.app.feature.home.presentation.create

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.commons.util.UiText
import com.aiavatar.app.commons.util.loadstate.LoadType
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.aiavatar.app.core.data.source.local.entity.UploadFileStatus
import com.aiavatar.app.core.data.source.local.entity.UploadFilesEntity
import com.aiavatar.app.core.data.source.local.entity.UploadSessionEntity
import com.aiavatar.app.core.data.source.local.entity.UploadSessionStatus
import com.aiavatar.app.feature.home.domain.model.SelectedMediaItem
import com.aiavatar.app.feature.home.domain.repository.HomeRepository
import com.aiavatar.app.commons.util.loadstate.LoadState
import com.aiavatar.app.commons.util.loadstate.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class UploadStep2ViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @Deprecated("migrate to repo")
    private val appDatabase: AppDatabase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadStep2State())
    val uiState: StateFlow<UploadStep2State> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UploadStep2UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        /*combine(
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
            val remainingCount = (MAX_IMAGE_COUNT - pickedUriModelList.filterIsInstance<UploadPreviewUiModel.Item>()
                .count { it.selected }).coerceAtLeast(0)
            _uiState.update { state ->
                state.copy(
                    previewModelList = pickedUriModelList,
                    remainingPhotoCount = remainingCount
                )
            }
        }.launchIn(viewModelScope)*/
    }

    fun deletePhoto(uri: Uri) {
        Timber.d("deletePhoto: $uri")
        val newPreviewModelList = uiState.value.previewModelList
            .filterIsInstance<UploadPreviewUiModel.Item>()
            .filterNot { uiModel ->
                uiModel.selectedMediaItem.uri == uri
            }

        setPickedUris(newPreviewModelList, append = false)
    }

    fun restoreUploadSession() = viewModelScope.launch {
        val cachedSessionId: Long = savedStateHandle[KEY_CACHED_UPLOAD_SESSION_ID] ?: return@launch
        val uploadSessionWithFilesEntity = appDatabase.uploadSessionDao().getUploadSessionSync(cachedSessionId)
        uploadSessionWithFilesEntity?.uploadFilesEntity
            ?.map { entity ->
                entity.localUriString.toUri()
            }?.let { pickedUris ->
                setPickedUris(
                    pickedUris.map { UploadPreviewUiModel.Item(SelectedMediaItem(it), selected = true) }
                )
            }
    }

    fun setCachedSessionId(sessionId: Long) {
        savedStateHandle[KEY_CACHED_UPLOAD_SESSION_ID] = sessionId
    }

    fun setFaceDetectionRunning(isRunning: Boolean) {
        _uiState.update { state ->
            state.copy(
                detectingFaces = isRunning
            )
        }
    }

    fun getMaxImages(): Int {
        return uiState.value.remainingPhotoCount
    }

    fun removeDuplicates(
        pickedUris: List<Uri>,
        completion: (removed: Int, normalizedList: List<Uri>) -> Unit,
    ) = viewModelScope.launch {
        val originalList = uiState.value.previewModelList.filterIsInstance<UploadPreviewUiModel.Item>()
            .map { it.selectedMediaItem.uri }
        val combinedUris = originalList.toMutableList().apply {
            addAll(pickedUris)
        }

        val normalizedList = combinedUris.distinct().toMutableList()
        val removed = (combinedUris.size - normalizedList.size).coerceAtLeast(0)
        normalizedList.removeAll(originalList)
        completion(removed, normalizedList)
    }

    fun setPickedUris(newPreviewModelList: List<UploadPreviewUiModel.Item>, append: Boolean = true) {
        // TODO: -done- process the result
        // TODO: clean this sheet!

        var remainingPhotoCount = 0
        val newModelList = uiState.value.previewModelList
            .filterNot { it is UploadPreviewUiModel.Placeholder }
            .toMutableList().apply {
                if (!append) { clear() }
                addAll(newPreviewModelList)
                val selectedCount = count { model ->
                    model is UploadPreviewUiModel.Item && model.selected
                }
                if (selectedCount < MAX_IMAGE_COUNT) {
                    remainingPhotoCount = (MAX_IMAGE_COUNT - selectedCount)
                    add(UploadPreviewUiModel.Placeholder(size))
                }
            }
        _uiState.update { state ->
            state.copy(
                previewModelList = newModelList,
                remainingPhotoCount = remainingPhotoCount
            )
        }
    }

    fun startUpload(context: Context) {
        setLoadState(LoadType.ACTION, LoadState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            var cachedSessionId: Long? = savedStateHandle[KEY_CACHED_UPLOAD_SESSION_ID]
            Timber.d("Cached session id: $cachedSessionId")

            val folderName = StorageUtil.getTempFolderName()
            if (cachedSessionId == null) {
                appDatabase.withTransaction {
                    appDatabase.uploadSessionDao().deleteAllUploadSessions()
                }
                StorageUtil.cleanUp(context)

                val uploadSessionEntity = UploadSessionEntity(
                    createdAt = System.currentTimeMillis(),
                    status = UploadSessionStatus.NOT_STARTED.status,
                    folderName = folderName,
                    trainingType = "unknown"
                )
                cachedSessionId = appDatabase.uploadSessionDao().insert(uploadSessionEntity)
                setCachedSessionId(cachedSessionId)
            }

            val totalUris = uiState.value.previewModelList
                .filterIsInstance<UploadPreviewUiModel.Item>()
                .filter { it.selected }
            val newPickedUris = totalUris
                .map { it.selectedMediaItem.uri }
                .mapNotNull { uri ->
                    val entry =
                        appDatabase.uploadFilesDao().getDeviceFileForUriSync(cachedSessionId, uri.toString())
                    if (entry != null) {
                        null
                    } else {
                        uri
                    }
                }

            Timber.d("Uri Diff: total ${totalUris.size} new ${newPickedUris.size}")

            val savedFileResult = StorageUtil.saveFilesToFolder(
                context,
                folderName = folderName,
                uris = newPickedUris
            )
            if (savedFileResult != null) {
                val uploadFiles = savedFileResult.savedFiles.mapIndexed { index, file ->
                    val fileUri = Uri.fromFile(file)
                    UploadFilesEntity(
                        sessionId = cachedSessionId,
                        fileUriString = fileUri.toString(),
                        localUriString = savedFileResult.originalFiles[index].toString(),
                        status = UploadFileStatus.NOT_STARTED.status,
                        progress = 0,
                    )
                }
                appDatabase.uploadFilesDao().insertAll(uploadFiles)
                setLoadState(LoadType.ACTION, LoadState.NotLoading.Complete)
                sendEvent(UploadStep2UiEvent.PrepareUpload(cachedSessionId))
            } else {
                val cause = IOException("Failed to save files")
                _uiState.update { state ->
                    state.copy(
                        exception = cause,

                    )
                }
                setLoadState(LoadType.ACTION, LoadState.Error(cause))
            }
        }
    }

    private fun setLoadState(
        loadType: LoadType,
        loadState: LoadState,
    ) {
        val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
        _uiState.update { state ->
            state.copy(
                loadState = newLoadState
            )
        }
    }

    private fun sendEvent(newEvent: UploadStep2UiEvent) = viewModelScope.launch {
        _uiEvent.emit(newEvent)
    }
}

data class UploadStep2State(
    val loadState: LoadStates = LoadStates.IDLE,
    val detectingFaces: Boolean = false,
    val previewModelList: List<UploadPreviewUiModel> = emptyList(),
    val remainingPhotoCount: Int = MAX_IMAGE_COUNT,
    val exception: Exception? = null,
    val uiErrorText: UiText? = null
)

interface UploadStep2UiEvent {
    data class PrepareUpload(val sessionId: Long) : UploadStep2UiEvent
}

interface UploadPreviewUiModel {
    data class Item(val selectedMediaItem: SelectedMediaItem, val selected: Boolean) :
        UploadPreviewUiModel
    data class Placeholder(val position: Int) : UploadPreviewUiModel
}

private const val MAX_IMAGE_COUNT = 20

private const val KEY_CACHED_UPLOAD_SESSION_ID = "cached_session_upload_id"
