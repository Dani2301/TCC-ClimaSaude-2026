package com.climasaude.ui.auth

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.climasaude.databinding.ActivityForgotPasswordBinding
import com.climasaude.presentation.viewmodels.AuthEvent
import com.climasaude.presentation.viewmodels.AuthViewModel
import com.climasaude.utils.ValidationUtils
import com.climasaude.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
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
            title = "Recuperar Senha"
        }
    }

    private fun setupUI() {
        binding.buttonResetPassword.setOnClickListener {
            performPasswordReset()
        }
    }

    private fun performPasswordReset() {
        val email = binding.editTextEmail.text?.toString().orEmpty().trim()

        // Validação: Evitar chamadas desnecessárias ao Firebase. Modificado por: Daniel
        if (!ValidationUtils.isValidEmail(email)) {
            binding.textInputLayoutEmail.error = "Por favor, insira um email válido"
            return
        } else {
            binding.textInputLayoutEmail.error = null
        }

        authViewModel.resetPassword(email)
    }

    private fun observeViewModel() {
        // Observar Loading State. Modificado por: Daniel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.isLoading.collect { isLoading ->
                    updateLoadingState(isLoading)
                }
            }
        }

        // Observar Eventos de tiro único. Modificado por: Daniel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authEvents.collect { event ->
                    when (event) {
                        is AuthEvent.ShowSuccess -> {
                            binding.root.showSnackbar(event.message)
                            // Pequeno delay para o usuário ler a mensagem antes de fechar a tela
                            binding.root.postDelayed({ finish() }, 2000)
                        }
                        is AuthEvent.ShowError -> {
                            binding.root.showSnackbar(event.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonResetPassword.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.buttonResetPassword.text = if (isLoading) "Enviando..." else "Recuperar Senha"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
