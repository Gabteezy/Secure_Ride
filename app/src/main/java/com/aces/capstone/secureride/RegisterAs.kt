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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityRegisterAsBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegisterAs : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var binding: ActivityRegisterAsBinding
    private lateinit var editTextPassword: EditText
    private lateinit var sharedPreferences: SharedPreferences


    private lateinit var credential: AuthCredential
    private lateinit var gmail: String


    private lateinit var googleSignInClient: GoogleSignInClient
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")

    private lateinit var userName: String
    private var userType = "UNKNOWN"



    companion object {
        private const val TAG = "GoogleActivity"

        //9001
        private const val RC_SIGN_IN = 9001

    }


    private val REQ_ONE_TAP = 2  // Can be any integer unique to the Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.app_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        binding.btnDriver.setOnClickListener {
            showRegisterForm("Driver")
        }

//        binding.btnAdmin.setOnClickListener {
//            showRegisterForm("Admin")
//        }

        binding.btnCommuter.setOnClickListener {
            showRegisterForm("Commuter")
        }

    }


    private fun showRegisterForm(userType: String) {

        Log.d("REGISTER", userType)
        val intent = Intent(this@RegisterAs, UserRegister::class.java)
        intent.putExtra("user", userType)
        startActivity(intent)
        finish()

    }


    private fun checkUserAccount() {
        binding.progressBar.visibility = View.VISIBLE
        Toast.makeText(this, "Checking Account...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "firebaseAuthWithGoogle: Checking User Account!")
        firebaseDatabaseReference.child("user")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "firebaseAuthWithGoogle: Checking User if ${firebaseAuth.currentUser!!.uid} Exist!")
                    if (snapshot.hasChild(firebaseAuth.currentUser!!.uid)) {
                        Log.d(TAG, "firebaseAuthWithGoogle: Retrieving User Details")
                        this@RegisterAs.userType = snapshot.child(firebaseAuth.currentUser!!.uid).child("type")
                            .getValue(String::class.java).toString()
                        this@RegisterAs.userName = snapshot.child(firebaseAuth.currentUser!!.uid).child("email")
                            .getValue(String::class.java).toString() + " " + snapshot.child(
                            firebaseAuth.currentUser!!.uid
                        ).child("Lastname").getValue(String::class.java).toString()

                        Log.d(TAG, "firebaseAuthWithGoogle:User Details ${"$userType - $userName"} Exist!")
                        logged()
                    } else {
                        Log.d(TAG, "firebaseAuthWithGoogle: User not Registered")
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@RegisterAs, "User not registered", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "firebaseAuthWithGoogle: Error Checking User due to ${error.message}")
                    Toast.makeText(this@RegisterAs, "onCancelled due to : " + error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun logged() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE

            val intent = when (userType) {
                "Driver" -> {
                    Log.d(TAG, "firebaseAuthWithGoogle: Hi $userName you Logged In as Driver")
                    Toast.makeText(this, "Logged In as Driver", Toast.LENGTH_LONG).show()
                    Intent(this, DriverDashboard::class.java)
                }
                "User" -> {
                    Log.d(TAG, "firebaseAuthWithGoogle: Hi $userName you Logged In as User")
                    Toast.makeText(this, "Logged In as User", Toast.LENGTH_LONG).show()
                    Intent(this, UserDashboard::class.java)
                }
                "Admin" -> {
                    Log.d(TAG, "firebaseAuthWithGoogle: Hi $userName you Logged In as Admin")
                    Toast.makeText(this, "Logged In as Admin", Toast.LENGTH_LONG).show()
                    Intent(this, AdminDashboard::class.java)
                }
                else -> {
                    Log.d(TAG, "firebaseAuthWithGoogle: Hi $userName you Logged In as Admin")
                    Toast.makeText(this, "Logged In as Admin", Toast.LENGTH_LONG).show()
                    Intent(this, AdminDashboard::class.java)
                }
            }

            intent.putExtra("user", userType)
            startActivity(intent)
            finish()
        }, 3000) // 3000 is the delayed time in milliseconds.
    }


}