package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityUserRegisterBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserRegister : AppCompatActivity() {

    private lateinit var binding: ActivityUserRegisterBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        binding.btnSubmit.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.passWord.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signupUser(email, password)
            } else {
                showToast("All fields are mandatory")
            }
        }
    }

    private fun signupUser(username: String, password: String) {
        databaseReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    val id = databaseReference.push().key
                    val userData = UserData(id, username, password)
                    databaseReference.child(id!!).setValue(userData)
                    showToast("Signup Successful")
                    startActivity(Intent(this@UserRegister, LoginActivity::class.java))
                    finish()
                } else {
                    showToast("User already exists")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Database Error: ${databaseError.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this@UserRegister, message, Toast.LENGTH_SHORT).show()
    }
}
