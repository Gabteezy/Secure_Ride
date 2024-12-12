package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityUserRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserRegister : AppCompatActivity() {

    private lateinit var binding: ActivityUserRegisterBinding
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var user: UserData
    private lateinit var userType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        userType = bundle!!.getString("user").toString()
        binding.userDisplayName.text = "Create an $userType Account"

        // Restrict phone input to digits only
        binding.phone.filters = arrayOf(
            android.text.InputFilter.LengthFilter(11), // Limit input length to 11 digits
            android.text.InputFilter { source, _, _, _, _, _ ->
                if (source.matches(Regex("^[0-9]*$"))) {
                    source // Allow digits
                } else {
                    "" // Block non-digits
                }
            }
        )

        binding.btnSubmit.setOnClickListener {
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val email = binding.email.text.toString()
            val phone = binding.phone.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            if (!binding.checkBox.isChecked) {
                Toast.makeText(this@UserRegister, "You must accept the Terms and Conditions to register.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate input fields
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                if (firstName.isEmpty()) binding.firstName.error = "Please enter your first name!"
                if (lastName.isEmpty()) binding.lastName.error = "Please enter your last name!"
                if (email.isEmpty()) binding.email.error = "Please enter your email!"
                if (phone.isEmpty()) binding.phone.error = "Please enter your phone!"
                if (password.isEmpty()) binding.password.error = "Please enter your password!"
                if (confirmPassword.isEmpty()) binding.confirmPassword.error = "Please confirm your password!"
                return@setOnClickListener
            }

            // Validate phone number format (digits only, at least 10 digits)
            if (!phone.matches(Regex("^[0-9]{11}$"))) {
                binding.phone.error = "Please enter a valid 11-digit phone number"
                return@setOnClickListener
            }

            // Validate password match
            if (password != confirmPassword) {
                binding.confirmPassword.error = "Password does not match!"
                Toast.makeText(this@UserRegister, "Password did not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register user with Firebase
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid ?: ""
                    if (userId.isNotEmpty()) {
                        firebaseDatabaseReference.child("user").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        Log.d("REGISTER", "$userType is already registered!")
                                        Toast.makeText(this@UserRegister, "$userType is already registered!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        user = UserData(userId, email, firstName, lastName, phone, password, userType, "false")
                                        val databaseRef = firebaseDatabaseReference.child("user").child(userId)
                                        databaseRef.setValue(user).addOnCompleteListener { saveTask ->
                                            if (saveTask.isSuccessful) {
                                                firebaseAuth.currentUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                                    if (verifyTask.isSuccessful) {
                                                        Log.d("REGISTER", "Verification email sent!")
                                                        Toast.makeText(this@UserRegister, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                                        showEmailVerificationDialog()
                                                    } else {
                                                        Log.e("REGISTER", "Failed to send verification email: ${verifyTask.exception?.message}")
                                                        Toast.makeText(this@UserRegister, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                Log.e("REGISTER", "Error saving user data: ${saveTask.exception?.message}")
                                                Toast.makeText(this@UserRegister, "Error saving user data: ${saveTask.exception?.message}", Toast.LENGTH_SHORT).show()
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

    private fun showEmailVerificationDialog() {
        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        progressBar.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.addView(progressBar)

        val builder = AlertDialog.Builder(this)
            .setTitle("Email Verification")
            .setMessage("A verification email has been sent to your inbox. Please verify your email to proceed.")
            .setCancelable(false)
            .setView(layout)

        val dialog = builder.create()
        dialog.show()

        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                firebaseAuth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        if (firebaseAuth.currentUser?.isEmailVerified == true) {
                            progressBar.visibility = ProgressBar.INVISIBLE
                            dialog.dismiss()
                            Toast.makeText(this@UserRegister, "Email verified! You can now log in.", Toast.LENGTH_SHORT).show()
                            firebaseAuth.signOut()
                            startActivity(Intent(this@UserRegister, LoginUser::class.java))
                            finish()
                        } else {
                            handler.postDelayed(this, 3000)
                        }
                    } else {
                        Log.e("REGISTER", "Failed to reload user: ${reloadTask.exception?.message}")
                    }
                }
            }
        }
        handler.post(runnable)

        dialog.setOnDismissListener {
            handler.removeCallbacks(runnable)
        }
    }
}
