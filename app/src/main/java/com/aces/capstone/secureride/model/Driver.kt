package com.aces.capstone.secureride.model

data class Driver(
    var uid: String? = "",
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val profileImage: String? = null,
    var isAvailable: Boolean = true, // Add this property to indicate availability
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val completedRides: Int = 0 // Example property for completed rides
)