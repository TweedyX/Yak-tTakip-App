package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_logs")
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val liters: Double,
    val totalCost: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val pricePerLiter: Double
        get() = if (liters > 0) totalCost / liters else 0.0
}
