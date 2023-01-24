package com.aiavatar.app.feature.home.presentation.forceupdate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.aiavatar.app.BuildConfig
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentForceUpdateBinding
import com.aiavatar.app.showToast
import timber.log.Timber

class ForceUpdateFragment : Fragment() {

    private val appStoreLink: String = "market://details?id=${BuildConfig.APPLICATION_ID}"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_force_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentForceUpdateBinding.bind(view)

        binding.apply {
            btnDownload.setOnClickListener {
                gotoMarket()
            }
        }


    }

    private fun gotoMarket() {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, appStoreLink.toUri())
            if (marketIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(marketIntent)
            } else {
                throw IllegalStateException("Unable to query for market")
            }
        } catch (e: Exception) {
            context?.showToast("Unable to perform this action!")
            Timber.e(e)
        }
    }
}