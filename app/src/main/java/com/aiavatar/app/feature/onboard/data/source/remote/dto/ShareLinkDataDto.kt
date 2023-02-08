package com.aiavatar.app.feature.onboard.data.source.remote.dto

import com.aiavatar.app.feature.onboard.domain.model.ShareLinkData
import com.google.gson.annotations.SerializedName

data class ShareLinkDataDto(
    @SerializedName("shortLink")
    val shortLink: String
)

fun ShareLinkDataDto.toShareLinkData(): ShareLinkData {
    return ShareLinkData(
        shortLink = shortLink
    )
}

