package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        binding.btnLogin.setOnClickListener {
            val email = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                showToast("All fields are mandatory")
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, UserRegister::class.java))
            finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var loginSuccessful = false
                    for (userSnapshot in dataSnapshot.children) {
                        val userData = userSnapshot.getValue(UserData::class.java)
                        if (userData != null && userData.password == password) {
                            showToast("Login Successful")
                            startActivity(Intent(this@LoginActivity, UserDashboard::class.java))
                            finish()
                            loginSuccessful = true
                            break
                        }
                    }
                    if (!loginSuccessful) {
                        showToast("Invalid email or password")
                    }
                } else {
                    showToast("User does not exist")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Database Error: ${databaseError.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }
}

