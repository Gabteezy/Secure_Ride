package com.aces.capstone.secureride

import android.os.Parcel
import android.os.Parcelable

data class UserData(
    var uid: String? = "",
    var email: String? = null,
    val firstname: String? = null,
    var authEmail: String? = null,
    val lastname: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val userType: String? = null,
    var isVerified: Boolean = false,
    val address: String? = null,
    var profileImage: String? = null,
    val plateNumberImage: String? = null,
    val licenceImage: String? = null,
    val rating: String? = null,
    var totalFare: Int = 0,
    val tricycleImage: String? = null,
    var isOnline: Boolean = false, // Change to var
    var note: String? = null,
    var timestamp: Long = System.currentTimeMillis(),
    val isSuspended: Boolean = false, // Change to var
    var isOnRide: Boolean = false, // Change to var
    var isLoggedIn: Boolean = false, // Change to var
    var suspensionReason: String? = null,
    var suspensionEndTime: Long = 0L,
    var suspensionEndDate: Long? = null // Change to var
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readValue(Long::class.java.classLoader) as? Long
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(email)
        parcel.writeString(firstname)
        parcel.writeString(authEmail)
        parcel.writeString(lastname)
        parcel.writeString(phone)
        parcel.writeString(password)
        parcel.writeString(userType)
        parcel.writeByte(if (isVerified) 1 else 0)
        parcel.writeString(address)
        parcel.writeString(profileImage)
        parcel.writeString(plateNumberImage)
        parcel.writeString(licenceImage)
        parcel.writeString(rating)
        parcel.writeInt(totalFare)
        parcel.writeString(tricycleImage)
        parcel.writeByte(if (isOnline) 1 else 0)
        parcel.writeString(note)
        parcel.writeLong(timestamp)
        parcel.writeByte(if (isSuspended) 1 else 0)
        parcel.writeByte(if (isOnRide) 1 else 0)
        parcel.writeByte(if (isLoggedIn) 1 else 0)
        parcel.writeString(suspensionReason)
        parcel.writeLong(suspensionEndTime)
        parcel.writeValue(suspensionEndDate)
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