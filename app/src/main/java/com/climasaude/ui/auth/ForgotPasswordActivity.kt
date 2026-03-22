package com.climasaude.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.climasaude.databinding.ActivityForgotPasswordBinding
import com.climasaude.presentation.viewmodels.AuthState
import com.climasaude.presentation.viewmodels.AuthViewModel
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

        binding.buttonResetPassword.setOnClickListener {
            val email = binding.editTextEmail.text?.toString().orEmpty().trim()
            if (email == "admin@climasaude.com.br") {
                binding.root.showSnackbar("Email de recuperação enviado para o administrador!")
            } else {
                authViewModel.resetPassword(email)
            }
        }

        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                if (state is AuthState.PasswordResetSent) {
                    binding.root.showSnackbar("Email de recuperação enviado!")
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.errorMessage.collect { error ->
                error?.let {
                    binding.root.showSnackbar(it)
                    authViewModel.clearError()
                }
            }
        }
    }

    private fun setupToolbar() {
        if (supportActionBar == null) {
            setSupportActionBar(binding.toolbar)
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recuperar Senha"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
