package com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pepul.app.pepulliv.Constant.MIME_TYPE_JPEG
import com.pepul.app.pepulliv.Constant.STREAM_STATE_PUBLISHING
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STARTING
import com.pepul.app.pepulliv.Constant.STREAM_STATE_STOPPED
import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.commons.util.UiText
import com.pepul.app.pepulliv.commons.util.net.ProgressRequestBody
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.feature.stream.domain.model.CommentItem
import com.pepul.app.pepulliv.feature.stream.domain.model.request.StreamIdRequest
import com.pepul.app.pepulliv.feature.stream.domain.model.request.UploadThumbnailRequest
import com.pepul.app.pepulliv.feature.stream.domain.repository.StreamRepository
import com.pepul.app.pepulliv.feature.stream.presentation.publish.DefaultPublishFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import hilt_aggregated_deps._com_pepul_app_pepulliv_feature_stream_presentation_watch_ExoPWatchFragment_GeneratedInjector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HaishinKitPubViewModel @Inject constructor(
    private val repository: StreamRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(HaishinKitPublishState())
    val uiState: StateFlow<HaishinKitPublishState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HaishinKitPublishUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (HaishinKitPublishUiAction) -> Unit

    private var startPublishJob: Job? = null
    private var stopPublishJob: Job? = null

    private var streamStateFetchJob: Job? = null
    private var liveCommentsHideJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: HaishinKitPublishUiAction) {
        when (action) {
            is HaishinKitPublishUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
        }
    }

    fun subscriberJoined(payload: JSONObject) {
        val liveComments = uiState.value.liveComment.toMutableList()
        val userId = payload.getString("userId")
        val username = if (ApplicationDependencies.getPersistentStore().userId == userId) {
            "You"
        } else {
            if (payload.has("userName")) {
                payload.getString("userName")
            } else {
                userId
            }
        }
        val commentItem = CommentItem(
            userId = username,
            content = "joined the stream",
            postedAt = System.currentTimeMillis()
        )
        liveComments.add(CommentsUiModel.Comment(commentItem, expired = false))
        _uiState.update { state ->
            state.copy(
                liveComment = liveComments
            )
        }
        Timber.d("Comments: ${uiState.value.liveComment}")
    }

    fun subscriberLeft(payload: JSONObject) {
        val liveComments = uiState.value.liveComment.toMutableList()
        val userId = payload.getString("userId")
        val username = if (ApplicationDependencies.getPersistentStore().userId == userId) {
            "You"
        } else {
            if (payload.has("userName")) {
                payload.getString("userName")
            } else {
                userId
            }
        }
        val commentItem = CommentItem(
            userId = username,
            content = "left the stream",
            postedAt = System.currentTimeMillis()
        )
        liveComments.add(CommentsUiModel.Comment(commentItem, expired = false))
        _uiState.update { state ->
            state.copy(
                liveComment = liveComments
            )
        }
        Timber.d("Comments: ${uiState.value.liveComment}")
    }

    fun commentReceived(payload: JSONObject) {
        val liveComments = uiState.value.liveComment.toMutableList()
        val userId = payload.getString("userId")
        val username = if (ApplicationDependencies.getPersistentStore().userId == userId) {
            "You"
        } else {
            if (payload.has("userName")) {
                payload.getString("userName")
            } else {
                userId
            }
        }
        val comment = if (payload.has("comment")) {
            payload.getString("comment")
        } else {
            ""
        }
        val commentItem = CommentItem(
            userId = username,
            content = comment,
            postedAt = System.currentTimeMillis()
        )
        liveComments.add(CommentsUiModel.Comment(commentItem, expired = false))
        _uiState.update { state ->
            state.copy(
                liveComment = liveComments
            )
        }
        Timber.d("Comments: ${uiState.value.liveComment}")
    }

    fun startPublish(id: String) {
        if (startPublishJob?.isActive == true) {
            val cause = IllegalStateException("A start publish job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        startPublishJob?.cancel(CancellationException("New Request")) // just in case
        val request = StreamIdRequest(id)
        startPublishJob = repository.startStream(request)
            .onEach { result ->
                Timber.d("Start Stream: $result")
                when (result) {
                    is Result.Success -> {
                        scheduleStreamStateInternal(id)
                    }
                    is Result.Error -> {
                        // Noop
                    }
                    is Result.Loading -> {
                        // Noop
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopPublish(id: String) {
        if (stopPublishJob?.isActive == true) {
            val cause = IllegalStateException("A stop publish job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        streamStateFetchJob?.cancel(CancellationException("Stream stop request received"))
        stopPublishJob?.cancel(CancellationException("New Request")) // just in case

        val request = StreamIdRequest(id)
        stopPublishJob = repository.stopStream(request)
            .onEach { result ->
                Timber.d("Stop Stream: $result")
                when (result) {
                    is Result.Error -> {
                        // TODO: handle unexpected errors
                        getStreamState(request)
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                streamState = STREAM_STATE_STOPPED,
                                recordButtonState = LiveRecordButtonState.IDLE
                            )
                        }
                    }
                    else -> { /* Noop */ }
                }
            }
            .launchIn(viewModelScope)
    }

    fun startedPublishing() {
        _uiState.update { state ->
            state.copy(
                streamState = STREAM_STATE_PUBLISHING,
                recordButtonState = LiveRecordButtonState.RECORDING
            )
        }
        scheduleLiveCommentsJob()
    }

    fun updateLiveRecordButtonState(newState: LiveRecordButtonState) {
        _uiState.update { state ->
            state.copy(
                recordButtonState = newState
            )
        }
    }

     fun uploadThumbnail(
        file: File,
        streamName: String,
        fileName: String = file.name,
        type: String = "thumbnail",
        userId: String = "",
        progressCallback: (progress: Float) -> Unit = {}
    ) {
         Timber.d("Uploading: $fileName Preparing upload..")
         val progressRequestBody = ProgressRequestBody(
             file,
             MIME_TYPE_JPEG,
             object : ProgressRequestBody.ProgressCallback {
                 override fun onProgressUpdate(percentage: Int) {
                     Timber.d("Uploading: $fileName PROGRESS $percentage")
                     progressCallback((percentage / 10f).coerceIn(0.0F, 1.0F))
                 }

                 override fun onError() {

                 }

             }
         )
         val filePart: MultipartBody.Part =
             MultipartBody.Part.createFormData(
                 "file",
                 fileName,
                 progressRequestBody
             )
         val typePart = MultipartBody.Part.createFormData(
             "type",
             type
         )
         val streamNamePart = MultipartBody.Part.createFormData(
             "streamName",
             streamName
         )
         val userIdPart = MultipartBody.Part.createFormData(
             "userId",
             ""
         )

         val thumbnailRequest = UploadThumbnailRequest(
             file = filePart,
             type = typePart,
             streamName = streamNamePart,
             userId = userIdPart
         )

         viewModelScope.launch {
             val result = repository.uploadStreamThumbnailSync(thumbnailRequest)
             Timber.d("Upload Thumbnail: result $result")
         }
    }

    private fun scheduleStreamStateInternal(id: String) {
        if (streamStateFetchJob?.isActive == true) {
            val cause = IllegalStateException("A state fetch job is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        streamStateFetchJob?.cancel(CancellationException("New request")) // just in case
        val request = StreamIdRequest(id)
        getStreamState(request)
        scheduleNewStateFetchJob(id)
    }

    private fun getStreamState(request: StreamIdRequest) {
        repository.getStreamState(request)
            .onEach { result ->
                Timber.d("Stream state: result $result")
                when (result) {
                    is Result.Success -> {
                        Timber.d("Stream state: ${result.data}")
                        _uiState.update { state ->
                            state.copy(streamState = result.data.state)
                        }
                        if (result.data.state == STREAM_STATE_STARTING) {
                            scheduleNewStateFetchJob(request.id)
                            updateLiveRecordButtonState(LiveRecordButtonState.PREPARING)
                        } else if (result.data.state == STREAM_STATE_STOPPED) {
                            updateLiveRecordButtonState(LiveRecordButtonState.IDLE)
                        }
                    }
                    is Result.Error -> {
                        scheduleNewStateFetchJob(request.id)
                    }
                    else -> {
                        // Noop
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun scheduleNewStateFetchJob(id: String) {
        streamStateFetchJob?.cancel(CancellationException("New request")) // just in case
        val request = StreamIdRequest(id)
        streamStateFetchJob = viewModelScope.launch {
            delay(1000)
            getStreamState(request)
        }
    }

    private fun scheduleLiveCommentsJob() {
        liveCommentsHideJob?.cancel(CancellationException("New request"))
        liveCommentsHideJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val updatedComments = uiState.value.liveComment
                    .filter { model -> model is CommentsUiModel.Comment && !model.expired }
                    .map { model ->
                    if (model is CommentsUiModel.Comment) {
                        val commentItem = model.commentItem
                        val expired = (System.currentTimeMillis() - commentItem.postedAt) >= LIVE_COMMENT_HIDE_DELAY
                        return@map CommentsUiModel.Comment(commentItem, expired)
                    }
                    model
                }
                _uiState.update { state ->
                    state.copy(
                        liveComment = updatedComments
                    )
                }
            }
        }
    }

    override fun onCleared() {
        val t = CancellationException("View Model is dead")
        streamStateFetchJob?.cancel(t)
        liveCommentsHideJob?.cancel(t)
        super.onCleared()
    }

    val STREAM_STATE_FETCH_DELAY = 1000L

}

data class HaishinKitPublishState(
    val streamState: String = DEFAULT_STREAM_STATE,
    val recordButtonState: LiveRecordButtonState = LiveRecordButtonState.IDLE,
    val liveComment: List<CommentsUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface HaishinKitPublishUiAction {
    data class ErrorShown(val e: Exception) : HaishinKitPublishUiAction
}

interface HaishinKitPublishUiEvent {
    data class ShowToast(val message: UiText) : HaishinKitPublishUiEvent
}

interface CommentsUiModel {
    data class Comment(val commentItem: CommentItem, val expired: Boolean) : CommentsUiModel
}

enum class LiveRecordButtonState { RECORDING, PREPARING, IDLE }

private const val DEFAULT_STREAM_STATE = "new"
const val LIVE_COMMENT_HIDE_DELAY = 5000L
