/**
 * Package definition for application data models and enums.
 */
package com.oem.evplanner.model

/**
 * Data class representing a waypoint along a multi-stop EV route itinerary.
 */
data class TripWaypoint(
    val title: String,
    val subtitle: String,
    val distanceFromStartKm: Double,
    val arrivalSocPercent: Int,
    val departureSocPercent: Int,
    val chargeMinutes: Int = 0,
    val station: ChargingStation? = null
)

/**
 * Data class representing a historical charging session record for analytics.
 */
data class ChargingHistoryRecord(
    val id: String,
    val dateLabel: String,
    val stationName: String,
    val energyAddedKwh: Double,
    val peakPowerKw: Int,
    val totalCostInr: Int
)

/**
 * Enum representing filter modes for searching and listing charging stations.
 */
enum class ChargerFilterMode(val label: String) {
    /** Show all charging stations. */
    ALL("All Chargers"),
    /** Show stations within current driving range. */
    IN_RANGE("Within Range"),
    /** Show ultra-fast chargers (150 kW and above). */
    ULTRA_FAST("150kW+ Ultra-Fast"),
    /** Show stations with currently available charging stalls. */
    AVAILABLE_NOW("Available Stalls")
}
