package com.aiavatar.app.feature.home.presentation.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.R
import com.aiavatar.app.commons.util.AnimationUtil.touchInteractFeedback
import com.aiavatar.app.commons.util.HapticUtil
import com.aiavatar.app.databinding.FragmentFeedbackBinding
import com.aiavatar.app.showToast
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates


/**
 * @author Hariskumar Kubendran
 * @date 01/03/23
 * Pepul Tech
 * hariskumar@pepul.com
 */
class FeedbackFragment : Fragment() {

    private val viewModel: FeedbackViewModel by viewModels()

    private val chipChoiceSet1 = listOf(
        "Efficiency",
        "Safety & Privacy",
        "Positive Content",
        "Other"
    )
    private val chipChoiceSet2 = listOf(
        "New Features",
        "UI/UX",
        "Performance",
        "Content",
        "Other"
    )
    private val chipChoiceSet3 = listOf(
        "Features",
        "Idea",
        "Data Consumption",
        "Bug",
        "Other"
    )

    private val tooltipTitles = listOf(
        "I'm Not Happy",
        "I'm OK",
        "I'm Happy"
    )

    private val emojiIds = listOf(
        R.drawable.smiley_sad,
        R.drawable.smiley_happyface,
        R.drawable.smiley_thumbs_up
    )

    private val chipChoiceTitles = listOf(
        "I'm happy with the app",
        "I'm OK, can be better with",
        "Area of opportunities",
    )

    private val feedbackStateList = listOf<FeedbackDescState>(
        FeedbackDescState(
            emojiId = emojiIds[0],
            tooltipTitle = tooltipTitles[0],
            chipChoiceTitle = chipChoiceTitles[0],
            chipChoiceSet = chipChoiceSet1
        ),
        FeedbackDescState(
            emojiId = emojiIds[1],
            tooltipTitle = tooltipTitles[1],
            chipChoiceTitle = chipChoiceTitles[1],
            chipChoiceSet = chipChoiceSet2
        ),
        FeedbackDescState(
            emojiId = emojiIds[2],
            tooltipTitle = tooltipTitles[2],
            chipChoiceTitle = chipChoiceTitles[2],
            chipChoiceSet = chipChoiceSet3
        )
    )

    private var lastSlideValue by Delegates.notNull<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentFeedbackBinding.bind(view)

        lastSlideValue = savedInstanceState?.getFloat("last_slide_value", DEFAULT_SLIDER_VALUE)
            ?: DEFAULT_SLIDER_VALUE

        binding.bindState(
            uiState = viewModel.uiState,
            uiEvent = viewModel.uiEvent
        )
    }

    private fun FragmentFeedbackBinding.bindState(
        uiState: StateFlow<FeedbackState>,
        uiEvent: SharedFlow<FeedbackUiEvent>
    ) {
        uiEvent.onEach { event ->
            when (event) {
                is FeedbackUiEvent.ShowToast -> {
                    context?.showToast(event.message.asString(requireContext()))
                }
            }
        }.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)

        setup(feedbackStateList[1])
        slider.addOnChangeListener { _, slideValue, fromUser ->
            Timber.d("Slider: delta = $slideValue")
            when {
                slideValue <= 0.33 -> {
                    if (lastSlideValue != 0.33F) {
                        setup(feedbackStateList[0])
                    }
                    lastSlideValue = 0.33F
                }
                slideValue <= 0.66F -> {
                    if (lastSlideValue != 0.66F) {
                        setup(feedbackStateList[1])
                    }
                    lastSlideValue = 0.66F
                }
                else -> {
                    if (lastSlideValue != 1.0F) {
                        setup(feedbackStateList[2])
                    }
                    lastSlideValue = 1.0F
                }
            }
            Timber.d("Slider: lastSlide = $lastSlideValue")
        }

        /*nextButton.setOnClickListener {
            if (reportChipGroup.checkedChipId != View.NO_ID) {
                tvError.isVisible = false
                tvError.text = null

                selectedMessage = reportChipGroup.findViewById<Chip>(reportChipGroup.checkedChipId)?.text?.toString().nullAsEmpty()
                sendReport(selectedMessage!!)
            } else {
                tvError.isVisible = true
                tvError.text = getString(R.string.please_select_an_option)
                nextButton.shakeNow()
            }
        }*/

        bindClick()

        bindToolbar()

        bindInput()
    }

    private fun FragmentFeedbackBinding.bindClick() {
        tvMaybeLater.setOnClickListener { findNavController().navigateUp() }
    }

    private fun FragmentFeedbackBinding.setup(feedbackDescState: FeedbackDescState) {
        // Reset
        chipGroup.removeAllViews()

        feedbackDescState.chipChoiceSet.forEach { chipTitle ->
            val chip = LayoutInflater.from(root.context)
                .inflate(R.layout.outlined_chip, chipGroup, false) as? Chip
            chip?.apply {
                id = ViewCompat.generateViewId()
                text = chipTitle
                chipGroup.addView(this)
            }
        }

        ivBigEmoji.setImageResource(feedbackDescState.emojiId)
        chipContainerTitle.text = feedbackDescState.chipChoiceTitle

        popupCaption.textView1.text = feedbackDescState.tooltipTitle
        popupCaption.root.touchInteractFeedback(scaleMultiplier = 1.1F)
        HapticUtil.createOneShot(requireContext())
    }

    private fun FragmentFeedbackBinding.bindInput() {
        val maxFeedbackCommentLength = resources.getInteger(R.integer.max_feedback_comment_length)
        edExperience.addTextChangedListener {
            val typedString = it.toString()
            edCharacterCounter.text = "${typedString.trim().length} / $maxFeedbackCommentLength"
        }
    }

    private fun FragmentFeedbackBinding.bindToolbar() {
        toolbarIncluded.apply {
            toolbarTitle.isVisible = false
            toolbarNavigationIcon.isVisible = true
            toolbarNavigationIcon.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("last_slide_value", lastSlideValue)
    }

    data class FeedbackDescState(
        @DrawableRes val emojiId: Int,
        val tooltipTitle: String,
        val chipChoiceTitle: String,
        val chipChoiceSet: List<String>,
    )

    companion object {
        private const val DEFAULT_SLIDER_VALUE: Float = 0.66F
    }
}