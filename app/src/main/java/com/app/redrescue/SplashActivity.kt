package com.app.redrescue

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {

    private lateinit var noInternetCard: CardView
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // keep splash in place
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        noInternetCard = findViewById(R.id.noInternetCard)
        btnRetry = findViewById(R.id.btnRetry)

        btnRetry.setOnClickListener {
            checkAndProceed()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkAndProceed()
        }, 500)
    }

    private fun checkAndProceed() {
        if (isConnectedToInternet()) {
            if (noInternetCard.visibility == View.VISIBLE) hideError() else goToMain()
        } else {
            showError()
        }
    }

    private fun showError() {
        if (noInternetCard.visibility != View.VISIBLE) {
            noInternetCard.visibility = View.VISIBLE
            noInternetCard.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
            )
        }
    }

    private fun hideError() {
        noInternetCard.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom).apply {
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(anim: Animation) {}
                    override fun onAnimationEnd(anim: Animation) {
                        noInternetCard.visibility = View.GONE
                        goToMain()
                    }
                    override fun onAnimationRepeat(anim: Animation) {}
                })
            }
        )
    }

    private fun goToMain() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        finish()
    }

    private fun isConnectedToInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}