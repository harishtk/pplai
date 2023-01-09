package com.pepul.app.pepulliv.feature.stream.presentation.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.pepul.app.pepulliv.commons.util.Result
import com.pepul.app.pepulliv.commons.util.UiText
import com.pepul.app.pepulliv.commons.util.loadstate.LoadType
import com.pepul.app.pepulliv.commons.util.net.ApiException
import com.pepul.app.pepulliv.commons.util.net.NoInternetException
import com.pepul.app.pepulliv.commons.util.succeeded
import com.pepul.app.pepulliv.di.ApplicationDependencies
import com.pepul.app.pepulliv.feature.stream.domain.model.CommentItem
import com.pepul.app.pepulliv.feature.stream.domain.model.request.StreamIdRequest
import com.pepul.app.pepulliv.feature.stream.domain.repository.StreamRepository
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.CommentsUiModel
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.LIVE_COMMENT_HIDE_DELAY
import com.pepulnow.app.data.LoadState
import com.pepulnow.app.data.LoadStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WatchStreamViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchStreamState>(WatchStreamState())
    val uiState: StateFlow<WatchStreamState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<WatchStreamUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val accept: (WatchStreamUiAction) -> Unit

    private var getStreamInfoFetchJob: Job? = null
    private var liveCommentsHideJob: Job? = null

    init {
        accept = { uiAction -> onUiAction(uiAction) }
    }

    private fun onUiAction(action: WatchStreamUiAction) {
        when (action) {
            is WatchStreamUiAction.ErrorShown -> {
                _uiState.update { state ->
                    state.copy(
                        exception = null,
                        uiErrorMessage = null
                    )
                }
            }
            is WatchStreamUiAction.SendComment -> {
                sendCommentInternal(action.typed, action.streamId)
            }
        }
    }

    fun subscriberJoined(payload: JSONObject) {
        val liveComments = uiState.value.liveComment.toMutableList()
        val userId = payload.getString("userId")
        if (ApplicationDependencies.getPersistentStore().userId == userId) {
            // This is own user
            return
        }
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
        if (ApplicationDependencies.getPersistentStore().userId == userId) {
            // This is own user
            return
        }
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

    private fun sendCommentInternal(comment: String, streamId: String) {
        val payload = JsonObject().apply {
            addProperty("userId", ApplicationDependencies.getPersistentStore().userId)
            addProperty("comment", comment)
            addProperty("streamId", streamId)
        }
        ApplicationDependencies.getAppWebSocket().messageBroker
            .commentStream(payload.toString())
    }

    fun getStreamInfo(id: String) {
        getStreamInfoInternal(id)
    }

    private fun getStreamInfoInternal(id: String) {
        if (getStreamInfoFetchJob?.isActive == true) {
            val cause = IllegalStateException("A stream info request is already active. Ignoring request")
            Timber.d(cause)
            return
        }
        getStreamInfoFetchJob?.cancel(CancellationException("New request")) // just in case
        val request = StreamIdRequest(id)
        getStreamInfoFetchJob = viewModelScope.launch {
            repository.getStreamInfo(request).collectLatest { result ->
                when (result) {
                    is Result.Loading -> setLoadState(LoadState.Loading(), LoadType.REFRESH)
                    is Result.Error -> {
                        when (result.exception) {
                            is ApiException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.somethingWentWrong
                                    )
                                }
                            }
                            is NoInternetException -> {
                                _uiState.update { state ->
                                    state.copy(
                                        exception = result.exception,
                                        uiErrorMessage = UiText.noInternet
                                    )
                                }
                            }
                        }
                        setLoadState(LoadState.Error(result.exception), LoadType.REFRESH)
                    }
                    is Result.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                playbackUrl = result.data.hlsPlaybackUrl ?: ""
                            )
                        }
                        setLoadState(LoadState.NotLoading.Complete, LoadType.REFRESH)
                        scheduleLiveCommentsJob()
                    }
                }
            }
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

    private fun setLoadState(
        loadState: LoadState,
        loadType: LoadType
    ) {
       val newLoadState = uiState.value.loadState.modifyState(loadType, loadState)
       _uiState.update { state -> state.copy(loadState = newLoadState) }
    }

    override fun onCleared() {
        val t = CancellationException("View Model is dead")
        getStreamInfoFetchJob?.cancel(t)
        liveCommentsHideJob?.cancel(t)
        super.onCleared()
    }
}

data class WatchStreamState(
    val loadState: LoadStates = LoadStates.IDLE,
    val playbackUrl: String = DEFAULT_PLAYBACK_URL,
    val liveComment: List<CommentsUiModel> = emptyList(),
    val exception: Exception? = null,
    val uiErrorMessage: UiText? = null
)

interface WatchStreamUiAction {
    data class ErrorShown(val e: Exception) : WatchStreamUiAction
    data class SendComment(val typed: String, val streamId: String) : WatchStreamUiAction
}

interface WatchStreamUiEvent {
    data class ShowToast(val message: UiText) : WatchStreamUiEvent
}

private const val DEFAULT_PLAYBACK_URL = ""