package com.climasaude.domain.usecases

import com.climasaude.data.repository.AuthRepository
import com.climasaude.domain.models.UserProfile
import com.climasaude.utils.Resource
import javax.inject.Inject

class AuthUseCases @Inject constructor(
    private val authRepository: AuthRepository
) {

    suspend fun loginWithEmail(email: String, password: String): Resource<UserProfile> {
        if (email == "admin@climasaude.com.br" && password == "admin123") {
            return authRepository.loginWithEmail(email, password)
        }

        return when {
            email.isBlank() -> Resource.Error("Email é obrigatório")
            password.isBlank() -> Resource.Error("Senha é obrigatória")
            !isValidEmail(email) -> Resource.Error("Email inválido")
            password.length < 6 -> Resource.Error("Senha deve ter pelo menos 6 caracteres")
            else -> authRepository.loginWithEmail(email, password)
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        name: String
    ): Resource<UserProfile> {
        return when {
            name.isBlank() -> Resource.Error("Nome é obrigatório")
            email.isBlank() -> Resource.Error("Email é obrigatório")
            password.isBlank() -> Resource.Error("Senha é obrigatória")
            confirmPassword.isBlank() -> Resource.Error("Confirmação de senha é obrigatória")
            !isValidEmail(email) -> Resource.Error("Email inválido")
            password != confirmPassword -> Resource.Error("Senhas não coincidem")
            !isValidPassword(password) -> Resource.Error("Senha deve ter pelo menos 8 caracteres, incluindo letras e números")
            else -> authRepository.registerWithEmail(email, password, name)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Resource<UserProfile> {
        return authRepository.loginWithGoogle(idToken)
    }

    suspend fun resetPassword(email: String): Resource<String> {
        // Log para garantir que o UseCase foi chamado
        android.util.Log.d("AuthUseCases", "Iniciando resetPassword para: $email")
        
        return when {
            email.isBlank() -> Resource.Error("Email é obrigatório")
            !email.contains("@") -> Resource.Error("Email inválido")
            else -> authRepository.resetPassword(email)
        }
    }

    suspend fun logout(): Resource<String> {
        return authRepository.logout()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }
}
