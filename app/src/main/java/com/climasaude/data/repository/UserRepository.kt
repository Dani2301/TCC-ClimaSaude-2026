package com.climasaude.data.repository

import com.climasaude.data.database.dao.UserDao
import com.climasaude.data.database.dao.EmergencyContactDao
import com.climasaude.data.database.entities.User
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.domain.models.UserProfile
import com.climasaude.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val emergencyContactDao: EmergencyContactDao,
    private val firestore: FirebaseFirestore
) {

    fun getUserProfileFlow(userId: String): Flow<UserProfile?> {
        return userDao.getUserByIdFlow(userId).map { user ->
            user?.let { convertToUserProfile(it) }
        }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Resource<String> {
        return try {
            val user = convertToUser(userProfile)
            userDao.updateUser(user)

            // Sync with Firebase
            try {
                firestore.collection("users")
                    .document(userProfile.id)
                    .set(userProfile)
                    .await()
            } catch (e: Exception) {
                // Ignore Firebase errors, data is saved locally
            }

            Resource.Success("Perfil atualizado com sucesso")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar perfil: ${e.message}")
        }
    }

    suspend fun updateUserPhoto(userId: String, photoUrl: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(photoUrl = photoUrl, updatedAt = Date())
                userDao.updateUser(updatedUser)
                Resource.Success("Foto atualizada com sucesso")
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar foto: ${e.message}")
        }
    }

    suspend fun addMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedConditions = user.medicalConditions.toMutableList()
                if (!updatedConditions.contains(condition)) {
                    updatedConditions.add(condition)
                    val updatedUser = user.copy(
                        medicalConditions = updatedConditions,
                        updatedAt = Date()
                    )
                    userDao.updateUser(updatedUser)
                    Resource.Success("Condição médica adicionada")
                } else {
                    Resource.Error("Condição já existe")
                }
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao adicionar condição: ${e.message}")
        }
    }

    suspend fun removeMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedConditions = user.medicalConditions.toMutableList()
                if (updatedConditions.remove(condition)) {
                    val updatedUser = user.copy(
                        medicalConditions = updatedConditions,
                        updatedAt = Date()
                    )
                    userDao.updateUser(updatedUser)
                    Resource.Success("Condição médica removida")
                } else {
                    Resource.Error("Condição não encontrada")
                }
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao remover condição: ${e.message}")
        }
    }

    suspend fun addAllergy(userId: String, allergy: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedAllergies = user.allergies.toMutableList()
                if (!updatedAllergies.contains(allergy)) {
                    updatedAllergies.add(allergy)
                    val updatedUser = user.copy(
                        allergies = updatedAllergies,
                        updatedAt = Date()
                    )
                    userDao.updateUser(updatedUser)
                    Resource.Success("Alergia adicionada")
                } else {
                    Resource.Error("Alergia já existe")
                }
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao adicionar alergia: ${e.message}")
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        preferences: Map<String, Boolean>
    ): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    notificationPreferences = preferences,
                    updatedAt = Date()
                )
                userDao.updateUser(updatedUser)
                Resource.Success("Preferências atualizadas")
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar preferências: ${e.message}")
        }
    }

    suspend fun updatePrivacySettings(
        userId: String,
        settings: Map<String, Boolean>
    ): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    privacySettings = settings,
                    updatedAt = Date()
                )
                userDao.updateUser(updatedUser)
                Resource.Success("Configurações de privacidade atualizadas")
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar privacidade: ${e.message}")
        }
    }

    suspend fun addEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try {
            emergencyContactDao.insertContact(contact)
            Resource.Success("Contato de emergência adicionado")
        } catch (e: Exception) {
            Resource.Error("Erro ao adicionar contato: ${e.message}")
        }
    }

    suspend fun updateEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try {
            emergencyContactDao.updateContact(contact)
            Resource.Success("Contato de emergência atualizado")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar contato: ${e.message}")
        }
    }

    suspend fun removeEmergencyContact(contactId: String): Resource<String> {
        return try {
            emergencyContactDao.deactivateContact(contactId)
            Resource.Success("Contato de emergência removido")
        } catch (e: Exception) {
            Resource.Error("Erro ao remover contato: ${e.message}")
        }
    }

    fun getEmergencyContactsFlow(userId: String): Flow<List<EmergencyContact>> {
        return emergencyContactDao.getEmergencyContactsFlow(userId)
    }

    suspend fun exportUserData(userId: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            val emergencyContacts = emergencyContactDao.getEmergencyContacts(userId)

            // Create JSON export
            val exportData = mapOf(
                "user" to user,
                "emergencyContacts" to emergencyContacts,
                "exportDate" to Date(),
                "version" to "1.0"
            )

            // Convert to JSON string (using Gson or similar)
            val jsonData = com.google.gson.Gson().toJson(exportData)

            Resource.Success(jsonData)
        } catch (e: Exception) {
            Resource.Error("Erro ao exportar dados: ${e.message}")
        }
    }

    suspend fun calculateProfileCompleteness(userId: String): Resource<Int> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                var completeness = 0
                val totalFields = 10

                // Basic info (30%)
                if (user.name.isNotBlank()) completeness += 10
                if (user.email.isNotBlank()) completeness += 10
                if (user.photoUrl != null) completeness += 10

                // Personal info (40%)
                if (user.birthDate != null) completeness += 10
                if (user.gender != null) completeness += 10
                if (user.weight != null) completeness += 10
                if (user.height != null) completeness += 10

                // Health info (30%)
                if (user.medicalConditions.isNotEmpty()) completeness += 10
                if (user.allergies.isNotEmpty()) completeness += 5
                if (user.emergencyContact != null) completeness += 5

                Resource.Success(completeness)
            } else {
                Resource.Error("Usuário não encontrado")
            }
        } catch (e: Exception) {
            Resource.Error("Erro ao calcular completude: ${e.message}")
        }
    }

    private fun convertToUserProfile(user: User): UserProfile {
        return UserProfile(
            id = user.id,
            email = user.email,
            name = user.name,
            photoUrl = user.photoUrl,
            birthDate = user.birthDate,
            age = user.birthDate?.let { calculateAge(it) },
            gender = user.gender,
            weight = user.weight,
            height = user.height,
            bmi = if (user.weight != null && user.height != null) {
                calculateBMI(user.weight, user.height)
            } else null,
            medicalConditions = user.medicalConditions.map { condition ->
                com.climasaude.domain.models.MedicalCondition(
                    id = UUID.randomUUID().toString(),
                    name = condition,
                    severity = "moderate",
                    diagnosedDate = null,
                    isWeatherSensitive = isWeatherSensitiveCondition(condition)
                )
            },
            allergies = user.allergies.map { allergy ->
                com.climasaude.domain.models.Allergy(
                    id = UUID.randomUUID().toString(),
                    name = allergy,
                    severity = "moderate",
                    season = determineAllergySeason(allergy)
                )
            },
            preferences = com.climasaude.domain.models.UserPreferences(
                theme = user.theme,
                language = user.language,
                notifications = com.climasaude.domain.models.NotificationPreferences(
                    weatherAlerts = user.notificationPreferences["weatherAlerts"] ?: true,
                    medicationReminders = user.notificationPreferences["medicationReminders"] ?: true,
                    healthTips = user.notificationPreferences["healthTips"] ?: true,
                    emergencyAlerts = user.notificationPreferences["emergencyAlerts"] ?: true,
                    dailyReports = user.notificationPreferences["dailyReports"] ?: false
                ),
                privacy = com.climasaude.domain.models.PrivacySettings(
                    shareLocation = user.privacySettings["shareLocation"] ?: true,
                    shareHealthData = user.privacySettings["shareHealthData"] ?: false,
                    analyticsEnabled = user.privacySettings["analyticsEnabled"] ?: true,
                    crashReportsEnabled = user.privacySettings["crashReportsEnabled"] ?: true
                ),
                location = com.climasaude.domain.models.LocationSettings()
            ),
            isComplete = calculateIsProfileComplete(user)
        )
    }

    private fun convertToUser(userProfile: UserProfile): User {
        return User(
            id = userProfile.id,
            email = userProfile.email,
            name = userProfile.name,
            photoUrl = userProfile.photoUrl,
            birthDate = userProfile.birthDate,
            gender = userProfile.gender,
            weight = userProfile.weight,
            height = userProfile.height,
            medicalConditions = userProfile.medicalConditions.map { it.name },
            allergies = userProfile.allergies.map { it.name },
            theme = userProfile.preferences.theme,
            language = userProfile.preferences.language,
            notificationPreferences = mapOf(
                "weatherAlerts" to userProfile.preferences.notifications.weatherAlerts,
                "medicationReminders" to userProfile.preferences.notifications.medicationReminders,
                "healthTips" to userProfile.preferences.notifications.healthTips,
                "emergencyAlerts" to userProfile.preferences.notifications.emergencyAlerts,
                "dailyReports" to userProfile.preferences.notifications.dailyReports
            ),
            privacySettings = mapOf(
                "shareLocation" to userProfile.preferences.privacy.shareLocation,
                "shareHealthData" to userProfile.preferences.privacy.shareHealthData,
                "analyticsEnabled" to userProfile.preferences.privacy.analyticsEnabled,
                "crashReportsEnabled" to userProfile.preferences.privacy.crashReportsEnabled
            ),
            updatedAt = Date()
        )
    }

    private fun calculateAge(birthDate: Date): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = birthDate
        val birthYear = calendar.get(Calendar.YEAR)
        return currentYear - birthYear
    }

    private fun calculateBMI(weight: Float, height: Float): Float {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }

    private fun isWeatherSensitiveCondition(condition: String): Boolean {
        val weatherSensitiveConditions = listOf(
            "artrite", "artrose", "reumatismo", "fibromialgia",
            "enxaqueca", "cefaleia", "dor de cabeça",
            "asma", "bronquite", "dpoc", "rinite", "sinusite",
            "pressão alta", "hipertensão", "hipotensão"
        )

        return weatherSensitiveConditions.any {
            condition.contains(it, ignoreCase = true)
        }
    }

    private fun determineAllergySeason(allergy: String): String? {
        return when {
            allergy.contains("pólen", ignoreCase = true) ||
                    allergy.contains("gramínea", ignoreCase = true) -> "primavera"
            allergy.contains("ácaro", ignoreCase = true) -> "inverno"
            allergy.contains("fungo", ignoreCase = true) ||
                    allergy.contains("mofo", ignoreCase = true) -> "outono"
            else -> null
        }
    }

    private fun calculateIsProfileComplete(user: User): Boolean {
        return user.name.isNotBlank() &&
                user.email.isNotBlank() &&
                user.birthDate != null &&
                user.gender != null &&
                (user.medicalConditions.isNotEmpty() || user.allergies.isNotEmpty())
    }
}