/**
 * Package definition for permission handling utilities in the automotive app.
 */
package com.oem.evplanner.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.core.content.ContextCompat

/**
 * Helper object managing required runtime permissions for automotive car energy,
 * powertrain telemetry, and precise location access.
 */
object EvPermissionHelper {

    /** List of mandatory runtime permissions required by the EV Trip Planner app. */
    val REQUIRED_RUNTIME_PERMISSIONS = listOf(
        "android.car.permission.CAR_ENERGY",
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Checks whether all required runtime permissions have been granted.
     *
     * @param carContext The car context.
     * @return True if all permissions are granted, false otherwise.
     */
    fun hasAllRequiredPermissions(carContext: CarContext): Boolean {
        return getMissingPermissions(carContext).isEmpty()
    }

    /**
     * Returns a list of required permissions that have not yet been granted.
     *
     * @param carContext The car context.
     * @return List of missing permission strings.
     */
    fun getMissingPermissions(carContext: CarContext): List<String> {
        return REQUIRED_RUNTIME_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(carContext, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Requests any missing runtime permissions from the driver.
     *
     * @param carContext The car context.
     * @param onResult Callback invoked with grant status, granted list, and denied list.
     */
    fun requestMissingPermissions(
        carContext: CarContext,
        onResult: (allGranted: Boolean, grantedList: List<String>, deniedList: List<String>) -> Unit
    ) {
        val missing = getMissingPermissions(carContext)
        if (missing.isEmpty()) {
            onResult(true, REQUIRED_RUNTIME_PERMISSIONS, emptyList())
            return
        }

        // Request missing permissions via CarContext
        carContext.requestPermissions(missing) { granted, rejected ->
            val allGranted = getMissingPermissions(carContext).isEmpty()
            onResult(allGranted, granted, rejected)
        }
    }
}
