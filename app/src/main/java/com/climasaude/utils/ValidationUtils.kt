package com.climasaude.utils

import android.util.Patterns
import java.util.*

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    fun getPasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.VERY_WEAK
        var score = 0
        score += if (password.length >= 8) 2 else 1
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    fun isValidName(name: String): Boolean {
        // QA Fix: Regex que permite letras de qualquer idioma latino (incluindo acentos brasileiros) e espaços
        val nameRegex = "^[\\p{L}\\s]{2,100}$".toRegex()
        return name.isNotBlank() && nameRegex.matches(name.trim())
    }

    fun validateUserRegistration(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        if (!isValidName(name)) errors.add("Nome inválido ou com caracteres especiais")
        if (!isValidEmail(email)) errors.add("E-mail inválido")
        if (!isValidPassword(password)) errors.add("Senha deve ter letras e números")
        if (password != confirmPassword) errors.add("As senhas não coincidem")
        return ValidationResult(errors.isEmpty(), errors)
    }

    data class ValidationResult(val isValid: Boolean, val errors: List<String> = emptyList())
}

enum class PasswordStrength(val displayName: String, val color: Int) {
    VERY_WEAK("Muito Fraca", android.graphics.Color.RED),
    WEAK("Fraca", android.graphics.Color.MAGENTA),
    MEDIUM("Média", android.graphics.Color.YELLOW),
    STRONG("Forte", android.graphics.Color.GREEN),
    VERY_STRONG("Muito Forte", android.graphics.Color.BLUE)
}
