package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EfficiencyLog
import com.example.data.model.FuelLog
import com.example.data.model.VehicleProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM vehicle_profile WHERE id = 1 LIMIT 1")
    fun getVehicleProfile(): Flow<VehicleProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleProfile(profile: VehicleProfile)

    @Update
    suspend fun updateVehicleProfile(profile: VehicleProfile)

    @Query("SELECT * FROM fuel_logs ORDER BY timestamp DESC")
    fun getAllFuelLogs(): Flow<List<FuelLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(log: FuelLog)

    @Query("DELETE FROM fuel_logs WHERE id = :id")
    suspend fun deleteFuelLogById(id: Long)

    @Query("SELECT * FROM efficiency_logs ORDER BY timestamp DESC")
    fun getAllEfficiencyLogs(): Flow<List<EfficiencyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEfficiencyLog(log: EfficiencyLog)

    @Query("DELETE FROM efficiency_logs WHERE id = :id")
    suspend fun deleteEfficiencyLogById(id: Long)
}
