package com.climasaude

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.climasaude.databinding.ActivityMainBinding
import com.climasaude.presentation.viewmodels.DashboardViewModel
import com.climasaude.presentation.viewmodels.AlertsViewModel
import com.climasaude.presentation.viewmodels.ProfileViewModel
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
    private val profileViewModel: ProfileViewModel by viewModels()

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
    }

    // Launcher específico para permissão de chamada
    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            makeEmergencyCall()
        } else {
            Toast.makeText(this, "Permissão de chamada necessária para emergência.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
        setupBottomNavigation()
        setupDrawer()
        setupEmergencyButton()
        observeViewModels()
        checkPermissions()
        handleIntent(intent)
        setupBackPress()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                if (!navController.popBackStack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
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

            navController.navigate(item.itemId, null, navOptions)
            true
        }
    }

    private fun setupDrawer() {
        binding.navViewRight.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.action_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
            if (handled) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }
            handled
        }
    }

    private fun setupEmergencyButton() {
        binding.fabEmergency.setOnClickListener {
            checkCallPermissionAndMakeCall()
        }
    }

    private fun checkCallPermissionAndMakeCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            makeEmergencyCall()
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun makeEmergencyCall() {
        val contacts = profileViewModel.emergencyContacts.value
        if (contacts.isNotEmpty()) {
            val contact = contacts.first() // Disca para o primeiro contato cadastrado
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phone}"))
            startActivity(intent)
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Nenhum contato")
                .setMessage("Você não possui contatos de emergência cadastrados. Deseja cadastrar agora?")
                .setPositiveButton("Sim") { _, _ ->
                    navController.navigate(R.id.navigation_emergency_contacts)
                }
                .setNegativeButton("Não", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                }
                true
            }
            android.R.id.home -> navController.navigateUp() || super.onOptionsItemSelected(item)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                alertsViewModel.unreadCount.collect { count -> updateAlertsBadge(count) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.errorMessage.collect { error ->
                    error?.let {
                        showError(it)
                        dashboardViewModel.clearError()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.userProfile.collect { profile ->
                    profile?.let { updateNavHeader(it.name, it.email) }
                }
            }
        }
    }

    private fun updateNavHeader(name: String, email: String) {
        val headerView = binding.navViewRight.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.text_nav_name).text = name
        headerView.findViewById<TextView>(R.id.text_nav_email).text = email
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
        intent?.getStringExtra("navigate_to")?.let { target ->
            val destination = when (target) {
                "weather" -> R.id.navigation_weather
                "health" -> R.id.navigation_health
                else -> null
            }
            destination?.let { navController.navigate(it) }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout)
            .setMessage("Deseja sair da conta?")
            .setPositiveButton(R.string.logout) { _, _ -> logout() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun logout() {
        notificationUtils.cancelAllNotifications()
        profileViewModel.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()
}
