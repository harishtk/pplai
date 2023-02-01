package com.aiavatar.app.feature.home.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.CatalogDetailRequest
import com.aiavatar.app.feature.home.domain.model.request.GenerateAvatarRequest
import com.aiavatar.app.feature.home.domain.model.request.GetAvatarsRequest
import com.aiavatar.app.feature.home.domain.model.request.SubscriptionPurchaseRequest
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    fun getCatalog(): Flow<Result<List<Category>>>

    fun getCatalogList(request: CatalogDetailRequest): Flow<Result<CatalogDetailData>>

    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>

    fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<PurchasePlanData>>

    fun generateAvatar(generateAvatarRequest: GenerateAvatarRequest): Flow<Result<Long>>

    fun getMyModels(): Flow<Result<List<ModelList>>>

    fun getModel(modelId: String): Flow<Result<ModelData>>

    fun getModel2(modelId: String): Flow<Result<ModelData>>

    fun getMyModels2(forceRefresh: Boolean): Flow<Result<List<ModelData>>>

    fun getAvatars(getAvatarsRequest: GetAvatarsRequest): Flow<Result<List<ListAvatar>>>

    fun getCatalog2(forceRefresh: Boolean): Flow<Result<List<Category>>>

    fun getCatalogList2(request: CatalogDetailRequest, forceRefresh: Boolean): Flow<Result<CatalogDetailData>>
}