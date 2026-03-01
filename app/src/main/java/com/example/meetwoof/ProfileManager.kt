package com.example.meetwoof

import android.content.Context
import android.content.SharedPreferences

object ProfileManager {
    private const val PREFS_NAME = "MeetWoofPrefs"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_IS_USER_SET = "is_user_set"
    private const val KEY_IS_DOG_SET = "is_dog_set"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserProfile(context: Context, name: String) {
        val editor = getPrefs(context).edit()
        editor.putString(KEY_USER_NAME, name)
        editor.putBoolean(KEY_IS_USER_SET, true)
        editor.apply()
    }

    fun setDogProfileCreated(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_IS_DOG_SET, true).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "User") ?: "User"
    }

    fun canStartGame(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_USER_SET, false) && prefs.getBoolean(KEY_IS_DOG_SET, false)
    }
}