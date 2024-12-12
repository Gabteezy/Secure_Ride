package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aces.capstone.secureride.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Account : AppCompatActivity() {

    private lateinit var userType: String
    private lateinit var user: UserData
    private lateinit var binding: ActivityAccountBinding
    private lateinit var bundle: Bundle
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize bundle and retrieve user type if it exists
        bundle = intent.extras ?: Bundle()
        userType = bundle.getString("user") ?: ""
        Log.d("PROFILE", "FETCH EXTRA USER TYPE: $userType")

        retrieveUserDetails()
    }


    private fun retrieveUserDetails() {
        Log.d("DASHBOARD", "CHECKING USER RECORD FROM FIREBASE")

        val databaseRef = firebaseDatabase.reference.child("user")
            .child(firebaseAuth.currentUser!!.uid)

        databaseRef.get().addOnCompleteListener { dataSnapshot ->
            if (dataSnapshot.isSuccessful) {
                user = dataSnapshot.result.getValue(UserData::class.java)!!
                if (user!=null) {
                    Log.d("DASHBOARD", " USER FOUND \n ${user.toString()}")
                    //bundle = Bundle()
                    //bundle.putParcelable("user", user)
                    binding.btnAccount.text = "${user.firstname} ${user.lastname}"
                    binding.btnEmail.text = "${user.email}"
                }
            } else {
                Log.d("USER_DETAILS_NOT_FOUND", "USER NOT FOUND")
            }
        }
    }
}