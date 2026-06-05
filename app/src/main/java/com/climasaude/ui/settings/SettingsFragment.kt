package com.climasaude.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import com.climasaude.data.preferences.AppPreferences
import com.climasaude.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBiometricSettings()
    }

    private fun setupBiometricSettings() {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.switchBiometric.isChecked = appPreferences.isBiometricEnabled()
            binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                appPreferences.setBiometricEnabled(isChecked)
                val message = if (isChecked) "Biometria ativada" else "Biometria desativada"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            // Se o dispositivo não suporta, podemos desabilitar ou esconder a opção
            binding.switchBiometric.isEnabled = false
            Toast.makeText(requireContext(), "Seu dispositivo não suporta biometria ou não possui digitais cadastradas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
