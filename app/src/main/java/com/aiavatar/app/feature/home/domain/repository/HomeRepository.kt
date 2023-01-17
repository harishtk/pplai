package com.aiavatar.app.feature.home.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.home.domain.model.Avatar
import com.aiavatar.app.feature.home.domain.model.SubscriptionPlan
import com.aiavatar.app.feature.home.domain.model.UserAndCategory
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    fun getCatalog(): Flow<Result<UserAndCategory>>

    fun getAvatars(): Flow<Result<List<Avatar>>>

    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>

}