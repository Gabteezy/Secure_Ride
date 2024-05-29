package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class Commuter(
    val commuterId: String? = null,
    val userType: UserType = UserType.COMMUTER,
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
        parcel.writeString(commuterId)
        parcel.writeString(firstname)
        parcel.writeString(email)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Commuter> {
        override fun createFromParcel(parcel: Parcel): Commuter {
            return Commuter(parcel)
        }

        override fun newArray(size: Int): Array<Commuter?> {
            return arrayOfNulls(size)
        }
    }
}