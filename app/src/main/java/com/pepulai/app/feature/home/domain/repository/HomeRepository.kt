package com.pepulai.app.feature.home.domain.repository

import com.pepulai.app.commons.util.Result
import com.pepulai.app.feature.home.domain.model.Avatar
import com.pepulai.app.feature.home.domain.model.UserAndCategory
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    fun getCatalog(): Flow<Result<UserAndCategory>>

    fun getAvatars(): Flow<Result<List<Avatar>>>

}