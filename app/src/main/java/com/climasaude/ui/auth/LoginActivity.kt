package com.climasaude.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.climasaude.BuildConfig
import com.climasaude.MainActivity
import com.climasaude.databinding.ActivityLoginBinding
import com.climasaude.presentation.viewmodels.AuthViewModel
import com.climasaude.presentation.viewmodels.AuthEvent
import com.climasaude.presentation.viewmodels.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.loginWithGoogle(token)
                } ?: run {
                    Toast.makeText(this, "Erro: Token do Google não encontrado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                if (e.statusCode != 12501) {
                    Toast.makeText(this, "Erro no login Google: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupBackPressed()
        
        // Verificação de sessão existente
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && !viewModel.isLoading.value) {
            // Se já existe conta Google, tentamos validar o estado no ViewModel
            // O ViewModel deve ser a fonte da verdade
        }
    }

    private fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            if (validateInput(email, password)) {
                viewModel.loginWithEmail(email, password)
            }
        }

        binding.buttonGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }

        binding.textviewSignUp.setOnClickListener {
            navigateToRegister()
        }

        binding.textviewForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.editTextEmail.addTextChangedListener {
            binding.textInputLayoutEmail.error = null
        }

        binding.editTextPassword.addTextChangedListener {
            binding.textInputLayoutPassword.error = null
        }
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Garantimos o signOut para evitar que o Google retorne uma sessão cacheada problemática
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun validateInput(email: String, pass: String): Boolean {
        var isValid = true
        if (email.isBlank()) {
            binding.textInputLayoutEmail.error = "E-mail obrigatório"
            isValid = false
        }
        if (pass.isBlank()) {
            binding.textInputLayoutPassword.error = "Senha obrigatória"
            isValid = false
        }
        return isValid
    }

    private fun setupObservers() {
        // Observer para Loading
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }
        }

        // Observer para o Estado de Autenticação (Mais confiável para navegação)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    if (state is AuthState.Authenticated) {
                        navigateToMain()
                    }
                }
            }
        }

        // Observer para Eventos Únicos (Toasts e Erros)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authEvents.collect { event ->
                    when (event) {
                        is AuthEvent.NavigateToMain -> {
                            navigateToMain()
                        }
                        is AuthEvent.ShowError -> {
                            Toast.makeText(this@LoginActivity, event.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthEvent.ShowSuccess -> {
                            Toast.makeText(this@LoginActivity, event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(this@LoginActivity)
                    .setTitle("Sair do App")
                    .setMessage("Deseja fechar o aplicativo?")
                    .setPositiveButton("Sair") { _, _ -> finish() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGoogleSignIn.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun navigateToForgotPassword() {
        startActivity(Intent(this, ForgotPasswordActivity::class.java))
    }
}
