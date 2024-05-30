package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable


data class Booking(
    val commuterId: String? = null,
    val driverId: String? = null,
    val details: Map<String, String>? = null,
    val status: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        TODO("details"),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(commuterId)
        parcel.writeString(driverId)
        parcel.writeString(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Booking> {
        override fun createFromParcel(parcel: Parcel): Booking {
            return Booking(parcel)
        }

        override fun newArray(size: Int): Array<Booking?> {
            return arrayOfNulls(size)
        }
    }
}
