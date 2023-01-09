package com.pepul.app.pepulliv.commons.util

data class PagedData<T>(
    val data: List<T>,
    val totalCount: Int,
    val prevKey: String?,
    val nextKey: String?
)