package com.aces.capstone.secureride.model

data class RideHistory(
    val rideId: String? = null,
    val commuterName: String? = null,
    val pickupLocation: String? = null,
    val dropoffLocation: String? = null,
    val status: String? = null,
    val totalFare: Int? = null,
    val time: String? = null,
    val date: String? = null,
    val rideInfo: String? = null,
    val timestamp: Long? = null
)
