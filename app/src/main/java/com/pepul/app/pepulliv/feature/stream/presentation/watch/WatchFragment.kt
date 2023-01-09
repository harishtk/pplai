package com.pepul.app.pepulliv.feature.stream.presentation.watch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pepul.app.pepulliv.R

class WatchFragment : Fragment() {

    private var playbackType: Int = PLAYBACK_TYPE_EXOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playbackType = arguments?.getInt(ARG_PLAYBACK_TYPE)
            ?: PLAYBACK_TYPE_EXOP
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_container_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val f = when (playbackType) {
            PLAYBACK_TYPE_EXOP -> ExoPWatchFragment().apply {
                this.arguments = this@WatchFragment.arguments
            }
            PLAYBACK_TYPE_HAISHINKIT -> HaishinKitWatchFragment().apply {
                this.arguments = this@WatchFragment.arguments
            }
            else -> error("Unknown playback type $playbackType")
        }

        childFragmentManager.beginTransaction()
            .add(R.id.fragment_container_view, f)
            .commit()
    }

    companion object {
        const val PLAYBACK_TYPE_EXOP = 0
        const val PLAYBACK_TYPE_HAISHINKIT = 1 // haishinkit doesn't have hls playback

        const val ARG_PLAYBACK_TYPE = "com.pepul.com.pepullive"
    }
}