package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class Booking(
    val commuterId: String? = null,
    val driverId: String? = null,
    val details: Map<String, String>? = null,
    val status: String? = null
)