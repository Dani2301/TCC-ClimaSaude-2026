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
                binding.root.showSnackbar("Erro: ID Token do Google não encontrado. Verifique a configuração do Firebase.")
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
            .setSubtitle("Use sua impressão digital ou face para fazer login")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun setupUI() {
        // Password visibility toggle
        binding.textInputLayoutPassword.setEndIconOnClickListener {
            val isPasswordVisible = binding.editTextPassword.transformationMethod == HideReturnsTransformationMethod.getInstance()

            if (isPasswordVisible) {
                binding.editTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.textInputLayoutPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                binding.editTextPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.textInputLayoutPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }

            binding.editTextPassword.setSelection(binding.editTextPassword.text?.length ?: 0)
        }

        // Real-time validation
        binding.editTextEmail.addTextChangedListener { text ->
            validateEmail(text.toString())
        }

        binding.editTextPassword.addTextChangedListener { text ->
            validatePassword(text.toString())
        }

        // Click listeners
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        binding.buttonGoogleSignIn.setOnClickListener {
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
                binding.root.showSnackbar("Google Sign-In não configurado. Adicione o GOOGLE_WEB_CLIENT_ID no build.gradle.")
            } else {
                signInWithGoogle()
            }
        }

        binding.textviewForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.textviewSignUp.setOnClickListener {
            navigateToRegister()
        }

        binding.buttonBiometric.setOnClickListener {
            authenticateWithBiometric()
        }

        // Check if biometric is available
        updateBiometricButton()
    }

    private fun validateEmail(email: String) {
        if (email.isNotEmpty() && !ValidationUtils.isValidEmail(email)) {
            binding.textInputLayoutEmail.error = "Email inválido"
        } else {
            binding.textInputLayoutEmail.error = null
        }
    }

    private fun validatePassword(password: String) {
        if (password.isNotEmpty() && password.length < 6) {
            binding.textInputLayoutPassword.error = "Senha deve ter pelo menos 6 caracteres"
        } else {
            binding.textInputLayoutPassword.error = null
        }
    }

    private fun updateBiometricButton() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.buttonBiometric.show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.buttonBiometric.hide()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        navigateToMain()
                    }
                    is AuthState.Error -> {
                        binding.root.showSnackbar(state.message)
                    }
                    is AuthState.PasswordResetSent -> {
                        binding.root.showSnackbar("Email de recuperação enviado!")
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.isLoading.collect { isLoading ->
                updateLoadingState(isLoading)
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

    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.show()
            binding.buttonLogin.isEnabled = false
            binding.buttonGoogleSignIn.isEnabled = false
            binding.buttonLogin.text = "Entrando..."
        } else {
            binding.progressBar.hide()
            binding.buttonLogin.isEnabled = true
            binding.buttonGoogleSignIn.isEnabled = true
            binding.buttonLogin.text = "Entrar"
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        // Atalho para login de teste: se ambos estiverem vazios, preenche com admin
        if (email.isEmpty() && password.isEmpty()) {
            val testEmail = "admin@climasaude.com.br"
            val testPassword = "admin123"
            binding.editTextEmail.setText(testEmail)
            binding.editTextPassword.setText(testPassword)
            authViewModel.loginWithEmail(testEmail, testPassword)
            return
        }

        // Clear previous errors
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null

        // Validate inputs
        var hasError = false

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email é obrigatório"
            hasError = true
        } else if (!ValidationUtils.isValidEmail(email)) {
            binding.textInputLayoutEmail.error = "Email inválido"
            hasError = true
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Senha é obrigatória"
            hasError = true
        } else if (password.length < 6) {
            binding.textInputLayoutPassword.error = "Senha deve ter pelo menos 6 caracteres"
            hasError = true
        }

        if (!hasError) {
            authViewModel.loginWithEmail(email, password)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun authenticateWithBiometric() {
        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already signed in with Google
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // User is signed in with Google, authenticate with Firebase
            account.idToken?.let { idToken ->
                authViewModel.loginWithGoogle(idToken)
            }
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair do App")
            .setMessage("Deseja sair do ClimaSaude?")
            .setPositiveButton("Sair") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
