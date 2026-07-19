package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "efficiency_logs")
data class EfficiencyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val l100km: Double,
    val timestamp: Long = System.currentTimeMillis()
)
