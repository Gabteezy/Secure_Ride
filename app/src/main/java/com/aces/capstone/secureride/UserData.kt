package com.aces.capstone.secureride

import android.os.Parcel
import android.os.Parcelable


data class UserData(
    val uid: String? = null,
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val type: String? = null,
    val verified: String? = null,
    ) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(email)
        parcel.writeString(firstname)
        parcel.writeString(lastname)
        parcel.writeString(phone)
        parcel.writeString(password)
        parcel.writeString(type)
        parcel.writeString(verified)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserData> {
        override fun createFromParcel(parcel: Parcel): UserData {
            return UserData(parcel)
        }

        override fun newArray(size: Int): Array<UserData?> {
            return arrayOfNulls(size)
        }
    }
}