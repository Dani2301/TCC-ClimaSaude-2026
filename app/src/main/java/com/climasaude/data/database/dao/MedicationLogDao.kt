package com.climasaude.data.database.dao

import androidx.room.*
import com.climasaude.data.database.entities.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs WHERE userId = :userId ORDER BY scheduledTime DESC")
    fun getAllLogs(userId: String): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE userId = :userId AND scheduledTime BETWEEN :start AND :end")
    suspend fun getLogsInRange(userId: String, start: Date, end: Date): List<MedicationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    @Update
    suspend fun updateLog(log: MedicationLog)

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId AND isTaken = 0 ORDER BY scheduledTime ASC LIMIT 1")
    suspend fun getNextPendingLog(medicationId: String): MedicationLog?

    @Query("UPDATE medication_logs SET isTaken = 1, takenTime = :takenTime WHERE id = :logId")
    suspend fun markAsTaken(logId: String, takenTime: Date)

    @Query("UPDATE medication_logs SET scheduledTime = :newTime, snoozedCount = snoozedCount + 1 WHERE id = :logId")
    suspend fun snoozeLog(logId: String, newTime: Date)
}
