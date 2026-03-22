package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.UserRepository
import com.climasaude.data.repository.AuthRepository
import com.climasaude.domain.models.UserProfile
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    private val _profileCompleteness = MutableStateFlow(0)
    val profileCompleteness: StateFlow<Int> = _profileCompleteness.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadUserProfile()
        loadEmergencyContacts()
        calculateProfileCompleteness()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userId = getCurrentUserId()
                userRepository.getUserProfileFlow(userId).collect { profile ->
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao carregar perfil"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                userRepository.getEmergencyContactsFlow(userId).collect { contacts ->
                    _emergencyContacts.value = contacts
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro ao carregar contatos de emergência"
            }
        }
    }

    private fun calculateProfileCompleteness() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.calculateProfileCompleteness(userId)) {
                    is Resource.Success -> {
                        _profileCompleteness.value = result.data ?: 0
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao calcular completude do perfil: ${e.message}"
            }
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                when (val result = userRepository.updateUserProfile(updatedProfile)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        calculateProfileCompleteness()
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar perfil: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updatePersonalInfo(
        name: String,
        birthDate: Date?,
        gender: String?,
        weight: Float?,
        height: Float?
    ) {
        val currentProfile = _userProfile.value ?: return
        val updatedProfile = currentProfile.copy(
            name = name,
            birthDate = birthDate,
            gender = gender,
            weight = weight,
            height = height
        )
        updateProfile(updatedProfile)
    }

    fun updatePhoto(photoUrl: String) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.updateUserPhoto(userId, photoUrl)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar foto: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun addMedicalCondition(condition: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.addMedicalCondition(userId, condition)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        calculateProfileCompleteness()
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao adicionar condição médica: ${e.message}"
            }
        }
    }

    fun removeMedicalCondition(condition: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.removeMedicalCondition(userId, condition)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        calculateProfileCompleteness()
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao remover condição médica: ${e.message}"
            }
        }
    }

    fun addAllergy(allergy: String) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.addAllergy(userId, allergy)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        calculateProfileCompleteness()
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao adicionar alergia: ${e.message}"
            }
        }
    }

    fun addEmergencyContact(
        name: String,
        phone: String,
        email: String?,
        relationship: String,
        isDoctor: Boolean = false,
        specialty: String? = null
    ) {
        viewModelScope.launch {
            try {
                val contact = EmergencyContact(
                    id = UUID.randomUUID().toString(),
                    userId = getCurrentUserId(),
                    name = name,
                    phone = phone,
                    email = email,
                    relationship = relationship,
                    isDoctor = isDoctor,
                    specialty = specialty,
                    priority = _emergencyContacts.value.size + 1
                )

                when (val result = userRepository.addEmergencyContact(contact)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        calculateProfileCompleteness()
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao adicionar contato: ${e.message}"
            }
        }
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                when (val result = userRepository.updateEmergencyContact(contact)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar contato: ${e.message}"
            }
        }
    }

    fun removeEmergencyContact(contactId: String) {
        viewModelScope.launch {
            try {
                when (val result = userRepository.removeEmergencyContact(contactId)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao remover contato: ${e.message}"
            }
        }
    }

    fun updateNotificationPreferences(preferences: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.updateNotificationPreferences(userId, preferences)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar preferências: ${e.message}"
            }
        }
    }

    fun updatePrivacySettings(settings: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.updatePrivacySettings(userId, settings)) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao atualizar privacidade: ${e.message}"
            }
        }
    }

    fun exportUserData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userId = getCurrentUserId()
                when (val result = userRepository.exportUserData(userId)) {
                    is Resource.Success -> {
                        _successMessage.value = "Dados exportados com sucesso"
                        // Handle export data - could save to file or share
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao exportar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                when (val result = authRepository.logout()) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        // Navigation to login will be handled by the UI
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro no logout: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                when (val result = authRepository.deleteAccount()) {
                    is Resource.Success -> {
                        _successMessage.value = result.data
                        // Navigation to login will be handled by the UI
                    }
                    is Resource.Error -> {
                        _errorMessage.value = result.message
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao deletar conta: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private fun getCurrentUserId(): String {
        return appPreferences.getUserId().ifEmpty { "admin_id" }
    }
}
