package com.aces.capstone.secureride

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView

class UserDashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)


        val searchView: SearchView = findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle the search query submission
                query?.let {
                    // Perform search operation
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle the search query text change
                newText?.let {
                    // Update search results in real-time
                }
                return false
            }
        })
    }
}
