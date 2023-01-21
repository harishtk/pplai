package com.aiavatar.app.feature.home.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.home.data.source.remote.model.ListAvatarDto
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    fun getCatalog(): Flow<Result<List<Category>>>

    fun getCatalogDetail(request: CatalogDetailRequest): Flow<Result<CatalogDetailData>>

    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>

    fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<String>>

    fun generateAvatar(generateAvatarRequest: GenerateAvatarRequest): Flow<Result<Long>>

    fun getMyModels(): Flow<Result<List<ModelList>>>

    fun getAvatars(getAvatarsRequest: GetAvatarsRequest): Flow<Result<List<ListAvatar>>>

}