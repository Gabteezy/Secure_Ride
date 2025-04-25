package com.aces.capstone.secureride.model

data class RideRequest(
    val userId: String? = "",
    var id: String? = "",
    var driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val info: String? = "",
    val phone: String? = null,
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
    var timestamp: Long? = null,
    val commuterCount: Int = 1,
    var timeRemaining: Long = 30000
)