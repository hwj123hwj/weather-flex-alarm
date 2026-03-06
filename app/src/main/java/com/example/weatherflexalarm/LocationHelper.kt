package com.example.weatherflexalarm

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun lastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val providers = locationManager.getProviders(true)

        var bestLocation: Location? = null
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || location.time > bestLocation.time) {
                bestLocation = location
            }
        }
        return bestLocation
    }
}
