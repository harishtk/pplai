package com.pepul.app.pepulliv.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.pepul.app.pepulliv.R

class LiveRecordButton : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context,
        attrs,
        defStyleAttr)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private var outlineImage: ImageView
    private var centerImage: ImageView

    private var isRecording: Boolean = false

    init {
        val contentView = inflate(context, R.layout.live_record_button, this)

        outlineImage = contentView.findViewById(R.id.outline_image)
        centerImage = contentView.findViewById(R.id.center_image)
    }

    fun setRecording(recording: Boolean) {
        if (isRecording && recording) { return; }
        this.isRecording = recording
        updateState(LiveRecordButtonState.RECORDING)
    }

    private fun updateState(liveRecordButtonState: LiveRecordButtonState) {
        when (liveRecordButtonState) {
            LiveRecordButtonState.RECORDING -> TODO()
            LiveRecordButtonState.PREPARING -> TODO()
            LiveRecordButtonState.IDLE -> {
                centerImage.setColorFilter(resources.getColor(android.R.color.holo_red_light))
            }
        }
    }

    enum class LiveRecordButtonState { RECORDING, PREPARING, IDLE }

}