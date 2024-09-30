package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class BookingRequest(
    val bookingId: String? = null,
    val commuterId: String? = null,
    val driverId: String? = null,
    val notificationId: String? = null,
    val status: String? = "requested",
    val pickupLocation: String? = null,
    val dropoffLocation: String? = null,
    val timeSlot: String? = null,
    val date: String? = null,
    val firstname: String? = null,
    val uid: String? = null,
    val type: String? = null
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
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(bookingId)
        parcel.writeString(commuterId)
        parcel.writeString(driverId)
        parcel.writeString(notificationId)
        parcel.writeString(status)
        parcel.writeString(pickupLocation)
        parcel.writeString(dropoffLocation)
        parcel.writeString(timeSlot)
        parcel.writeString(date)
        parcel.writeString(firstname)
        parcel.writeString(uid)
        parcel.writeString(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BookingRequest> {
        override fun createFromParcel(parcel: Parcel): BookingRequest {
            return BookingRequest(parcel)
        }

        override fun newArray(size: Int): Array<BookingRequest?> {
            return arrayOfNulls(size)
        }
    }
}