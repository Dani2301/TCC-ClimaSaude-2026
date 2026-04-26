package com.climasaude.data.repository

import com.climasaude.data.database.dao.MedicationDao
import com.climasaude.data.database.dao.SymptomDao
import com.climasaude.data.database.dao.UserDao
import com.climasaude.data.database.dao.MedicationLogDao
import com.climasaude.data.database.entities.Medication
import com.climasaude.data.database.entities.Symptom
import com.climasaude.data.database.entities.MedicationLog
import com.climasaude.domain.models.*
import com.climasaude.utils.HealthXlsxExporter
import com.climasaude.utils.Resource
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val userDao: UserDao,
    private val medicationDao: MedicationDao,
    private val symptomDao: SymptomDao,
    private val medicationLogDao: MedicationLogDao,
    private val healthXlsxExporter: HealthXlsxExporter
) {
    fun getActiveMedications(userId: String) = medicationDao.getActiveMedicationsFlow(userId)

    suspend fun getAllSymptoms(userId: String) = symptomDao.getAllSymptomsSnapshot(userId)

    suspend fun addMedication(medication: Medication) {
        medicationDao.insertMedication(medication)
    }

    suspend fun addSymptom(symptom: Symptom) {
        symptomDao.insertSymptom(symptom)
    }

    suspend fun deleteSymptom(symptom: Symptom) {
        symptomDao.deleteSymptom(symptom)
    }

    suspend fun logMedication(log: MedicationLog) {
        medicationLogDao.insertLog(log)
    }

    suspend fun getUserHealthProfile(userId: String): UserProfile {
        val user = userDao.getUserById(userId)
        return UserProfile(
            id = userId,
            name = user?.name ?: "",
            email = user?.email ?: ""
            // Aqui você mapearia outras propriedades se o seu banco de dados as tivesse
        )
    }

    suspend fun exportMedicationsAndSymptoms(userId: String, targetFile: File): Resource<File> {
        val user = userDao.getUserById(userId)
        val medications = medicationDao.getActiveMedications(userId)
        val startDate = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)
        val symptoms = symptomDao.getSymptomsByDateRange(userId, startDate)
        
        return healthXlsxExporter.exportHealthDataTo(
            targetFile = targetFile,
            userName = user?.name ?: "Usuário",
            medications = medications,
            symptoms = symptoms
        )
    }

    suspend fun getHealthStatistics(userId: String, days: Int): Resource<HealthStatistics> {
        return try {
            val startDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
            val symptoms = symptomDao.getSymptomsByDateRange(userId, startDate)
            
            val averageSeverity = if (symptoms.isNotEmpty()) {
                symptoms.map { it.intensity.toDouble() }.average()
            } else 0.0

            Resource.Success(HealthStatistics(averageSeverity, symptoms.size))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro desconhecido")
        }
    }
}

data class HealthStatistics(
    val averageSeverity: Double,
    val totalSymptoms: Int
)
