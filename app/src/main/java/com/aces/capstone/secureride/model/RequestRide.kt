package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class RideRequest(
    val userId: String? = "",
    var id: String? = "",
    val info: String? = "",
    val destination: String? = "",
    val firstName: String? = "",
    val lastName: String? = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: String? = "pending",
    var driverId: String? = "",
    var driverName: String? = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeString(id)
        parcel.writeString(info)
        parcel.writeString(destination)
        parcel.writeString(firstName)
        parcel.writeString(lastName)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(status)
        parcel.writeString(driverId)
        parcel.writeString(driverName)
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