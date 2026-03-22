package com.climasaude.data.database.dao

import androidx.room.*
import com.climasaude.data.database.entities.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MedicationLogDao {

    @Query("SELECT * FROM medication_logs WHERE userId = :userId ORDER BY scheduledTime DESC")
    fun getMedicationLogs(userId: String): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY scheduledTime DESC")
    suspend fun getLogsForMedication(medicationId: String): List<MedicationLog>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId AND scheduledTime BETWEEN :startDate AND :endDate")
    suspend fun getLogsBetweenDates(userId: String, startDate: Date, endDate: Date): List<MedicationLog>

    @Query("SELECT COUNT(*) FROM medication_logs WHERE medicationId = :medicationId AND isTaken = 1")
    suspend fun getCompletedDosesCount(medicationId: String): Int

    @Query("SELECT COUNT(*) FROM medication_logs WHERE medicationId = :medicationId AND isTaken = 0 AND scheduledTime < :currentTime")
    suspend fun getMissedDosesCount(medicationId: String, currentTime: Date): Int

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND scheduledTime BETWEEN :startDate AND :endDate")
    suspend fun getLogsForMedicationInPeriod(medicationId: String, startDate: Date, endDate: Date): List<MedicationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<MedicationLog>)

    @Update
    suspend fun updateLog(log: MedicationLog)

    @Query("UPDATE medication_logs SET isTaken = 1, takenTime = :takenTime WHERE id = :logId")
    suspend fun markAsTaken(logId: String, takenTime: Date)

    @Query("UPDATE medication_logs SET snoozedCount = snoozedCount + 1, scheduledTime = :newScheduledTime WHERE id = :logId")
    suspend fun snoozeLog(logId: String, newScheduledTime: Date)

    @Delete
    suspend fun deleteLog(log: MedicationLog)

    @Query("DELETE FROM medication_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: String)
}