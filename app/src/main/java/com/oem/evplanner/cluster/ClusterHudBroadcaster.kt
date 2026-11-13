package com.oem.evplanner.cluster

import android.content.Context
import android.content.Intent

object ClusterHudBroadcaster {

    const val ACTION_EV_NAVIGATION_UPDATE = "com.oem.cluster.ACTION_EV_NAVIGATION_UPDATE"
    const val EXTRA_STATION_NAME = "extra_station_name"
    const val EXTRA_DISTANCE_KM = "extra_distance_km"
    const val EXTRA_ARRIVAL_SOC = "extra_arrival_soc"
    const val EXTRA_IS_ACTIVE = "extra_is_active"

    fun publishNavigationTelemetry(
        context: Context,
        stationName: String,
        distanceKm: Double,
        arrivalSocPercent: Int,
        isActive: Boolean
    ) {
        val intent = Intent(ACTION_EV_NAVIGATION_UPDATE).apply {
            setPackage("com.oem.cluster")
            putExtra(EXTRA_STATION_NAME, stationName)
            putExtra(EXTRA_DISTANCE_KM, distanceKm)
            putExtra(EXTRA_ARRIVAL_SOC, arrivalSocPercent)
            putExtra(EXTRA_IS_ACTIVE, isActive)
        }
        context.sendBroadcast(intent)
    }
}