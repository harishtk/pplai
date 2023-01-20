package com.aiavatar.app.feature.home.presentation.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aiavatar.app.R
import com.aiavatar.app.databinding.FragmentSubscriptionBinding
import com.aiavatar.app.databinding.FragmentSubscriptionSuccessBinding

class SubscriptionSuccessFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_subscription_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSubscriptionSuccessBinding.bind(view)

        // TODO: bind state
    }


}