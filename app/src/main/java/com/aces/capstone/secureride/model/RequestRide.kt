package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class RideRequest(
    val userId: String? = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String? = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RideRequest> {
        override fun createFromParcel(parcel: Parcel): RideRequest {
            return RideRequest(parcel)
        }

        override fun newArray(size: Int): Array<RideRequest?> {
            return arrayOfNulls(size)
        }
    }
}
