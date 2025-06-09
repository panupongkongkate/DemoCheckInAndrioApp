package com.example.checkinapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.checkinapp.MainActivityWithDrawer
import com.example.checkinapp.R
import com.example.checkinapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        // Check if already logged in
        if (sessionManager.isLoggedIn) {
            navigateToCheckin()
            return
        }

        // Initialize views
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)

        // Set click listener for login button
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateLogin(username, password)) {
                // Set login session
                sessionManager.login(username)
                
                // Notify activity of successful login
                (activity as? MainActivityWithDrawer)?.onLoginSuccess()
                
                // Show success message
                Toast.makeText(context, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()
                
                // Navigate to check-in
                navigateToCheckin()
            } else {
                showError()
            }
        }
    }

    private fun validateLogin(username: String, password: String): Boolean {
        return username == "admin" && password == "admin123"
    }

    private fun navigateToCheckin() {
        findNavController().navigate(R.id.action_loginFragment_to_step1RegistrationFragment)
    }

    private fun showError() {
        Toast.makeText(
            context,
            "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง",
            Toast.LENGTH_SHORT
        ).show()
    }
}
