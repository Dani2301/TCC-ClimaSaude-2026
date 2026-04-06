package com.climasaude.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.data.repository.UserRepository
import com.climasaude.data.repository.AuthRepository
import com.climasaude.domain.models.UserProfile
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateResult = MutableSharedFlow<Resource<String>>()
    val updateResult: SharedFlow<Resource<String>> = _updateResult.asSharedFlow()

    init {
        observeUserProfile()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ProfileViewModel", "Observando perfil para o ID: $userId")
            
            if (userId.isEmpty()) {
                Log.e("ProfileViewModel", "ERRO: UserId está vazio!")
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

    // Método atômico para atualizar tudo de uma vez e evitar perda de dados. Modificado por: Daniel
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
        }
    }

    fun addMedicalCondition(condition: String) {
        viewModelScope.launch {
            userRepository.addMedicalCondition(getCurrentUserId(), condition)
        }
    }

    fun removeMedicalCondition(condition: String) {
        viewModelScope.launch {
            userRepository.removeMedicalCondition(getCurrentUserId(), condition)
        }
    }

    fun addAllergy(allergy: String) {
        viewModelScope.launch {
            userRepository.addAllergy(getCurrentUserId(), allergy)
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
