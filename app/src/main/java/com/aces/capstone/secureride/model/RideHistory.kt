package com.aces.capstone.secureride.model

data class RideHistory(
    var rideId: String? = null,
    var commuterName: String? = null,
    var pickuplocation: String? = null,
    var dropofflocation: String? = null,
    var status: String? = null,
    var timestamp: Long? = null,
    val totalFare: Int = 0,
    val rideInfo: String? = ""
)