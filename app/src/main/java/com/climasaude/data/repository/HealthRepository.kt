package com.climasaude.data.repository

import com.climasaude.data.database.dao.*
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.database.entities.MedicationLog
import com.climasaude.data.database.entities.Symptom as DbSymptom
import com.climasaude.data.network.HealthApiService
import com.climasaude.domain.models.*
import com.climasaude.domain.usecases.*
import com.climasaude.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthAlertDao: HealthAlertDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val symptomDao: SymptomDao,
    private val userDao: UserDao,
    private val healthApiService: HealthApiService,
    private val userRepository: UserRepository
) {

    fun getAllSymptoms(userId: String): Flow<List<DbSymptom>> {
        return symptomDao.getAllSymptoms(userId)
    }

    suspend fun addSymptom(symptom: DbSymptom) {
        symptomDao.insertSymptom(symptom)
    }

    suspend fun deleteSymptom(symptom: DbSymptom) {
        symptomDao.deleteSymptom(symptom)
    }

    suspend fun recordSymptom(userId: String, symptom: com.climasaude.domain.models.Symptom): Resource<String> {
        return try {
            val dbSymptom = DbSymptom(
                id = symptom.id,
                userId = userId,
                name = symptom.name,
                intensity = symptom.severity,
                notes = symptom.location,
                timestamp = symptom.timestamp
            )
            symptomDao.insertSymptom(dbSymptom)
            Resource.Success("Sintoma registrado")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro ao registrar sintoma")
        }
    }

    suspend fun getRecentSymptoms(userId: String, days: Int): List<com.climasaude.domain.models.Symptom> {
        val startDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
        return symptomDao.getSymptomsByDateRange(userId, startDate).map {
            com.climasaude.domain.models.Symptom(
                id = it.id,
                name = it.name,
                severity = it.intensity,
                duration = "",
                timestamp = it.timestamp
            )
        }
    }

    suspend fun getUserHealthProfile(userId: String): UserProfile {
        return userRepository.getUserProfileFlow(userId).firstOrNull() ?: UserProfile(
            id = userId,
            email = "",
            name = "",
            preferences = UserPreferences(
                notifications = NotificationPreferences(),
                privacy = PrivacySettings(),
                location = LocationSettings()
            )
        )
    }

    suspend fun analyzeSymptomTrends(userId: String, symptomName: String, days: Int): Resource<SymptomTrendAnalysis> {
        return Resource.Error("Análise de tendências não implementada")
    }

    suspend fun checkMedicationAdherence(userId: String): Resource<MedicationAdherenceReport> {
        return Resource.Error("Verificação de adesão não implementada")
    }

    suspend fun correlateWeatherSymptoms(userId: String, period: Int): Resource<WeatherSymptomCorrelation> {
        return Resource.Error("Correlação não implementada")
    }

    fun getActiveMedications(userId: String): Flow<List<Medication>> {
        return medicationDao.getActiveMedicationsFlow(userId)
    }

    suspend fun addMedication(medication: Medication) {
        medicationDao.insertMedication(medication)
    }

    suspend fun logMedication(log: MedicationLog) {
        medicationLogDao.insertLog(log)
    }

    suspend fun getHealthStatistics(userId: String, days: Int): Resource<HealthStatistics> {
        return try {
            val startDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
            val symptoms = symptomDao.getSymptomsByDateRange(userId, startDate)
            
            val averageSeverity = if (symptoms.isNotEmpty()) {
                symptoms.map { it.intensity.toDouble() }.average()
            } else 0.0
            
            val mostCommon = symptoms.groupBy { it.name }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .map { it.first }

            Resource.Success(HealthStatistics(
                symptomsRecorded = symptoms.size,
                averageSeverity = averageSeverity,
                mostCommonSymptoms = mostCommon
            ))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro ao buscar estatísticas")
        }
    }
}
