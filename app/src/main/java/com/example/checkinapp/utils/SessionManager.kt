package com.example.checkinapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        private const val PREF_NAME = "CheckInAppSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USERNAME = "username"
    }

    var isLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) {
            editor.putBoolean(KEY_IS_LOGGED_IN, value)
            editor.apply()
        }

    var username: String?
        get() = sharedPreferences.getString(KEY_USERNAME, null)
        set(value) {
            editor.putString(KEY_USERNAME, value)
            editor.apply()
        }

    fun login(username: String) {
        this.username = username
        isLoggedIn = true
    }

    fun logout() {
        editor.clear()
        editor.apply()
    }
}
