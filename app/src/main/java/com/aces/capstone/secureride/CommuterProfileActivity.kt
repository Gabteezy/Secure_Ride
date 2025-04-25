package com.aces.capstone.secureride.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData

class CommuterProfileActivity : AppCompatActivity() {

    private lateinit var btnAccount: TextView
    private lateinit var btnEmail: TextView
    private lateinit var btnAddress: TextView
    private lateinit var btnPassword: TextView
    private lateinit var btnBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commuter_profile)

        // Initialize views

        btnBack = findViewById(R.id.btnBack)
        btnAccount = findViewById(R.id.btnAccount)
        btnEmail = findViewById(R.id.btnEmail)
        btnAddress = findViewById(R.id.btnAddress)
        btnPassword = findViewById(R.id.btnPassword)

        // Get the commuter data from the intent
        val commuter = intent.getParcelableExtra<UserData>("commuter_data")

        // Display commuter details
        commuter?.let {

            btnAccount.text = "${it.firstname} ${it.lastname}" // Display first name
            btnEmail.text = it.email // Display email
            btnAddress.text = it.address // Display address
            btnPassword.text = "********" // Display masked password or handle accordingly
        }
        btnBack.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }
    }
}