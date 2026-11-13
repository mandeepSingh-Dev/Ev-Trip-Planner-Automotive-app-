package com.oem.evplanner.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat

object EvPermissionHelper {

    val REQUIRED_RUNTIME_PERMISSIONS = listOf(
        "android.car.permission.CAR_ENERGY",
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasAllRequiredPermissions(carContext: CarContext): Boolean {
        return getMissingPermissions(carContext).isEmpty()
    }

    fun getMissingPermissions(carContext: CarContext): List<String> {
        return REQUIRED_RUNTIME_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(carContext, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestMissingPermissions(
        carContext: CarContext,
        onResult: (allGranted: Boolean, grantedList: List<String>, deniedList: List<String>) -> Unit
    ) {
        val missing = getMissingPermissions(carContext)
        if (missing.isEmpty()) {
            onResult(true, REQUIRED_RUNTIME_PERMISSIONS, emptyList())
            return
        }

        carContext.requestPermissions(missing) { granted, rejected ->
            val allGranted = getMissingPermissions(carContext).isEmpty()
            onResult(allGranted, granted, rejected)
        }
    }
}