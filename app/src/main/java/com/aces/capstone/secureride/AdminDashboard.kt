package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.databinding.ActivityAdminDashboardBinding
import com.aces.capstone.secureride.ui.ListOfCommuterActivity
import com.aces.capstone.secureride.ui.ListOfDriversActivity
import com.aces.capstone.secureride.ui.ListOfPendingDrivers
import com.aces.capstone.secureride.ui.ListOfRideRequest
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isUserAuthenticated()) {
            redirectToLogin()
            return
        }

        // Initialize ViewBinding
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        // Set up Toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set up NavigationView for menu actions
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    // Already on Dashboard, refresh data
                    refreshData()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_about_us -> {
                    startActivity(Intent(this@AdminDashboard, AboutUsActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }

        // Load Data on First Entry
        refreshData()

        // Set onClickListeners for the CardViews
        binding.cardViewDrivers.setOnClickListener {
            startActivity(Intent(this@AdminDashboard, ListOfDriversActivity::class.java))
        }

        binding.cardViewRides.setOnClickListener {
            startActivity(Intent(this@AdminDashboard, ListOfRideRequest::class.java))
        }

        binding.cardViewCommuters.setOnClickListener {
            startActivity(Intent(this@AdminDashboard, ListOfCommuterActivity::class.java))
        }

        binding.cardViewPendingDrivers.setOnClickListener {
            startActivity(Intent(this@AdminDashboard, ListOfPendingDrivers::class.java))
        }

        // Add click listener for Gas Price Card to update price
        binding.cardViewGasPrice.setOnClickListener {
            showGasPriceDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Find buttons in the dialog layout
        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            logout()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showGasPriceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_gas_price, null)
        val gasPriceInput = dialogView.findViewById<EditText>(R.id.etGasPrice)
        val saveButton = dialogView.findViewById<Button>(R.id.btnSaveGasPrice)
        val cancelButton = dialogView.findViewById<Button>(R.id.btnCancelGasPrice)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val newGasPrice = gasPriceInput.text.toString().toDoubleOrNull()
            if (newGasPrice != null && newGasPrice > 0) {
                updateGasolinePrice(newGasPrice)
                dialog.dismiss()
            } else {
                gasPriceInput.error = "Please enter a valid price"
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateGasolinePrice(newPrice: Double) {
        firebaseDatabase.child("gasoline_price").child("updated_gasoline_price").setValue(newPrice)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Gasoline price updated to ₱$newPrice", android.widget.Toast.LENGTH_SHORT).show()
                // Update the UI immediately after saving
                binding.gasPriceText.text = String.format("₱%.2f", newPrice)
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Failed to update gasoline price", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun refreshData() {
        swipeRefreshLayout.isRefreshing = true

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userRef = firebaseDatabase.child("user").child(user.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firstname = snapshot.child("firstname").value?.toString() ?: "Admin"
                    binding.toolbar.title = "Hi $firstname"

                    getDriverCount()
                    getCommuterCount()
                    getRideRequestCount()
                    getPendingDriversCount()
                    getGasolinePrice()

                    swipeRefreshLayout.isRefreshing = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminDashboard", "Failed to retrieve user data: ${error.message}")
                    swipeRefreshLayout.isRefreshing = false
                }
            })
        }
    }

    private fun getDriverCount() {
        firebaseDatabase.child("user").orderByChild("userType").equalTo("Driver")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    binding.driverCountText.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminDashboard", "Failed to retrieve driver count: ${error.message}")
                }
            })
    }

    private fun getCommuterCount() {
        firebaseDatabase.child("user").orderByChild("userType").equalTo("Commuter")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    binding.commuterCountText.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminDashboard", "Failed to retrieve commuter count: ${error.message}")
                }
            })
    }

    private fun getRideRequestCount() {
        firebaseDatabase.child("ride_requests").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.ridesCountText.text = count.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to retrieve ride requests count: ${error.message}")
            }
        })
    }

    private fun getPendingDriversCount() {
        firebaseDatabase.child("pending_drivers").orderByChild("userType").equalTo("Driver")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    binding.pendingDriversCountText.text = count.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminDashboard", "Failed to retrieve pending drivers count: ${error.message}")
                }
            })
    }

    private fun getGasolinePrice() {
        firebaseDatabase.child("gasoline_price").child("updated_gasoline_price")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val price = snapshot.getValue(Double::class.java)
                    if (price != null) {
                        binding.gasPriceText.text = String.format("₱%.2f", price)
                    } else {
                        binding.gasPriceText.text = "₱0.00"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminDashboard", "Failed to retrieve gasoline price: ${error.message}")
                    binding.gasPriceText.text = "₱0.00"
                }
            })
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}