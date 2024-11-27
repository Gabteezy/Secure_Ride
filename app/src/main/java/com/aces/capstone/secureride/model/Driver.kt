package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class Driver(
    val driverId: String = "",
    val name: String = "",
    val plateNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val currentAddress: String = "",
    val rating: String = "",
    val emergencyNumber: String = "",
    val profilePicture: String = ""
)
