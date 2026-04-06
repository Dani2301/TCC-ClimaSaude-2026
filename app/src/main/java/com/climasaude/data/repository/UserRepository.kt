package com.climasaude.data.repository

import com.climasaude.data.database.dao.UserDao
import com.climasaude.data.database.dao.EmergencyContactDao
import com.climasaude.data.database.entities.User
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.domain.models.*
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

    suspend fun getUserProfile(userId: String): UserProfile? {
        val user = userDao.getUserById(userId)
        return user?.let { convertToUserProfile(it) }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Resource<String> {
        return try {
            val existingUser = userDao.getUserById(userProfile.id)
            val user = convertToUser(userProfile).copy(
                medications = existingUser?.medications ?: emptyList(),
                emergencyContact = existingUser?.emergencyContact,
                createdAt = existingUser?.createdAt ?: Date()
            )
            userDao.insertUser(user)
            syncWithFirebase(userProfile)
            Resource.Success("Perfil atualizado")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar: ${e.message}")
        }
    }

    // Novo método para atualizar múltiplos dados de saúde de uma vez, evitando race conditions. Modificado por: Daniel
    suspend fun updateFullHealthProfile(
        userId: String,
        weight: Float?,
        height: Float?,
        newCondition: String?,
        newAllergy: String?
    ): Resource<String> {
        return try {
            val user = userDao.getUserById(userId) ?: return Resource.Error("Usuário não encontrado")
            
            val updatedConditions = user.medicalConditions.toMutableList()
            if (!newCondition.isNullOrBlank() && !updatedConditions.contains(newCondition.trim())) {
                updatedConditions.add(newCondition.trim())
            }

            val updatedAllergies = user.allergies.toMutableList()
            if (!newAllergy.isNullOrBlank() && !updatedAllergies.contains(newAllergy.trim())) {
                updatedAllergies.add(newAllergy.trim())
            }

            val updatedUser = user.copy(
                weight = weight,
                height = height,
                medicalConditions = updatedConditions,
                allergies = updatedAllergies,
                updatedAt = Date()
            )

            userDao.insertUser(updatedUser)
            syncWithFirebase(convertToUserProfile(updatedUser))
            Resource.Success("Perfil de saúde atualizado com sucesso")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar perfil completo: ${e.message}")
        }
    }

    private suspend fun syncWithFirebase(userProfile: UserProfile) {
        try {
            firestore.collection("users").document(userProfile.id).set(userProfile).await()
        } catch (_: Exception) { }
    }

    suspend fun addMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val trimmed = condition.trim()
                val updated = user.medicalConditions.toMutableList()
                if (!updated.contains(trimmed)) {
                    updated.add(trimmed)
                    val updatedUser = user.copy(medicalConditions = updated, updatedAt = Date())
                    userDao.insertUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Adicionado")
                } else Resource.Error("Já existe")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }

    suspend fun removeMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val trimmed = condition.trim()
                val updated = user.medicalConditions.toMutableList()
                if (updated.removeAll { it.trim().equals(trimmed, ignoreCase = true) }) {
                    val updatedUser = user.copy(medicalConditions = updated, updatedAt = Date())
                    userDao.insertUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Removido")
                } else Resource.Error("Não encontrado")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }

    suspend fun addAllergy(userId: String, allergy: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val trimmed = allergy.trim()
                val updated = user.allergies.toMutableList()
                if (!updated.contains(trimmed)) {
                    updated.add(trimmed)
                    val updatedUser = user.copy(allergies = updated, updatedAt = Date())
                    userDao.insertUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Adicionado")
                } else Resource.Error("Já existe")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }

    suspend fun removeAllergy(userId: String, allergy: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val trimmed = allergy.trim()
                val updated = user.allergies.toMutableList()
                if (updated.removeAll { it.trim().equals(trimmed, ignoreCase = true) }) {
                    val updatedUser = user.copy(allergies = updated, updatedAt = Date())
                    userDao.insertUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Removido")
                } else Resource.Error("Não encontrado")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }

    private fun convertToUserProfile(user: User): UserProfile {
        val notifyPrefs = NotificationPreferences(
            weatherAlerts = user.notificationPreferences["weatherAlerts"] ?: true,
            medicationReminders = user.notificationPreferences["medicationReminders"] ?: true,
            healthTips = user.notificationPreferences["healthTips"] ?: true,
            emergencyAlerts = user.notificationPreferences["emergencyAlerts"] ?: true,
            dailyReports = user.notificationPreferences["dailyReports"] ?: false
        )

        val privacySet = PrivacySettings(
            shareLocation = user.privacySettings["shareLocation"] ?: true,
            shareHealthData = user.privacySettings["shareHealthData"] ?: false,
            analyticsEnabled = user.privacySettings["analyticsEnabled"] ?: true,
            crashReportsEnabled = user.privacySettings["crashReportsEnabled"] ?: true
        )

        val userPrefs = UserPreferences(
            theme = user.theme,
            language = user.language,
            units = "metric",
            notifications = notifyPrefs,
            privacy = privacySet,
            location = LocationSettings()
        )

        val weight = user.weight
        val height = user.height
        val bmi = if (weight != null && weight > 0 && height != null && height > 0) {
            calculateBMI(weight, height)
        } else null

        return UserProfile(
            id = user.id,
            email = user.email,
            name = user.name,
            photoUrl = user.photoUrl,
            birthDate = user.birthDate,
            age = user.birthDate?.let { calculateAge(it) },
            gender = user.gender,
            weight = weight,
            height = height,
            bmi = bmi,
            medicalConditions = user.medicalConditions.map { MedicalCondition(UUID.randomUUID().toString(), it, "moderate", null, isWeatherSensitiveCondition(it)) },
            allergies = user.allergies.map { Allergy(UUID.randomUUID().toString(), it, "moderate", determineAllergySeason(it)) },
            preferences = userPrefs,
            isComplete = calculateIsProfileComplete(user)
        )
    }

    private fun convertToUser(userProfile: UserProfile): User {
        val prefs = userProfile.preferences ?: UserPreferences()
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
            theme = prefs.theme,
            language = prefs.language,
            notificationPreferences = mapOf(
                "weatherAlerts" to prefs.notifications.weatherAlerts,
                "medicationReminders" to prefs.notifications.medicationReminders,
                "healthTips" to prefs.notifications.healthTips,
                "emergencyAlerts" to prefs.notifications.emergencyAlerts,
                "dailyReports" to prefs.notifications.dailyReports
            ),
            privacySettings = mapOf(
                "shareLocation" to prefs.privacy.shareLocation,
                "shareHealthData" to prefs.privacy.shareHealthData,
                "analyticsEnabled" to prefs.privacy.analyticsEnabled,
                "crashReportsEnabled" to prefs.privacy.crashReportsEnabled
            ),
            updatedAt = Date()
        )
    }

    private fun calculateAge(birthDate: Date): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = birthDate
        return currentYear - calendar.get(Calendar.YEAR)
    }

    private fun calculateBMI(weight: Float, height: Float): Float {
        val hInMeters = height / 100
        return weight / (hInMeters * hInMeters)
    }

    private fun isWeatherSensitiveCondition(condition: String): Boolean {
        val sensitive = listOf("artrite", "artrose", "reumatismo", "fibromialgia", "enxaqueca", "cefaleia", "dor de cabeça", "asma", "bronquite", "dpoc", "rinite", "sinusite", "pressão alta", "hipertensão", "hipotensão")
        return sensitive.any { condition.contains(it, ignoreCase = true) }
    }

    private fun determineAllergySeason(allergy: String): String? {
        val a = allergy.lowercase()
        return when {
            a.contains("pólen") || a.contains("gramínea") -> "primavera"
            a.contains("ácaro") -> "inverno"
            a.contains("fungo") || a.contains("mofo") -> "outono"
            else -> null
        }
    }

    private fun calculateIsProfileComplete(user: User): Boolean {
        return user.name.isNotBlank() && user.email.isNotBlank() && user.birthDate != null && user.gender != null && (user.medicalConditions.isNotEmpty() || user.allergies.isNotEmpty())
    }

    suspend fun addEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try { emergencyContactDao.insertContact(contact); Resource.Success("Adicionado") } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }
    suspend fun updateEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try { emergencyContactDao.updateContact(contact); Resource.Success("Atualizado") } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }
    suspend fun removeEmergencyContact(contactId: String): Resource<String> {
        return try { emergencyContactDao.deactivateContact(contactId); Resource.Success("Removido") } catch (e: Exception) { Resource.Error(e.message ?: "Erro") }
    }
    fun getEmergencyContactsFlow(userId: String): Flow<List<EmergencyContact>> = emergencyContactDao.getEmergencyContactsFlow(userId)
}
