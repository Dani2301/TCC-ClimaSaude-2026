package com.climasaude.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.climasaude.BuildConfig
import com.climasaude.R
import com.climasaude.databinding.ActivityLoginBinding
import com.climasaude.presentation.viewmodels.AuthViewModel
import com.climasaude.presentation.viewmodels.AuthState
import com.climasaude.utils.hide
import com.climasaude.utils.show
import com.climasaude.utils.showSnackbar
import com.climasaude.utils.ValidationUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.climasaude.MainActivity

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                authViewModel.loginWithGoogle(idToken)
            } ?: run {
                binding.root.showSnackbar("Erro: ID Token do Google não encontrado.")
            }
        } catch (e: ApiException) {
            binding.root.showSnackbar("Erro no login com Google: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupBiometricAuth()
        setupUI()
        observeViewModel()
    }

    private fun setupGoogleSignIn() {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) {
            gsoBuilder.requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        }
        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
    }

    private fun setupBiometricAuth() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    binding.root.showSnackbar(errString.toString())
                }
            }
        )
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica")
            .setSubtitle("Use sua biometria para entrar")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun setupUI() {
        binding.textInputLayoutPassword.setEndIconOnClickListener {
            val isVisible = binding.editTextPassword.transformationMethod == HideReturnsTransformationMethod.getInstance()
            binding.editTextPassword.transformationMethod = if (isVisible) PasswordTransformationMethod.getInstance() else HideReturnsTransformationMethod.getInstance()
            binding.textInputLayoutPassword.setEndIconDrawable(if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            binding.editTextPassword.setSelection(binding.editTextPassword.text?.length ?: 0)
        }

        binding.buttonLogin.setOnClickListener { performLogin() }
        binding.buttonGoogleSignIn.setOnClickListener { signInWithGoogle() }
        binding.textviewForgotPassword.setOnClickListener { navigateToForgotPassword() }
        binding.textviewSignUp.setOnClickListener { navigateToRegister() }
        binding.buttonBiometric.setOnClickListener { biometricPrompt.authenticate(promptInfo) }

        updateBiometricButton()
    }

    private fun updateBiometricButton() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.buttonBiometric.show()
        } else {
            binding.buttonBiometric.hide()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> navigateToMain()
                    is AuthState.Error -> binding.root.showSnackbar(state.message)
                    else -> {}
                }
            }
        }
        lifecycleScope.launch {
            authViewModel.isLoading.collect { isLoading -> updateLoadingState(isLoading) }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.show()
            binding.buttonLogin.isEnabled = false
            binding.buttonLogin.text = "Entrando..."
        } else {
            binding.progressBar.hide()
            binding.buttonLogin.isEnabled = true
            binding.buttonLogin.text = "Entrar"
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            binding.root.showSnackbar("Preencha todos os campos")
            return
        }

        if (!ValidationUtils.isValidEmail(email)) {
            binding.textInputLayoutEmail.error = "Email inválido"
            return
        }

        authViewModel.loginWithEmail(email, password)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun navigateToForgotPassword() {
        startActivity(Intent(this, ForgotPasswordActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        // Removido login automático por Google no onStart para evitar logins indesejados. Modificado por: Daniel
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair do App")
            .setMessage("Deseja fechar o aplicativo?")
            .setPositiveButton("Sair") { _, _ -> finish() }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
