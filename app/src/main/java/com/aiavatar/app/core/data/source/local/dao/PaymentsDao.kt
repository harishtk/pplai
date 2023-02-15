package com.aiavatar.app.core.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiavatar.app.core.data.source.local.entity.PaymentsEntity
import com.aiavatar.app.core.data.source.local.entity.PaymentsTable
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentsDao {

    @Query("SELECT * FROM ${PaymentsTable.name} ORDER BY ${PaymentsTable.Columns.UPDATED_AT} DESC")
    suspend fun getPaymentsSync(): List<PaymentsEntity>

    @Query("SELECT * FROM ${PaymentsTable.name} ORDER BY ${PaymentsTable.Columns.UPDATED_AT} DESC")
    fun getPaymentsFlow(): Flow<List<PaymentsEntity>>

    @Query("SELECT * FROM ${PaymentsTable.name} " +
            "WHERE ${PaymentsTable.Columns.TRANSACTION_ID} = :transactionId")
    suspend fun getPaymentForTransactionIdSync(transactionId: String): PaymentsEntity?

    @Query("SELECT * FROM ${PaymentsTable.name} " +
            "WHERE ${PaymentsTable.Columns.TRANSACTION_ID} = :transactionId")
    fun getPaymentForTransactionId(transactionId: String): Flow<PaymentsEntity?>

    @Query("UPDATE ${PaymentsTable.name} " +
            "SET ${PaymentsTable.Columns.STATUS} = :status, " +
            "${PaymentsTable.Columns.UPDATED_AT} = :updatedAt " +
            "WHERE ${PaymentsTable.Columns.TRANSACTION_ID} = :transactionId")
    suspend fun updateStatus(transactionId: String, status: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paymentEntities: List<PaymentsEntity>)

    @Query("DELETE FROM ${PaymentsTable.name}")
    suspend fun deleteAll()
}