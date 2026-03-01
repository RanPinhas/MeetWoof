package com.example.meetwoof

import android.app.Activity
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout

object NavigationManager {

    fun setupBottomNavigation(activity: Activity) {
        val navGame = activity.findViewById<LinearLayout>(R.id.navGame)
        val navParks = activity.findViewById<LinearLayout>(R.id.navParks)
        val navHome = activity.findViewById<LinearLayout>(R.id.navHome)
        val navDogs = activity.findViewById<LinearLayout>(R.id.navDogs)
        val navProfile = activity.findViewById<LinearLayout>(R.id.navProfile)

        navGame?.setOnClickListener {
            vibrate(it)
            navigateTo(activity, MatchFiltersActivity::class.java)
        }

        navParks?.setOnClickListener {
            vibrate(it)
            navigateTo(activity, MapActivity::class.java)
        }

        navHome?.setOnClickListener {
            vibrate(it)
            navigateTo(activity, HomeActivity::class.java)
        }

        navDogs?.setOnClickListener {
            vibrate(it)
            navigateTo(activity, MyDogsActivity::class.java)
        }

        navProfile?.setOnClickListener {
            vibrate(it)
            navigateTo(activity, UserProfileActivity::class.java)
        }
    }

    fun vibrate(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun navigateTo(currentActivity: Activity, targetActivity: Class<*>) {
        if (currentActivity::class.java != targetActivity) {
            val intent = Intent(currentActivity, targetActivity)


            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)

            currentActivity.startActivity(intent)

            currentActivity.finish()

            currentActivity.overridePendingTransition(0, 0)
        }
    }
}