package com.app.redrescue

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.app.redrescue.Fragments.DashboardFragment
import com.app.redrescue.Fragments.HistoryFragment
import com.app.redrescue.Fragments.ProfileFragment
import com.app.redrescue.Fragments.ReportsFragment
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.ismaeldivita.chipnavigation.ChipNavigationBar

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Firebase.messaging.subscribeToTopic("sos")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("SOS", "Subscribed to SOS topic successfully")
                } else {
                    Log.e("SOS", "Failed to subscribe to SOS topic")
                }
            }

        // Full screen window status bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val bottomNav = findViewById<ChipNavigationBar>(R.id.bottom_nav)
        drawerLayout = findViewById(R.id.drawer_layout1)

        if (savedInstanceState == null) {
            bottomNav.setItemSelected(R.id.home, true)
            navigateTo(DashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { id ->
            when (id) {
                R.id.report -> navigateTo(ReportsFragment())
                R.id.profile -> navigateTo(ProfileFragment())
                R.id.history -> navigateTo(HistoryFragment())
                R.id.home -> navigateTo(DashboardFragment())
                else -> navigateTo(DashboardFragment())
            }
            true
        }
    }

    private fun navigateTo(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f)
            .commit()
    }

    fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        exitDialog = AlertDialog.Builder(this)
            .setTitle("EXIT")
            .setMessage("Do you want to really exit?")
            .setPositiveButton("Exit") { _, _ -> finishAffinity() }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .create()
        exitDialog?.show()
    }

    override fun onPause() {
        super.onPause()
        exitDialog?.dismiss()
        exitDialog = null
    }
}
