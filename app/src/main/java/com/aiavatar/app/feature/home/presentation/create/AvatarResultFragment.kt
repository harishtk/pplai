package com.aiavatar.app.feature.home.presentation.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentAvatarResultBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AvatarResultFragment : Fragment() {

    private val viewModel: AvatarResultViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_avatar_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAvatarResultBinding.bind(view)

        binding.bindState()
    }

    private fun FragmentAvatarResultBinding.bindState() {

    }
}