package com.example.data.repository

import com.example.data.db.FuelDao
import com.example.data.model.EfficiencyLog
import com.example.data.model.FuelLog
import com.example.data.model.VehicleProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FuelRepository(private val fuelDao: FuelDao) {
    val vehicleProfile: Flow<VehicleProfile?> = fuelDao.getVehicleProfile()
    val allFuelLogs: Flow<List<FuelLog>> = fuelDao.getAllFuelLogs()
    val allEfficiencyLogs: Flow<List<EfficiencyLog>> = fuelDao.getAllEfficiencyLogs()

    suspend fun saveVehicleProfile(profile: VehicleProfile) {
        fuelDao.insertVehicleProfile(profile)
    }

    suspend fun insertFuelLog(log: FuelLog) {
        fuelDao.insertFuelLog(log)
    }

    suspend fun deleteFuelLog(id: Long) {
        fuelDao.deleteFuelLogById(id)
    }

    suspend fun insertEfficiencyLog(log: EfficiencyLog) {
        fuelDao.insertEfficiencyLog(log)
    }

    suspend fun deleteEfficiencyLog(id: Long) {
        fuelDao.deleteEfficiencyLogById(id)
    }

    suspend fun initializeDefaultIfNeeded() {
        try {
            // Check if profile exists; first() will suspend until a value is emitted.
            val current = fuelDao.getVehicleProfile().first()
            if (current == null) {
                fuelDao.insertVehicleProfile(VehicleProfile(id = 1, name = "Aracım", tankCapacity = 50.0, fuelType = "Benzin"))
            }
        } catch (e: Exception) {
            // In case of empty emissions or errors, safely write a default profile
            fuelDao.insertVehicleProfile(VehicleProfile(id = 1, name = "Aracım", tankCapacity = 50.0, fuelType = "Benzin"))
        }
    }
}
