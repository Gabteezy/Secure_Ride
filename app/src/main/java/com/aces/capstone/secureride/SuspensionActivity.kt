package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SuspensionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_suspension) // Reuse your layout

        val daysRemaining = intent.getLongExtra("DAYS_REMAINING", -1L)

        // Update suspension message
        val message = if (daysRemaining == -1L) {
            "Your account is suspended, but we couldn't retrieve the suspension end date."
        } else {
            "Your account is suspended for $daysRemaining day${if (daysRemaining != 1L) "s" else ""}."
        }
        findViewById<TextView>(R.id.tvSuspensionMessage).text = message

        // Handle OK button click
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginUser::class.java))
            finish()
        }

        // Handle Exit button click
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExit).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finishAffinity()
            System.exit(0)
        }
    }
}