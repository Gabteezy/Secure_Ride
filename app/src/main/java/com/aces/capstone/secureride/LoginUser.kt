package com.aces.capstone.secureride

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginUserBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class LoginUser : AppCompatActivity() {

    private lateinit var binding: ActivityLoginUserBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private lateinit var credential: AuthCredential
    private var userType = "UNKNOWN"

    companion object {
        private const val TAG = "LoginUser"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.app_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        binding.registerAs.setOnClickListener {
            startActivity(Intent(this, RegisterAs::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.username.text.toString().trim()
            val password = binding.passWord.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                handleEmptyFields()
            } else if (!ValidEmail(username)) {
                binding.username.error = "Please enter a valid email address."
            } else {
                signInWithEmailAndPassword(username, password)
            }
        }

        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun handleEmptyFields() {
        if (binding.username.text!!.isEmpty()) {
            binding.username.error = "Please enter a valid email."
        }
        if (binding.passWord.text!!.isEmpty()) {
            binding.passWord.error = "Please enter a password."
        }
        Toast.makeText(this, "Please check the error(s)!", Toast.LENGTH_SHORT).show()
    }

    private fun signInWithEmailAndPassword(username: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE // Show progress bar
        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE // Hide progress bar
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully logged in")
                    checkUserAccount()
                } else {
                    Log.e(TAG, "Login failed", task.exception)
                    Toast.makeText(this, "Invalid username or password! Please try again!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun ValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                Log.d(TAG, "Google sign-in successful")
                checkUserAccount()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Google sign-in failed", e)
            }
    }

    private fun checkUserAccount() {
        binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Checking account...", Toast.LENGTH_SHORT).show()

        firebaseDatabaseReference.child("user")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser != null && snapshot.hasChild(currentUser.uid)) {
                        userType = snapshot.child(currentUser.uid).child("type").getValue(String::class.java) ?: "UNKNOWN"
                        logged(userType)
                    } else {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginUser, "User not registered", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@LoginUser, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Error checking user: ${error.message}")
                }
            })
    }

    private fun logged(userType: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
            val intent = when (userType) {
                "Driver" -> Intent(this, DriverDashboard::class.java)
                "Commuter" -> Intent(this, UserDashboard::class.java)
                "Admin" -> Intent(this, AdminDashboard::class.java)
                else -> Intent(this, AdminDashboard::class.java)
            }
            startActivity(intent)
            finish()
        }, 3000)
    }
}
