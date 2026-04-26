package com.climasaude.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.climasaude.data.database.entities.Medication
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllMedications(userId: String): List<Medication>

    @Query("SELECT * FROM medications WHERE userId = :userId AND isActive = 1 ORDER BY name ASC")
    fun getActiveMedicationsFlow(userId: String): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveMedications(userId: String): List<Medication>

    @Query("SELECT * FROM medications WHERE userId = :userId AND weatherSensitive = 1 AND isActive = 1")
    suspend fun getWeatherSensitiveMedications(userId: String): List<Medication>

    @Query("SELECT * FROM medications WHERE userId = :userId AND reminderEnabled = 1 AND isActive = 1")
    suspend fun getMedicationsWithReminders(userId: String): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :medicationId")
    suspend fun getMedicationById(medicationId: String): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("UPDATE medications SET isActive = 0 WHERE id = :medicationId")
    suspend fun deactivateMedication(medicationId: String)

    @Query("UPDATE medications SET reminderEnabled = :enabled WHERE id = :medicationId")
    suspend fun updateReminderStatus(medicationId: String, enabled: Boolean)

    @Query("SELECT * FROM medications WHERE userId = :userId AND endDate IS NOT NULL AND endDate < :currentDate AND isActive = 1")
    suspend fun getExpiredMedications(userId: String, currentDate: Date): List<Medication>
}
