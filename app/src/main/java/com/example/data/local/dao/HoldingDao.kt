package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.HoldingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holding_records ORDER BY id ASC")
    fun getAllHoldings(): Flow<List<HoldingRecordEntity>>

    @Query("SELECT * FROM holding_records WHERE holdingId = :holdingId LIMIT 1")
    suspend fun getHoldingById(holdingId: String): HoldingRecordEntity?

    @Query("SELECT * FROM holding_records WHERE hasCalculationMismatch = 1 OR hasMissingValues = 1 OR isDuplicate = 1")
    fun getInconsistentHoldings(): Flow<List<HoldingRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holdings: List<HoldingRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holding: HoldingRecordEntity): Long

    @Update
    suspend fun update(holding: HoldingRecordEntity)

    @Query("DELETE FROM holding_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM holding_records")
    suspend fun getCount(): Int
}
