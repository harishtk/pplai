package com.pepul.app.pepulliv.feature.stream.presentation.publish

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.pepul.app.pepulliv.R
import com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit.HaishinKitPublishFragment

class PublishFragment : Fragment() {

    private var sdkType: Int = SDK_TYPE_NO_SDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdkType = arguments?.getInt(ARG_SDK_TYPE)
            ?: SDK_TYPE_NO_SDK
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_publish, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragment = when (sdkType) {
            SDK_TYPE_NO_SDK -> DefaultPublishFragment().apply {
                this.arguments = this@PublishFragment.arguments
            }
            SDK_TYPE_WOWZA -> WowzaPublishFragment().apply {
                this.arguments = this@PublishFragment.arguments
            }
            SDK_TYPE_HAISHINKIT -> HaishinKitPublishFragment().apply {
                this.arguments = this@PublishFragment.arguments
            }
            else -> error("Invalid sdk type $sdkType")
        }

        childFragmentManager.beginTransaction()
            .add(R.id.fragment_container_view, fragment)
            .commit()
    }

    companion object {
        const val SDK_TYPE_NO_SDK = 0
        const val SDK_TYPE_WOWZA = 1
        const val SDK_TYPE_HAISHINKIT = 3

        const val ARG_SDK_TYPE = "com.pepul.com.pepullive.args.SDK_TYPE"
    }
}