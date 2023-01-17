package com.aiavatar.app.feature.onboard.presentation.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentWalthrough1Binding
import com.aiavatar.app.makeColoredSubstring
import java.util.Collections

class WalkThroughContent1 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_walthrough_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalthrough1Binding.bind(view)

        binding.bindState()
    }

    private fun FragmentWalthrough1Binding.bindState() {
        val spacingPx = resources.getDimensionPixelSize(R.dimen.spacer_size_small)
        val topDownAnimation = TranslateAnimation(0f, 0f, 0f,  spacingPx.toFloat()).apply {
            duration = 2000L
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }

        val span = makeColoredSubstring(
            requireContext(),
            getString(R.string.walkthrough_big_des_1),
            Collections.singletonList(getString(R.string.walkthrough_big_des_1_multicolor))
        )
        bigDescription1.setText(span, TextView.BufferType.SPANNABLE)

        col1.startAnimation(topDownAnimation)
        col3.startAnimation(topDownAnimation)

        val bottomUpAnimation = TranslateAnimation(0f, 0f, 0f, -spacingPx.toFloat()).apply {
            duration = 2000L
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        col2.postDelayed({col2.startAnimation(bottomUpAnimation)}, 500L)

    }
}