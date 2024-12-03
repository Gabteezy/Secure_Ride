package com.aces.capstone.secureride.model

data class RideRequest(
    val userId: String? = "",
    var id: String? = "",
    val driverId: String = "",
    val driverName: String = "",
    val info: String? = "",
    val destination: String? = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: String? = "pending",
    val userType: String = "COMMUTER",
    val totalFare: Int = 0,
    val pickupLocation: String = "",
    val rideInfo: String? = "",
    val dropoffLocation: String = "",
    val dropOffLatitude: Double = 0.0,
    val dropOffLongitude: Double = 0.0,
)