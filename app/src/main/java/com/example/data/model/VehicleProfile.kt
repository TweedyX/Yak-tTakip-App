package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_profile")
data class VehicleProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Aracım",
    val tankCapacity: Double = 50.0,
    val fuelType: String = "Benzin"
)
