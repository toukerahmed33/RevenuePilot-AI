package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holding_records")
data class HoldingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val holdingId: String,
    val ownerName: String = "",
    val ward: String = "",
    val area: String = "",
    val previousArrears: Double = 0.0,
    val currentDemand: Double = 0.0,
    val totalPayable: Double = 0.0,
    val amountCollected: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val sourceRowNumber: Int = 0,
    val rawCsvRow: String = "",
    val hasCalculationMismatch: Boolean = false,
    val hasMissingValues: Boolean = false,
    val isDuplicate: Boolean = false,
    val mismatchNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
