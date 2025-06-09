package com.example.checkinapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.checkinapp.utils.SessionManager
import com.google.android.material.navigation.NavigationView

class MainActivityWithDrawer : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_with_drawer)
        
        sessionManager = SessionManager(this)
        
        // Initialize navigation components
        initializeNavigation()
        
        // Setup based on login status
        if (sessionManager.isLoggedIn) {
            setupNavigationWithDrawer()
        } else {
            hideDrawer()
        }
        
        // Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    hideDrawer()
                    supportActionBar?.hide()  // ซ่อน Top Bar สำหรับหน้า Login
                }
                R.id.step1RegistrationFragment -> {
                    if (sessionManager.isLoggedIn) {
                        showDrawer()
                        supportActionBar?.show()  // แสดง Top Bar
                        supportActionBar?.title = "ลงทะเบียน"
                    }
                }
                R.id.step2CameraFragment -> {
                    supportActionBar?.show()  // แสดง Top Bar
                    supportActionBar?.title = "ถ่ายรูป"
                }
                R.id.step3CheckInFragment -> {
                    supportActionBar?.show()  // แสดง Top Bar
                    supportActionBar?.title = "Check-in"
                }
            }
        }
    }

    private fun initializeNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        // Setup navigation controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun setupNavigationWithDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        // Update header with username
        updateNavigationHeader()

        // Configure app bar with drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.step1RegistrationFragment), // Top level destinations
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Enable drawer
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun hideDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportActionBar?.hide()  // ซ่อน Top Bar
    }

    private fun showDrawer() {
        if (sessionManager.isLoggedIn) {
            setupNavigationWithDrawer()
            supportActionBar?.show()  // แสดง Top Bar
        }
    }

    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.nav_username)
        usernameTextView.text = sessionManager.username ?: "Admin"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_checkin -> {
                // Navigate to check-in flow
                if (navController.currentDestination?.id != R.id.step1RegistrationFragment) {
                    navController.navigate(R.id.step1RegistrationFragment)
                }
            }
            R.id.nav_logout -> {
                showLogoutConfirmation()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("ออกจากระบบ")
            .setMessage("คุณต้องการออกจากระบบหรือไม่?")
            .setPositiveButton("ออกจากระบบ") { _, _ ->
                logout()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun logout() {
        sessionManager.logout()
        
        // Navigate back to login and clear back stack
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.startDestinationId, true)
            .build()
        navController.navigate(R.id.loginFragment, null, navOptions)
        
        // Hide drawer
        hideDrawer()
        
        Toast.makeText(this, "ออกจากระบบเรียบร้อย", Toast.LENGTH_SHORT).show()
    }

    // Called from LoginFragment after successful login
    fun onLoginSuccess() {
        showDrawer()
        supportActionBar?.show()  // แสดง Top Bar หลังจาก Login สำเร็จ
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (::appBarConfiguration.isInitialized) {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Handle back press based on current destination
            when (navController.currentDestination?.id) {
                R.id.loginFragment -> {
                    // Exit app from login screen
                    finish()
                }
                R.id.step1RegistrationFragment -> {
                    if (sessionManager.isLoggedIn) {
                        // Stay on check-in screen, don't go back to login
                        return
                    } else {
                        super.onBackPressed()
                    }
                }
                else -> {
                    super.onBackPressed()
                }
            }
        }
    }
}
