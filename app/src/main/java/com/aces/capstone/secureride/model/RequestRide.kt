package com.aces.capstone.secureride.model

data class RideRequest(
    val userId: String? = "",
    var id: String? = "",
    val driverId: String? = null,
    val driverName: String? = null, // Name of the driver
    val driverPhone: String? = null, // Contact number of the driver
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
    val confirmationStatus: Boolean = false,
    val timestamp: Long? = null
)