/**
 * Package definition for cluster and HUD (Head-Up Display) telemetry broadcasting components.
 */
package com.oem.evplanner.cluster

import android.content.Context
import android.content.Intent

/**
 * Broadcaster utility object responsible for sending navigation telemetry and charging
 * updates to the vehicle's digital instrument cluster or HUD system via Android broadcast intents.
 */
object ClusterHudBroadcaster {

    /** Broadcast intent action string for EV navigation and charging updates. */
    const val ACTION_EV_NAVIGATION_UPDATE = "com.oem.cluster.ACTION_EV_NAVIGATION_UPDATE"
    /** Intent extra key for the target charging station name. */
    const val EXTRA_STATION_NAME = "extra_station_name"
    /** Intent extra key for distance remaining in kilometers. */
    const val EXTRA_DISTANCE_KM = "extra_distance_km"
    /** Intent extra key for estimated arrival state of charge percentage. */
    const val EXTRA_ARRIVAL_SOC = "extra_arrival_soc"
    /** Intent extra key indicating whether route guidance is active. */
    const val EXTRA_IS_ACTIVE = "extra_is_active"

    /**
     * Publishes real-time navigation and charging telemetry to the cluster receiver.
     *
     * @param context Application or car context used to send broadcasts.
     * @param stationName Name of the destination charging station.
     * @param distanceKm Remaining distance to the station in kilometers.
     * @param arrivalSocPercent Estimated State of Charge upon arrival at the station.
     * @param isActive Flag indicating if EV guidance is currently active.
     */
    fun publishNavigationTelemetry(
        context: Context,
        stationName: String,
        distanceKm: Double,
        arrivalSocPercent: Int,
        isActive: Boolean
    ) {
        // Construct broadcast intent targeting the OEM cluster package receiver
        val intent = Intent(ACTION_EV_NAVIGATION_UPDATE).apply {
            setPackage("com.oem.cluster")
            putExtra(EXTRA_STATION_NAME, stationName)
            putExtra(EXTRA_DISTANCE_KM, distanceKm)
            putExtra(EXTRA_ARRIVAL_SOC, arrivalSocPercent)
            putExtra(EXTRA_IS_ACTIVE, isActive)
        }
        // Send the broadcast intent system-wide to registered cluster receivers
        context.sendBroadcast(intent)
    }
}
