package com.aces.capstone.secureride.model

import android.os.Parcel
import android.os.Parcelable

data class DriverData(
    val id: String? = null,
    val name: String? = null,
    val online: Boolean = false,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    ) {}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeByte(if (online) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DriverData> {
        override fun createFromParcel(parcel: Parcel): DriverData = DriverData(parcel)
        override fun newArray(size: Int): Array<DriverData?> = arrayOfNulls(size)
    }
}
