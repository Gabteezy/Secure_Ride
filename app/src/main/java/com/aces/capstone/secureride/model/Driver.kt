package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class Driver(
    val driverId: String? = null,
    val userType: UserType = UserType.DRIVER,
    val firstname: String? = null,
    val email: String? = null,
    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        TODO("userType"),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(driverId)
        parcel.writeString(firstname)
        parcel.writeString(email)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Driver> {
        override fun createFromParcel(parcel: Parcel): Driver {
            return Driver(parcel)
        }

        override fun newArray(size: Int): Array<Driver?> {
            return arrayOfNulls(size)
        }
    }
}