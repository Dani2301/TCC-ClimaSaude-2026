package com.climasaude

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.climasaude.R
import com.climasaude.databinding.ActivityMainBinding
import com.climasaude.presentation.viewmodels.DashboardViewModel
import com.climasaude.presentation.viewmodels.AlertsViewModel
import com.climasaude.utils.Constants
import com.climasaude.utils.NotificationUtils
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.climasaude.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val alertsViewModel: AlertsViewModel by viewModels()

    @Inject
    lateinit var notificationUtils: NotificationUtils

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Location permission granted
                dashboardViewModel.refreshData()
            }
            else -> {
                showLocationPermissionDialog()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showNotificationPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
        setupBottomNavigation()
        observeViewModels()
        checkPermissions()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_weather,
                R.id.navigation_health,
                R.id.navigation_reports,
                R.id.navigation_profile
            )
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Update toolbar title based on navigation
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = when (destination.id) {
                R.id.navigation_dashboard -> "Dashboard"
                R.id.navigation_weather -> "Clima"
                R.id.navigation_health -> "Saúde"
                R.id.navigation_reports -> "Relatórios"
                R.id.navigation_alerts -> "Alertas"
                R.id.navigation_profile -> "Perfil"
                else -> "ClimaSaude"
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle reselection of current tab
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    dashboardViewModel.refreshData()
                }
                R.id.navigation_weather -> {
                    // Scroll to top or refresh
                }
                R.id.navigation_health -> {
                    // Scroll to top or refresh
                }
            }
        }
    }

    private fun observeViewModels() {
        // Observe alerts for badge
        lifecycleScope.launch {
            alertsViewModel.unreadCount.collect { count ->
                updateAlertsBadge(count)
            }
        }

        // Observe dashboard state for general UI updates
        lifecycleScope.launch {
            dashboardViewModel.dashboardState.collect { state ->
                // Handle loading states, errors, etc.
            }
        }

        // Observe error messages
        lifecycleScope.launch {
            dashboardViewModel.errorMessage.collect { error ->
                error?.let {
                    showError(it)
                    dashboardViewModel.clearError()
                }
            }
        }
    }

    private fun updateAlertsBadge(count: Int) {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.navigation_alerts)
        if (count > 0) {
            badge.number = count
            badge.isVisible = true
        } else {
            badge.isVisible = false
        }
    }

    private fun checkPermissions() {
        // Check location permissions
        if (!hasLocationPermission()) {
            requestLocationPermissions()
        }

        // Check notification permissions (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showLocationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissão de Localização")
            .setMessage("O ClimaSaude precisa acessar sua localização para fornecer informações meteorológicas precisas e recomendações personalizadas de saúde.")
            .setPositiveButton("Permitir") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissão de Notificações")
            .setMessage("Permitir notificações para receber alertas importantes sobre clima e lembretes de medicamentos?")
            .setPositiveButton("Permitir") { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.getStringExtra("navigate_to")) {
                "weather" -> {
                    navController.navigate(R.id.navigation_weather)
                }
                "health" -> {
                    navController.navigate(R.id.navigation_health)

                    // Handle specific medication reminder
                    it.getStringExtra("medication_id")?.let { medicationId ->
                        // Navigate to specific medication or show reminder dialog
                    }
                }
                "emergency" -> {
                    // Handle emergency alert
                    it.getStringExtra("alert_id")?.let { alertId ->
                        navController.navigate(R.id.navigation_alerts)
                        // Show specific alert
                    }
                }
                "dashboard" -> {
                    navController.navigate(R.id.navigation_dashboard)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshCurrentData()
                true
            }
            R.id.action_settings -> {
                navController.navigate(R.id.navigation_settings)
                true
            }
            R.id.action_alerts -> {
                navController.navigate(R.id.navigation_alerts)
                true
            }
            R.id.action_emergency -> {
                showEmergencyOptions()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshCurrentData() {
        val currentDestination = navController.currentDestination?.id
        when (currentDestination) {
            R.id.navigation_dashboard -> {
                dashboardViewModel.refreshData()
            }
            R.id.navigation_weather -> {
                // Trigger weather refresh
            }
            R.id.navigation_health -> {
                // Trigger health data refresh
            }
            R.id.navigation_reports -> {
                // Trigger reports refresh
            }
        }
    }

    private fun showEmergencyOptions() {
        val options = arrayOf(
            "Ligar para SAMU (192)",
            "Contatar Emergência Pessoal",
            "Enviar Localização",
            "Cancelar"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Opções de Emergência")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = android.net.Uri.parse("tel:192")
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED) {
                            startActivity(intent)
                        }
                    }
                    1 -> {
                        // Show emergency contacts
                        navController.navigate(R.id.navigation_emergency_contacts)
                    }
                    2 -> {
                        // Send location to emergency contacts
                        sendLocationToEmergencyContacts()
                    }
                }
            }
            .show()
    }

    private fun sendLocationToEmergencyContacts() {
        // Implementation for sending location to emergency contacts
        lifecycleScope.launch {
            try {
                // Get current location and send to emergency contacts
                // This would use the LocationUtils and emergency contacts repository
            } catch (e: Exception) {
                showError("Erro ao enviar localização: ${e.message}")
            }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair da Conta")
            .setMessage("Tem certeza que deseja sair da sua conta?")
            .setPositiveButton("Sair") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        // Clear all notifications
        notificationUtils.cancelAllNotifications()

        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (!navController.popBackStack()) {
            super.onBackPressed()
        }
    }
}
