package com.climasaude.data.database.dao

import androidx.room.*
import com.climasaude.data.database.entities.Symptom
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSymptoms(userId: String): Flow<List<Symptom>>

    @Query("SELECT * FROM symptoms WHERE userId = :userId AND timestamp >= :startDate ORDER BY timestamp DESC")
    suspend fun getSymptomsByDateRange(userId: String, startDate: Date): List<Symptom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: Symptom)

    @Delete
    suspend fun deleteSymptom(symptom: Symptom)
}
