package com.aiavatar.app.core.data.source.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aiavatar.app.core.data.source.local.AppDatabase
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = PaymentsTable.name,
    indices = [
        Index(
            name = "transaction_id_index",
            value = [PaymentsTable.Columns.TRANSACTION_ID],
            unique = true
        )
    ]
)
data class PaymentsEntity(
    @ColumnInfo("transaction_id")
    val transactionId: String,
    @ColumnInfo("status")
    val status: String,
    @ColumnInfo("purchase_token")
    val purchaseToken: String,
    @ColumnInfo("product_sku")
    val productSku: String,
    @ColumnInfo("created_at")
    val createdAt: Long,
    @ColumnInfo("updated_at")
    val updatedAt: Long
) {
    @ColumnInfo("id")
    @PrimaryKey(autoGenerate = true)
    var _id: Long? = null
}

const val PAYMENT_STATUS_INITIALIZING   = "initializing"
const val PAYMENT_STATUS_PROCESSING     = "processing"
const val PAYMENT_STATUS_PENDING        = "pending"
const val PAYMENT_STATUS_FAILED         = "failed"
const val PAYMENT_STATUS_CANCELED       = "canceled"
const val PAYMENT_STATUS_COMPLETE       = "complete"

object PaymentsTable {
    const val name: String = AppDatabase.TABLE_PAYMENTS

    object Columns {
        const val TRANSACTION_ID            = "transaction_id"
        const val STATUS                    = "status"
        const val PURCHASE_TOKEN            = "purchase_token"
        const val PRODUCT_SKU               = "product_sku"
        const val CREATED_AT                = "created_at"
        const val UPDATED_AT                = "updated_at"
    }
}