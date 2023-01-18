package com.aiavatar.app.core.data.source.remote.dto

import com.aiavatar.app.core.domain.model.request.AvatarStatusRequest
import com.google.gson.annotations.SerializedName

data class AvatarStatusRequestDto(
    @SerializedName("id")
    val id: String
)

fun AvatarStatusRequest.asDto(): AvatarStatusRequestDto {
    return AvatarStatusRequestDto(id = id)
}

