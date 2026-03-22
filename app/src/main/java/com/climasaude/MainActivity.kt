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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
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

        // Solução para Navegação Reversa. Modificado por: Daniel
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) {
                // Se já estiver na aba, não faz nada (ou faz refresh)
                return@setOnItemSelectedListener false
            }

            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
                .build()

            // Forçar retorno para o Dashboard se o botão for clicado. Modificado por: Daniel
            if (item.itemId == R.id.navigation_dashboard) {
                navController.popBackStack(R.id.navigation_dashboard, false)
            } else {
                navController.navigate(item.itemId, null, navOptions)
            }
            true
        }

        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_dashboard) {
                // Se clicar no Dashboard estando em uma tela "em cima" dele, volta para o Dashboard. Modificado por: Daniel
                navController.popBackStack(R.id.navigation_dashboard, false)
                dashboardViewModel.refreshData()
            }
        }
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            alertsViewModel.unreadCount.collect { count ->
                updateAlertsBadge(count)
            }
        }

        lifecycleScope.launch {
            dashboardViewModel.dashboardState.collect { state -> }
        }

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
        badge.number = count
        badge.isVisible = count > 0
    }

    private fun checkPermissions() {
        if (!hasLocationPermission()) {
            requestLocationPermissions()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showLocationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissão de Localização")
            .setMessage("O ClimaSaude precisa acessar sua localização para fornecer informações meteorológicas precisas.")
            .setPositiveButton("Permitir") { _, _ -> requestLocationPermissions() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissão de Notificações")
            .setMessage("Permitir notificações para receber alertas importantes?")
            .setPositiveButton("Permitir") { _, _ -> requestNotificationPermission() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val destination = when (it.getStringExtra("navigate_to")) {
                "weather" -> R.id.navigation_weather
                "health" -> R.id.navigation_health
                "emergency" -> R.id.navigation_alerts
                "dashboard" -> R.id.navigation_dashboard
                else -> null
            }
            
            destination?.let { id ->
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_dashboard, inclusive = false)
                    .setLaunchSingleTop(true)
                    .build()
                navController.navigate(id, null, navOptions)
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
        when (navController.currentDestination?.id) {
            R.id.navigation_dashboard -> dashboardViewModel.refreshData()
        }
    }

    private fun showEmergencyOptions() {
        val options = arrayOf("Ligar para SAMU (192)", "Contatar Emergência Pessoal", "Cancelar")
        MaterialAlertDialogBuilder(this)
            .setTitle("Opções de Emergência")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val callIntent = Intent(Intent.ACTION_CALL).apply { data = android.net.Uri.parse("tel:192") }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            startActivity(callIntent)
                        }
                    }
                    1 -> navController.navigate(R.id.navigation_emergency_contacts)
                }
            }
            .show()
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair da Conta")
            .setMessage("Tem certeza que deseja sair?")
            .setPositiveButton("Sair") { _, _ -> logout() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        notificationUtils.cancelAllNotifications()
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
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
        if (navController.currentDestination?.id == R.id.navigation_dashboard) {
            super.onBackPressed()
        } else {
            if (!navController.popBackStack()) {
                super.onBackPressed()
            }
        }
    }
}
