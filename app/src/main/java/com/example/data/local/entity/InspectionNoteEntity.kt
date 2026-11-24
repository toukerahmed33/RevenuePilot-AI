package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_notes")
data class InspectionNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inspectionRef: String,
    val holdingRef: String,
    val assignedArea: String,
    val inspectionDate: String, // YYYY-MM-DD
    val verificationStatus: String, // VERIFIED, MISMATCH_MEASUREMENT, UNAUTHORIZED_COMMERCIAL, EXTENSION_FOUND, INCOMPLETE
    val observations: String,
    val followUpAction: String,
    val notes: String = "",
    val isDraft: Boolean = true,
    val photoUri: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
