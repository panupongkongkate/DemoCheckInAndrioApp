package com.example.checkinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
class LoginActivity : AppCompatActivity() {
    
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    
    // SessionManager (inline class)
    private class SessionManager(private val activity: LoginActivity) {
        private val prefs = activity.getSharedPreferences("CheckInAppSession", AppCompatActivity.MODE_PRIVATE)
        
        fun isLoggedIn(): Boolean = prefs.getBoolean("isLoggedIn", false)
        fun setLoggedIn(isLoggedIn: Boolean) {
            prefs.edit().putBoolean("isLoggedIn", isLoggedIn).apply()
        }
        fun setUsername(username: String) {
            prefs.edit().putString("username", username).apply()
        }
    }
    
    private lateinit var sessionManager: SessionManager
    
    companion object {
        private const val ADMIN_USERNAME = "admin"
        private const val ADMIN_PASSWORD = "admin123"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Hide action bar
        supportActionBar?.hide()
        
        sessionManager = SessionManager(this)
        
        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            startMainActivity()
            return
        }
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
    }
    
    private fun setupListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }
    }
    
    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (username.isEmpty()) {
            etUsername.error = "กรุณากรอกชื่อผู้ใช้"
            etUsername.requestFocus()
            return
        }
        
        if (password.isEmpty()) {
            etPassword.error = "กรุณากรอกรหัสผ่าน"
            etPassword.requestFocus()
            return
        }
        
        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            sessionManager.setLoggedIn(true)
            sessionManager.setUsername(username)
            
            Toast.makeText(this, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()
            startMainActivity()
        } else {
            Toast.makeText(this, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            etPassword.text.clear()
            etPassword.requestFocus()
        }
    }
    
    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}