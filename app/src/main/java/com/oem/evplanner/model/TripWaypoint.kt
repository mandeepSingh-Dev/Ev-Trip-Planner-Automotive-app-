package com.oem.evplanner.model

data class TripWaypoint(
    val title: String,
    val subtitle: String,
    val distanceFromStartKm: Double,
    val arrivalSocPercent: Int,
    val departureSocPercent: Int,
    val chargeMinutes: Int = 0,
    val station: ChargingStation? = null
)

data class ChargingHistoryRecord(
    val id: String,
    val dateLabel: String,
    val stationName: String,
    val energyAddedKwh: Double,
    val peakPowerKw: Int,
    val totalCostInr: Int
)

enum class ChargerFilterMode(val label: String) {
    ALL("All Chargers"),
    IN_RANGE("Within Range"),
    ULTRA_FAST("150kW+ Ultra-Fast"),
    AVAILABLE_NOW("Available Stalls")
}