package com.app.redrescue

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SOS : AppCompatActivity() {
    private lateinit var btnClose: ImageView
    private lateinit var optionTrusted: LinearLayout
    private lateinit var optionAdmin: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sos)

        btnClose = findViewById(R.id.btnClose)
        optionTrusted = findViewById(R.id.optionTrusted)
        optionAdmin = findViewById(R.id.optionAdmin)

        btnClose.setOnClickListener {
            finish()
        }

        optionTrusted.setOnClickListener {
            val intent = Intent(this,Contacts::class.java)
            startActivity(intent)
        }

        optionAdmin.setOnClickListener {
            // TODO: Implement admin alert logic here
        }
        
    }
}