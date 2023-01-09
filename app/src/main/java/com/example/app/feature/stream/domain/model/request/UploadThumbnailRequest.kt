package com.example.app.feature.stream.domain.model.request

import okhttp3.MultipartBody
import retrofit2.http.Part

data class UploadThumbnailRequest(
    @Part val file: MultipartBody.Part,
    @Part val type: MultipartBody.Part,
    @Part val streamName: MultipartBody.Part,
    @Part val userId: MultipartBody.Part,
)
