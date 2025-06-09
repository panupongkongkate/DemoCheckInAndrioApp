package com.example.checkinapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.checkinapp.data.RegistrationData
import com.example.checkinapp.fragments.Step1RegistrationFragment
import com.example.checkinapp.fragments.Step2CameraFragment
import com.example.checkinapp.fragments.Step3CheckInFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    // SessionManager (temporary inline class)
    private class SessionManager(private val activity: MainActivity) {
        private val prefs = activity.getSharedPreferences("CheckInAppSession", MODE_PRIVATE)
        
        fun isLoggedIn(): Boolean = prefs.getBoolean("isLoggedIn", false)
        fun getUsername(): String? = prefs.getString("username", null)
        fun logout() {
            prefs.edit().clear().apply()
            val intent = Intent(activity, LoginActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }
    
    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    
    private lateinit var step1Circle: View
    private lateinit var step2Circle: View
    private lateinit var step3Circle: View
    private lateinit var line1: View
    private lateinit var line2: View
    
    private var currentStep = 1
    private var registrationData: RegistrationData? = null
    private var capturedPhoto: Bitmap? = null
    
    // Fragments
    private val step1Fragment = Step1RegistrationFragment()
    private val step2Fragment = Step2CameraFragment()
    private val step3Fragment = Step3CheckInFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize session manager
        sessionManager = SessionManager(this)
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main_with_drawer)
        
        initViews()
        setupDrawer()
        setupListeners()
        updateStepUI()
        
        // Show first step
        showFragment(step1Fragment)
        
        // Set up fragment listeners
        setupFragmentListeners()
    }
    
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)
        
        step1Circle = findViewById(R.id.step1_circle)
        step2Circle = findViewById(R.id.step2_circle)
        step3Circle = findViewById(R.id.step3_circle)
        line1 = findViewById(R.id.line1)
        line2 = findViewById(R.id.line2)
    }
    
    private fun setupListeners() {
        btnPrevious.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
                showCurrentStep()
            }
        }
        
        btnNext.setOnClickListener {
            when (currentStep) {
                1 -> {
                    if (validateStep1()) {
                        currentStep++
                        updateStepUI()
                        showCurrentStep()
                    }
                }
                2 -> {
                    if (validateStep2()) {
                        currentStep++
                        updateStepUI()
                        showCurrentStep()
                    }
                }
                3 -> {
                    // Final check in will be handled by Step3Fragment
                }
            }
        }
    }
    
    private fun setupFragmentListeners() {
        // Step 1 Fragment Listener
        step1Fragment.setOnDataLoadedListener { data ->
            this.registrationData = data
            updateStepUI() // Update UI to show enabled button
        }
        
        step1Fragment.setOnDataClearedListener {
            this.registrationData = null
            updateStepUI() // Update UI to disable button
        }
        
        // Step 2 Fragment Listener
        step2Fragment.setOnPhotoChangedListener { photo, detectionResults ->
            this.capturedPhoto = photo
        }
        
        // Step 3 Fragment Listener
        step3Fragment.setOnCheckInCompleteListener {
            // Reset data and go back to step 1
            resetCheckIn()
        }
    }
    
    private fun setupDrawer() {
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Check In System"
        
        // Set up drawer toggle
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        // Set navigation item listener
        navigationView.setNavigationItemSelectedListener(this)
        
        // Update header with username
        val headerView = navigationView.getHeaderView(0)
        val tvUsername = headerView.findViewById<TextView>(R.id.tv_username)
        tvUsername.text = "สวัสดี, ${sessionManager.getUsername() ?: "ผู้ใช้"}"
    }
    
    private fun validateStep1(): Boolean {
        return registrationData != null
    }
    
    private fun validateStep2(): Boolean {
        return true // Photo is optional
    }
    
    private fun showCurrentStep() {
        when (currentStep) {
            1 -> showFragment(step1Fragment)
            2 -> {
                // Check camera permission before showing camera step
                if (checkCameraPermission()) {
                    showFragment(step2Fragment)
                } else {
                    requestCameraPermission()
                }
            }
            3 -> showFragment(step3Fragment)
        }
    }
    
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, fragment)
            .commitNow()
        
        // Update summary for step 3 after fragment is shown
        if (fragment == step3Fragment) {
            step3Fragment.updateSummary(registrationData, capturedPhoto)
        }
    }
    
    private fun updateStepUI() {
        // Update step circles
        when (currentStep) {
            1 -> {
                step1Circle.setBackgroundResource(R.drawable.step_circle_active)
                step2Circle.setBackgroundResource(R.drawable.step_circle_inactive)
                step3Circle.setBackgroundResource(R.drawable.step_circle_inactive)
                line1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                line2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            2 -> {
                step1Circle.setBackgroundResource(R.drawable.step_circle_completed)
                step2Circle.setBackgroundResource(R.drawable.step_circle_active)
                step3Circle.setBackgroundResource(R.drawable.step_circle_inactive)
                line1.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
                line2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            3 -> {
                step1Circle.setBackgroundResource(R.drawable.step_circle_completed)
                step2Circle.setBackgroundResource(R.drawable.step_circle_completed)
                step3Circle.setBackgroundResource(R.drawable.step_circle_active)
                line1.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
                line2.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
            }
        }
        
        // Update navigation buttons
        btnPrevious.visibility = if (currentStep > 1) View.VISIBLE else View.INVISIBLE
        
        when (currentStep) {
            1 -> {
                btnNext.text = "ถัดไป"
                btnNext.visibility = View.VISIBLE
                btnNext.isEnabled = registrationData != null
            }
            2 -> {
                btnNext.text = "ถัดไป"
                btnNext.visibility = View.VISIBLE
                btnNext.isEnabled = true
            }
            3 -> {
                btnNext.visibility = View.GONE
            }
        }
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showFragment(step2Fragment)
                } else {
                    Toast.makeText(this, "ต้องการอนุญาตกล้องเพื่อถ่ายรูป", Toast.LENGTH_LONG).show()
                    currentStep = 1
                    updateStepUI()
                    showCurrentStep()
                }
            }
        }
    }
    
    private fun resetCheckIn() {
        currentStep = 1
        registrationData = null
        capturedPhoto = null
        
        updateStepUI()
        showCurrentStep()
        
        // Reset fragments after showing step 1
        step1Fragment.resetData()
        
        Toast.makeText(this, "Check In สำเร็จ!", Toast.LENGTH_LONG).show()
    }
    
    // Navigation methods
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_checkin -> {
                // Already on check in page, do nothing
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_logout -> {
                sessionManager.logout()
                return true
            }
        }
        return false
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}