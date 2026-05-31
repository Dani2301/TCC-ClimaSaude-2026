package com.climasaude.presentation.viewmodels

import android.util.Log
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateResult = MutableSharedFlow<Resource<String>>()
    val updateResult: SharedFlow<Resource<String>> = _updateResult.asSharedFlow()

    init {
        observeUserProfile()
        observeEmergencyContacts()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) return@launch

            _isLoading.value = true
            userRepository.getUserProfileFlow(userId)
                .onEach { _isLoading.value = false }
                .catch { e -> 
                    Log.e("ProfileViewModel", "Erro no Flow de perfil: ${e.message}")
                    _isLoading.value = false 
                }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }
    }

    private fun observeEmergencyContacts() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) return@launch

            userRepository.getEmergencyContactsFlow(userId).collect { contacts ->
                _emergencyContacts.value = contacts
            }
        }
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            val contact = EmergencyContact(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                phone = phone,
                relationship = relationship
            )
            val result = userRepository.addEmergencyContact(contact)
            _updateResult.emit(result)
        }
    }

    fun updateFullHealthProfile(weight: Float?, height: Float?, condition: String?, allergy: String?) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            _isLoading.value = true
            val result = userRepository.updateFullHealthProfile(
                userId = userId,
                weight = weight,
                height = height,
                newCondition = if (condition.isNullOrBlank()) null else condition,
                newAllergy = if (allergy.isNullOrBlank()) null else allergy
            )
            _updateResult.emit(result)
            _isLoading.value = false
        }
    }

    fun removeEmergencyContact(contactId: String) {
        viewModelScope.launch {
            val result = userRepository.removeEmergencyContact(contactId)
            _updateResult.emit(result)
        }
    }

    fun removeMedicalCondition(condition: String) {
        viewModelScope.launch {
            userRepository.removeMedicalCondition(getCurrentUserId(), condition)
        }
    }

    fun removeAllergy(allergy: String) {
        viewModelScope.launch {
            userRepository.removeAllergy(getCurrentUserId(), allergy)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private fun getCurrentUserId(): String {
        return appPreferences.getUserId()
    }
}
