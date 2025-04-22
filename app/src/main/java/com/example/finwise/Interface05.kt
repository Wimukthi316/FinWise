package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Interface05 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interface05)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { navigateToScreen04() }

        // Navigate to Screen05 when clicking the "NEXT" button
        val nextButton = findViewById<Button>(R.id.loginButton)
        nextButton.setOnClickListener {
            val intent = Intent(this, Interface06::class.java)
            startActivity(intent)
        }

    }
    private fun navigateToScreen04() {
        val intent = Intent(this, Interface04::class.java)
        startActivity(intent)
    }
}