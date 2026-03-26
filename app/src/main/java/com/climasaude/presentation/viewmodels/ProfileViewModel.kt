package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.UserRepository
import com.climasaude.data.repository.AuthRepository
import com.climasaude.domain.models.UserProfile
import com.climasaude.data.database.entities.EmergencyContact
import com.climasaude.data.preferences.AppPreferences
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) return@launch

            _isLoading.value = true
            // Senior Fix: Confiar apenas no Flow do Banco de Dados. Modificado por: Daniel
            userRepository.getUserProfileFlow(userId)
                .onEach { _isLoading.value = false }
                .catch { _isLoading.value = false }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }
    }

    fun updatePersonalInfo(name: String, birthDate: Date?, gender: String?, weight: Float?, height: Float?) {
        val currentProfile = _userProfile.value ?: return
        val updatedProfile = currentProfile.copy(
            name = name,
            birthDate = birthDate,
            gender = gender,
            weight = weight,
            height = height
        )
        viewModelScope.launch {
            userRepository.updateUserProfile(updatedProfile)
            // Não precisa de refresh manual, o observeUserProfile cuidará disso. Modificado por: Daniel
        }
    }

    fun addMedicalCondition(condition: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            userRepository.addMedicalCondition(userId, condition)
        }
    }

    fun removeMedicalCondition(condition: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            userRepository.removeMedicalCondition(userId, condition)
        }
    }

    fun addAllergy(allergy: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            userRepository.addAllergy(userId, allergy)
        }
    }

    fun removeAllergy(allergy: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            userRepository.removeAllergy(userId, allergy)
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
