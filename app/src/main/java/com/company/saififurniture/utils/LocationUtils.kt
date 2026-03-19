package com.saififurnitures.app.utils

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices

object LocationUtils {
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onLocationReceived: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) onLocationReceived(loc.latitude, loc.longitude)
                else onError("Location unavailable — ensure GPS is on")
            }
            .addOnFailureListener { onError(it.message ?: "Location error") }
    }
}