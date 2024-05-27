package com.aces.capstone.secureride

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.util.Log
import com.aces.capstone.secureride.databinding.ActivityUserDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserDashboard : AppCompatActivity() {

    private lateinit var userType: String
    private lateinit var user: UserData
    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var bundle: Bundle
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize bundle and retrieve user type if it exists
        bundle = intent.extras!!
        userType = bundle?.getString("user") ?: ""
        Log.d("DASHBOARD", "FETCH EXTRA USER TYPE")

        retrieveUserDetails()

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav

        setupBottomNavigation(bottomNavigationView)

        binding.book.setOnClickListener {
            startActivity(Intent(this@UserDashboard, Search::class.java))
        }
        binding.book1.setOnClickListener {
            startActivity(Intent(this@UserDashboard, Search::class.java))
        }
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
                    binding.name.text = "${user.firstname} ${user.lastname}"

                }
            } else {
                Log.d("USER_DETAILS_NOT_FOUND", "USER NOT FOUND")
            }
        }
    }

    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    // Home is already selected, no action needed
                    true
                }
                R.id.navCustomerFindRide -> {
                    // Navigate to SearchActivity
                    startActivity(Intent(this@UserDashboard, Search::class.java))
                    true
                }
                R.id.navCustomerMyRides -> {
                    // Navigate to Map activity
                    startActivity(Intent(this@UserDashboard, BookingDetails::class.java))
                    true
                }
                R.id.navCustomerProfile -> {
                    startActivity(Intent(this@UserDashboard, Profile::class.java))
                    true
                }

                else -> false
            }
        }
    }
}
