package com.climasaude.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.climasaude.MainActivity
import com.climasaude.databinding.ActivityRegisterBinding
import com.climasaude.presentation.viewmodels.AuthEvent
import com.climasaude.presentation.viewmodels.AuthViewModel
import com.climasaude.utils.PasswordStrength
import com.climasaude.utils.ValidationUtils
import com.climasaude.utils.hide
import com.climasaude.utils.show
import com.climasaude.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Criar Conta"
        }
    }

    private fun setupUI() {
        binding.editTextName.addTextChangedListener { validateName(it.toString()) }
        binding.editTextEmail.addTextChangedListener { validateEmail(it.toString()) }
        binding.editTextPassword.addTextChangedListener { validatePassword(it.toString()) }
        binding.editTextConfirmPassword.addTextChangedListener { validateConfirmPassword(it.toString()) }

        binding.buttonRegister.setOnClickListener {
            performRegister()
        }

        binding.textviewLogin.setOnClickListener {
            finish()
        }

        binding.checkboxTerms.setOnCheckedChangeListener { _, _ ->
            updateRegisterButtonState()
        }
    }

    private fun validateName(name: String) {
        binding.textInputLayoutName.error = if (name.isNotEmpty() && !ValidationUtils.isValidName(name)) {
            "Nome inválido (use apenas letras e acentos)"
        } else null
        updateRegisterButtonState()
    }

    private fun validateEmail(email: String) {
        binding.textInputLayoutEmail.error = if (email.isNotEmpty() && !ValidationUtils.isValidEmail(email)) {
            "Email inválido"
        } else null
        updateRegisterButtonState()
    }

    private fun validatePassword(password: String) {
        when {
            password.isEmpty() -> {
                binding.textInputLayoutPassword.error = null
                binding.passwordStrengthIndicator.hide()
            }
            password.length < 8 -> {
                binding.textInputLayoutPassword.error = "Mínimo de 8 caracteres"
                binding.passwordStrengthIndicator.hide()
            }
            !password.any { it.isDigit() } || !password.any { it.isLetter() } -> {
                binding.textInputLayoutPassword.error = "Use letras e números"
                binding.passwordStrengthIndicator.show()
                updatePasswordStrength(password)
            }
            else -> {
                binding.textInputLayoutPassword.error = null
                binding.passwordStrengthIndicator.show()
                updatePasswordStrength(password)
            }
        }
        updateRegisterButtonState()
    }

    private fun updatePasswordStrength(password: String) {
        val strength = ValidationUtils.getPasswordStrength(password)
        binding.passwordStrengthText.text = "Força da senha: ${strength.displayName}"
        binding.passwordStrengthBar.setBackgroundColor(strength.color)
        val params = binding.passwordStrengthBar.layoutParams as android.widget.LinearLayout.LayoutParams
        params.weight = when (strength) {
            PasswordStrength.VERY_WEAK -> 0.2f
            PasswordStrength.WEAK -> 0.4f
            PasswordStrength.MEDIUM -> 0.6f
            PasswordStrength.STRONG -> 0.8f
            PasswordStrength.VERY_STRONG -> 1.0f
        }
        binding.passwordStrengthBar.layoutParams = params
    }

    private fun validateConfirmPassword(confirmPassword: String) {
        val password = binding.editTextPassword.text.toString()
        binding.textInputLayoutConfirmPassword.error = if (confirmPassword.isNotEmpty() && confirmPassword != password) {
            "As senhas não coincidem"
        } else null
        updateRegisterButtonState()
    }

    private fun areFieldsValid(): Boolean {
        return ValidationUtils.isValidName(binding.editTextName.text.toString()) &&
                ValidationUtils.isValidEmail(binding.editTextEmail.text.toString()) &&
                ValidationUtils.isValidPassword(binding.editTextPassword.text.toString()) &&
                binding.editTextPassword.text.toString() == binding.editTextConfirmPassword.text.toString() &&
                binding.textInputLayoutName.error == null &&
                binding.textInputLayoutEmail.error == null &&
                binding.textInputLayoutPassword.error == null &&
                binding.textInputLayoutConfirmPassword.error == null
    }

    private fun updateRegisterButtonState() {
        // Se estiver carregando, mantemos desabilitado. Se não, verificamos a validade dos campos. Modificado por: Daniel
        if (authViewModel.isLoading.value) {
            binding.buttonRegister.isEnabled = false
        } else {
            binding.buttonRegister.isEnabled = areFieldsValid() && binding.checkboxTerms.isChecked
        }
    }

    private fun observeViewModel() {
        // Observar Eventos de tiro único (Navegação, Toasts, Snackbars) Modificado por: Daniel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authEvents.collect { event ->
                    when (event) {
                        is AuthEvent.NavigateToMain -> {
                            // Pequeno delay para o usuário ver o feedback de sucesso Modificado por: Daniel
                            binding.root.postDelayed({ navigateToMain() }, 1000)
                        }
                        is AuthEvent.ShowSuccess -> {
                            binding.root.showSnackbar(event.message)
                        }
                        is AuthEvent.ShowError -> {
                            binding.root.showSnackbar(event.message)
                        }
                    }
                }
            }
        }

        // Observar Loading State Modificado por: Daniel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
                    updateLoadingState(isLoading)
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.show()
            binding.buttonRegister.text = "Criando conta..."
        } else {
            binding.progressBar.hide()
            binding.buttonRegister.text = "Criar Conta"
        }
        
        // Bloquear/Desbloquear interação com campos durante o carregamento Modificado por: Daniel
        binding.buttonRegister.isEnabled = !isLoading && areFieldsValid() && binding.checkboxTerms.isChecked
        binding.editTextName.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.editTextConfirmPassword.isEnabled = !isLoading
        binding.checkboxTerms.isEnabled = !isLoading
    }

    private fun performRegister() {
        if (authViewModel.isLoading.value) return

        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()

        authViewModel.registerWithEmail(email, password, confirmPassword, name)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
