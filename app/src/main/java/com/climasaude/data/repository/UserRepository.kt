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
            val user = convertToUser(userProfile)
            userDao.updateUser(user)
            syncWithFirebase(userProfile)
            Resource.Success("Perfil atualizado com sucesso")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar perfil: ${e.message}")
        }
    }

    suspend fun updateUserPhoto(userId: String, photoUrl: String): Resource<String> {
        return try {
            userDao.updatePhotoUrl(userId, photoUrl)
            // Optional: sync with Firebase if profile exists
            getUserProfile(userId)?.let { syncWithFirebase(it) }
            Resource.Success("Foto atualizada com sucesso")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar foto: ${e.message}")
        }
    }

    suspend fun updateNotificationPreferences(userId: String, preferences: Map<String, Boolean>): Resource<String> {
        return try {
            userDao.updateNotificationPreferences(userId, preferences)
            getUserProfile(userId)?.let { syncWithFirebase(it) }
            Resource.Success("Preferências de notificação atualizadas")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar preferências: ${e.message}")
        }
    }

    suspend fun updatePrivacySettings(userId: String, settings: Map<String, Boolean>): Resource<String> {
        return try {
            userDao.updatePrivacySettings(userId, settings)
            getUserProfile(userId)?.let { syncWithFirebase(it) }
            Resource.Success("Configurações de privacidade atualizadas")
        } catch (e: Exception) {
            Resource.Error("Erro ao atualizar privacidade: ${e.message}")
        }
    }

    private suspend fun syncWithFirebase(userProfile: UserProfile) {
        try {
            firestore.collection("users")
                .document(userProfile.id)
                .set(userProfile)
                .await()
        } catch (_: Exception) { }
    }

    suspend fun addMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedConditions = user.medicalConditions.toMutableList()
                if (!updatedConditions.contains(condition)) {
                    updatedConditions.add(condition)
                    val updatedUser = user.copy(medicalConditions = updatedConditions, updatedAt = Date())
                    userDao.updateUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Condição médica adicionada")
                } else Resource.Error("Condição já existe")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun removeMedicalCondition(userId: String, condition: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedConditions = user.medicalConditions.toMutableList()
                if (updatedConditions.remove(condition)) {
                    val updatedUser = user.copy(medicalConditions = updatedConditions, updatedAt = Date())
                    userDao.updateUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Condição removida")
                } else Resource.Error("Não encontrada")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun addAllergy(userId: String, allergy: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedAllergies = user.allergies.toMutableList()
                if (!updatedAllergies.contains(allergy)) {
                    updatedAllergies.add(allergy)
                    val updatedUser = user.copy(allergies = updatedAllergies, updatedAt = Date())
                    userDao.updateUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Alergia adicionada")
                } else Resource.Error("Já existe")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun removeAllergy(userId: String, allergy: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedAllergies = user.allergies.toMutableList()
                if (updatedAllergies.remove(allergy)) {
                    val updatedUser = user.copy(allergies = updatedAllergies, updatedAt = Date())
                    userDao.updateUser(updatedUser)
                    syncWithFirebase(convertToUserProfile(updatedUser))
                    Resource.Success("Alergia removida")
                } else Resource.Error("Não encontrada")
            } else Resource.Error("Usuário não encontrado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun addEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try {
            emergencyContactDao.insertContact(contact)
            Resource.Success("Contato adicionado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun updateEmergencyContact(contact: EmergencyContact): Resource<String> {
        return try {
            emergencyContactDao.updateContact(contact)
            Resource.Success("Contato atualizado")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    suspend fun removeEmergencyContact(contactId: String): Resource<String> {
        return try {
            emergencyContactDao.deactivateContact(contactId)
            Resource.Success("Contato removido")
        } catch (e: Exception) { Resource.Error("Erro: ${e.message}") }
    }

    fun getEmergencyContactsFlow(userId: String): Flow<List<EmergencyContact>> {
        return emergencyContactDao.getEmergencyContactsFlow(userId)
    }

    suspend fun exportUserData(userId: String): Resource<String> {
        return try {
            val user = userDao.getUserById(userId)
            val emergencyContacts = emergencyContactDao.getEmergencyContacts(userId)
            val exportData = mapOf("user" to user, "emergencyContacts" to emergencyContacts, "exportDate" to Date())
            val jsonData = com.google.gson.Gson().toJson(exportData)
            Resource.Success(jsonData)
        } catch (e: Exception) { Resource.Error("Erro ao exportar") }
    }

    suspend fun calculateProfileCompleteness(userId: String): Resource<Int> {
        return try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                var c = 0
                if (user.name.isNotBlank()) c += 10
                if (user.email.isNotBlank()) c += 10
                if (user.photoUrl != null) c += 10
                if (user.birthDate != null) c += 10
                if (user.gender != null) c += 10
                if (user.weight != null) c += 10
                if (user.height != null) c += 10
                if (user.medicalConditions.isNotEmpty()) c += 10
                if (user.allergies.isNotEmpty()) c += 10
                if (user.emergencyContact != null) c += 10
                Resource.Success(c)
            } else Resource.Error("Não encontrado")
        } catch (e: Exception) { Resource.Error("Erro cálculo") }
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
            notifications = notifyPrefs,
            privacy = privacySet,
            location = LocationSettings()
        )

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
            bmi = if (user.weight != null && user.height != null) calculateBMI(user.weight, user.height) else null,
            medicalConditions = user.medicalConditions.map { MedicalCondition(UUID.randomUUID().toString(), it, "moderate", null, isWeatherSensitiveCondition(it)) },
            allergies = user.allergies.map { Allergy(UUID.randomUUID().toString(), it, "moderate", determineAllergySeason(it)) },
            preferences = userPrefs,
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
        return currentYear - calendar.get(Calendar.YEAR)
    }

    private fun calculateBMI(weight: Float, height: Float): Float {
        val heightInMeters = height / 100
        return weight / (heightInMeters * heightInMeters)
    }

    private fun isWeatherSensitiveCondition(condition: String): Boolean {
        val sensitive = listOf("artrite", "artrose", "reumatismo", "fibromialgia", "enxaqueca", "cefaleia", "dor de cabeça", "asma", "bronquite", "dpoc", "rinite", "sinusite", "pressão alta", "hipertensão", "hipotensão")
        return sensitive.any { condition.contains(it, ignoreCase = true) }
    }

    private fun determineAllergySeason(allergy: String): String? {
        return when {
            allergy.contains("pólen", ignoreCase = true) || allergy.contains("gramínea", ignoreCase = true) -> "primavera"
            allergy.contains("ácaro", ignoreCase = true) -> "inverno"
            allergy.contains("fungo", ignoreCase = true) || allergy.contains("mofo", ignoreCase = true) -> "outono"
            else -> null
        }
    }

    private fun calculateIsProfileComplete(user: User): Boolean {
        return user.name.isNotBlank() && user.email.isNotBlank() && user.birthDate != null && user.gender != null && (user.medicalConditions.isNotEmpty() || user.allergies.isNotEmpty())
    }
}
