package com.climasaude.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.UserRepository
import com.climasaude.data.repository.AuthRepository
import com.climasaude.domain.models.UserProfile
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
            // Log Técnico para Daniel conferir no Logcat do dispositivo físico
            Log.d("ProfileViewModel", "Observando perfil para o ID: $userId")
            
            if (userId.isEmpty()) {
                Log.e("ProfileViewModel", "ERRO: UserId está vazio! O salvamento não funcionará.")
                return@launch
            }

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
            Log.d("ProfileViewModel", "Tentando salvar biometria: Peso $weight, Altura $height")
            userRepository.updateUserProfile(updatedProfile)
        }
    }

    fun addMedicalCondition(condition: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ProfileViewModel", "Adicionando condição: $condition")
            userRepository.addMedicalCondition(userId, condition)
        }
    }

    fun removeMedicalCondition(condition: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ProfileViewModel", "Removendo condição: $condition")
            userRepository.removeMedicalCondition(userId, condition)
        }
    }

    fun addAllergy(allergy: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ProfileViewModel", "Adicionando alergia: $allergy")
            userRepository.addAllergy(userId, allergy)
        }
    }

    fun removeAllergy(allergy: String) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ProfileViewModel", "Removendo alergia: $allergy")
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
