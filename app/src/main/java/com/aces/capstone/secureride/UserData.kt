package com.aces.capstone.secureride


data class UserData(
    val uid: String? = null,
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val type: String? = null,
    val isVerified: String = "",
    val address: String? = null,
    var profileImage: String? = null,
    val platenumber: String? = null,
    val licenceImage: String? = null,
    val rating: String? = null,
    val totalFare: Int = 0
)