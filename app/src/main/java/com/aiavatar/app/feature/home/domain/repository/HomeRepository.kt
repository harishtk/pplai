package com.aiavatar.app.feature.home.domain.repository

import com.aiavatar.app.commons.util.Result
import com.aiavatar.app.feature.home.data.model.ModelListWithModelEntity
import com.aiavatar.app.feature.home.data.source.local.entity.ModelAvatarEntity
import com.aiavatar.app.feature.home.data.source.remote.dto.VerifyCouponDataDto
import com.aiavatar.app.feature.home.domain.model.*
import com.aiavatar.app.feature.home.domain.model.request.*
import dagger.Reusable
import kotlinx.coroutines.flow.Flow

interface HomeRepository {

    fun getCatalog(): Flow<Result<List<Category>>>

    fun getCatalogList(request: CatalogDetailRequest): Flow<Result<CatalogDetailData>>

    fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>>

    fun purchasePlan(subscriptionPurchaseRequest: SubscriptionPurchaseRequest): Flow<Result<PurchasePlanData>>

    fun generateAvatar(generateAvatarRequest: GenerateAvatarRequest): Flow<Result<Long>>

    fun getMyModels(forceRefresh: Boolean): Flow<Result<List<ModelListWithModel>>>

    fun getModel(modelId: String): Flow<Result<ModelData>>

    fun getModel2(modelId: String): Flow<Result<ModelData>>

    fun getMyModels2(forceRefresh: Boolean): Flow<Result<List<ModelData>>>

    fun getAvatars(getAvatarsRequest: GetAvatarsRequest): Flow<Result<List<ListAvatar>>>

    fun getAvatars2(getAvatarsRequest: GetAvatarsRequest, forceRefresh: Boolean): Flow<Result<List<ModelAvatar>>>

    @Deprecated("not used")
    suspend fun getAvatars2Sync(getAvatarsRequest: GetAvatarsRequest): Result<List<ModelAvatar>>

    fun getCatalog2(forceRefresh: Boolean): Flow<Result<List<Category>>>

    fun getCatalogList2(request: CatalogDetailRequest, forceRefresh: Boolean): Flow<Result<CatalogDetailData>>

    fun subscriptionLog(request: SubscriptionLogRequest): Flow<Result<String>>

    fun verifyCoupon(request: VerifyCouponRequest): Flow<Result<VerifyCouponData>>
}