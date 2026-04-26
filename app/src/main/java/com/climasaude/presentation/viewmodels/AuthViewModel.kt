package com.climasaude.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.climasaude.domain.usecases.AuthUseCases
import com.climasaude.domain.models.UserProfile
import com.climasaude.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthEvent {
    object NavigateToMain : AuthEvent()
    data class ShowSuccess(val message: String) : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
}

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

    // Usamos um buffer para garantir que eventos não sejam perdidos durante transições de ciclo de vida.
    private val _eventChannel = Channel<AuthEvent>(Channel.BUFFERED)
    val authEvents = _eventChannel.receiveAsFlow()

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = authUseCases.loginWithEmail(email, password)) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            _userProfile.value = user
                            _authState.value = AuthState.Authenticated(user)
                            _eventChannel.send(AuthEvent.NavigateToMain)
                        } else {
                            _eventChannel.send(AuthEvent.ShowError("Falha ao autenticar"))
                        }
                    }
                    is Resource.Error -> {
                        val errorMsg = result.message ?: "Erro ao autenticar"
                        _authState.value = AuthState.Error(errorMsg)
                        _eventChannel.send(AuthEvent.ShowError(errorMsg))
                    }
                    else -> {}
                }
            } finally {
                _isLoading.value = false
            }
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
            try {
                val result = authUseCases.registerWithEmail(email, password, confirmPassword, name)
                when (result) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            _userProfile.value = user
                            _authState.value = AuthState.Authenticated(user)
                            _eventChannel.send(AuthEvent.ShowSuccess("Conta criada com sucesso! Redirecionando..."))
                            _eventChannel.send(AuthEvent.NavigateToMain)
                        }
                    }
                    is Resource.Error -> {
                        val errorMsg = result.message ?: "Erro ao cadastrar"
                        _authState.value = AuthState.Error(errorMsg)
                        _eventChannel.send(AuthEvent.ShowError(errorMsg))
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _eventChannel.send(AuthEvent.ShowError("Erro inesperado no cadastro"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = authUseCases.loginWithGoogle(idToken)) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            _userProfile.value = user
                            _authState.value = AuthState.Authenticated(user)
                            _eventChannel.send(AuthEvent.NavigateToMain)
                        }
                    }
                    is Resource.Error -> {
                        _eventChannel.send(AuthEvent.ShowError(result.message ?: "Erro no login Google"))
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _eventChannel.send(AuthEvent.ShowError("Erro ao processar login Google"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = authUseCases.resetPassword(email)) {
                    is Resource.Success -> {
                        _eventChannel.send(AuthEvent.ShowSuccess("Email de recuperação enviado!"))
                    }
                    is Resource.Error -> {
                        _eventChannel.send(AuthEvent.ShowError(result.message ?: "Erro ao resetar senha"))
                    }
                    else -> {}
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authUseCases.logout()
                _userProfile.value = null
                _authState.value = AuthState.Unauthenticated
            } finally {
                _isLoading.value = false
            }
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
