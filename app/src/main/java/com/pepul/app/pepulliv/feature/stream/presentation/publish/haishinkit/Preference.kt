package com.pepul.app.pepulliv.feature.stream.presentation.publish.haishinkit

data class Preference(var rtmpURL: String, var streamName: String) {
    companion object {
        var shared = Preference(
            "",
            ""
        )

        var useSurfaceView: Boolean = true

        fun clear() {
            shared = Preference("", "")
        }
    }
}
