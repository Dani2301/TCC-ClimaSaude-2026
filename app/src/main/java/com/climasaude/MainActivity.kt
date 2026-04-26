package com.climasaude

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import com.climasaude.databinding.ActivityMainBinding
import com.climasaude.presentation.viewmodels.DashboardViewModel
import com.climasaude.presentation.viewmodels.AlertsViewModel
import com.climasaude.utils.NotificationUtils
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
            dashboardViewModel.refreshData()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
            if (!notificationsGranted) {
                // Notificações são importantes para os remédios
            }
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
                R.id.navigation_health -> "Medicamentos"
                R.id.navigation_reports -> "Relatórios"
                R.id.navigation_alerts -> "Alertas"
                R.id.navigation_profile -> "Perfil"
                else -> "ClimaSaude"
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) return@setOnItemSelectedListener false

            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
                .build()

            if (item.itemId == R.id.navigation_dashboard) {
                navController.popBackStack(R.id.navigation_dashboard, false)
            } else {
                navController.navigate(item.itemId, null, navOptions)
            }
            true
        }
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            alertsViewModel.unreadCount.collect { count -> updateAlertsBadge(count) }
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
        val permissionsNeeded = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val destination = when (it.getStringExtra("navigate_to")) {
                "weather" -> R.id.navigation_weather
                "health" -> R.id.navigation_health
                "dashboard" -> R.id.navigation_dashboard
                else -> null
            }
            destination?.let { id ->
                navController.navigate(id, null, NavOptions.Builder().setPopUpTo(R.id.navigation_dashboard, false).build())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair")
            .setMessage("Deseja sair da conta?")
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (navController.currentDestination?.id != R.id.navigation_dashboard) {
            navController.popBackStack(R.id.navigation_dashboard, false)
        } else {
            super.onBackPressed()
        }
    }
}
