package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val holdingId: String = "",
    val dueDate: String, // YYYY-MM-DD
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val category: String = "INSPECTION", // INSPECTION, DEMAND_NOTICE, HEARING, FOLLOW_UP, GENERAL
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED
    val notes: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
