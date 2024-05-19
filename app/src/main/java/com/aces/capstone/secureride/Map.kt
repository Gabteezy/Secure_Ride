package com.aces.capstone.secureride

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class Map() : AppCompatActivity(), OnMapReadyCallback, Parcelable {
    private var mGoogleMap:GoogleMap? = null

    constructor(parcel: Parcel) : this() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mGoogleMap = googleMap
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Map> {
        override fun createFromParcel(parcel: Parcel): Map {
            return Map(parcel)
        }

        override fun newArray(size: Int): Array<Map?> {
            return arrayOfNulls(size)
        }
    }
}