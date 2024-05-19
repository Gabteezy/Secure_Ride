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
            val password = binding.passWord.text.toString()
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
                    binding.passWord.error = "Please enter your Password!"
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
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            firebaseDatabaseReference.child("user")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.hasChild(firebaseAuth.currentUser?.uid ?: "")) {
                                            Log.d("REGISTER", "User is already Registered!")
                                            Toast.makeText(this@UserRegister, "User is already Registered!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val userId = firebaseAuth.currentUser?.uid ?: ""
                                            if (userId.isNotEmpty()) {
                                                val databaseRef = firebaseDatabase.reference.child("user").child(userId)

                                                user = UserData(
                                                    userId,
                                                    email,
                                                    firstName,
                                                    lastName,
                                                    phone,
                                                    password,
                                                    userType,
                                                    false.toString()
                                                )

                                                databaseRef.setValue(user)
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            Log.d("REGISTER", "User has been successfully Registered!")
                                                            Toast.makeText(
                                                                this@UserRegister,
                                                                "User has been successfully Registered!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            firebaseAuth.signOut()
                                                            val intent = Intent(this@UserRegister, LoginActivity::class.java)
                                                            startActivity(intent)
                                                        } else {
                                                            Log.e("REGISTER", "Error registering user: ${task.exception}")
                                                            Toast.makeText(
                                                                this@UserRegister,
                                                                "Error registering user: ${task.exception?.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                            } else {
                                                Log.e("REGISTER", "Current user ID is null or empty.")
                                                Toast.makeText(this@UserRegister, "Current user ID is null or empty.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }


                                    override fun onCancelled(error: DatabaseError) {
                                        Log.d(
                                            "REGISTER",
                                            "Error in Registering due to ${error.message}!"
                                        )
                                        Toast.makeText(
                                            this@UserRegister,
                                            "Error in Registering due to ${error.message}!",
                                            Toast.LENGTH_LONG
                                        )
                                    }

                                })
                        } else {
                            Log.d(
                                "REGISTER", it.exception!!.message.toString()
                            )
                            binding.passWord.error = it.exception!!.message.toString()
                            Toast.makeText(
                                this@UserRegister,
                                it.exception!!.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

    }
}