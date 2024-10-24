package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityUserRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserRegister : AppCompatActivity() {

    private lateinit var binding: ActivityUserRegisterBinding

    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var user: UserData
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras

// getting the string back
        userType = bundle!!.getString("user").toString()

        binding.userDisplayName.text = "Create an $userType Account"


        binding.btnSubmit.setOnClickListener {

            val email = binding.email.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()
            val password = binding.password.text.toString()
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val phone = binding.phone.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                if (firstName.isEmpty()) {
                    binding.firstName.error = "Please enter your Name first!"
                }
                if (lastName.isEmpty()) {
                    binding.lastName.error = "Please enter your Name Last!"
                }

                if (email.isEmpty()) {
                    binding.email.error = "Please enter your Email!"
                }
                if (phone.isEmpty()) {
                    binding.phone.error = "Please enter your Phone!"
                }
                if (password.isEmpty()) {
                    binding.password.error = "Please enter your Password!"
                }
                if (confirmPassword.isEmpty()) {
                    binding.confirmPassword.error = "Please confirm your Password!"
                }

            } else {
                if (!confirmPassword.equals(password, false)) {
                    binding.confirmPassword.error = "Password not match!"
                    Toast.makeText(
                        this@UserRegister,
                        "Password did not match!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = firebaseAuth.currentUser?.uid ?: ""
                            if (userId.isNotEmpty()) {
                                firebaseDatabaseReference.child("user").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            Log.d("REGISTER", "$userType is already registered!")
                                            Toast.makeText(this@UserRegister, "$userType is already registered!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            user = UserData(userId, email, firstName, lastName, phone, password, userType, "false")
                                            val databaseRef = firebaseDatabase.reference.child("user").child(userId)

                                            databaseRef.setValue(user).addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Log.d("REGISTER", "$userType has been successfully registered!")
                                                    Toast.makeText(this@UserRegister, "$userType has been successfully registered!", Toast.LENGTH_SHORT).show()

                                                    // Sign out and redirect to login
                                                    firebaseAuth.signOut()
                                                    startActivity(Intent(this@UserRegister, LoginActivity::class.java))
                                                    finish() // Call finish to remove the registration activity from the back stack
                                                } else {
                                                    Log.e("REGISTER", "Error saving user data: ${task.exception?.message}")
                                                    Toast.makeText(this@UserRegister, "Error saving user data: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("REGISTER", "Database error: ${error.message}")
                                        Toast.makeText(this@UserRegister, "Database error: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                })
                            } else {
                                Log.e("REGISTER", "User ID is null or empty.")
                                Toast.makeText(this@UserRegister, "User ID is null or empty.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("REGISTER", "Registration failed: ${task.exception?.message}")
                            Toast.makeText(this@UserRegister, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

            }
        }

    }
}