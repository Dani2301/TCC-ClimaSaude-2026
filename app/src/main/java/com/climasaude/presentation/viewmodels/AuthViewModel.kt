package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.domain.usecases.AuthUseCases
import com.climasaude.domain.models.UserProfile
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authUseCases.loginWithEmail(email, password)) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        _userProfile.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Error("Falha ao autenticar")
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.Error(result.message ?: "Erro ao autenticar")
                }
                is Resource.Loading -> Unit
            }

            _isLoading.value = false
        }
    }

    fun registerWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        name: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authUseCases.registerWithEmail(email, password, confirmPassword, name)) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        _userProfile.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Error("Falha ao cadastrar")
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.Error(result.message ?: "Erro ao cadastrar")
                }
                is Resource.Loading -> Unit
            }

            _isLoading.value = false
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authUseCases.loginWithGoogle(idToken)) {
                is Resource.Success -> {
                    val user = result.data
                    if (user != null) {
                        _userProfile.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Error("Falha no login com Google")
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                    _authState.value = AuthState.Error(result.message ?: "Erro no login com Google")
                }
                is Resource.Loading -> Unit
            }

            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = authUseCases.resetPassword(email)) {
                is Resource.Success -> {
                    _authState.value = AuthState.PasswordResetSent
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> Unit
            }

            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = authUseCases.logout()) {
                is Resource.Success -> {
                    _userProfile.value = null
                    _authState.value = AuthState.Unauthenticated
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> Unit
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setInitialState() {
        _authState.value = AuthState.Initial
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
    object PasswordResetSent : AuthState()
}
