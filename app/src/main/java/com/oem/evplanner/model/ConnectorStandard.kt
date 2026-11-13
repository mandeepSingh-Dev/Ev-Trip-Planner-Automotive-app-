/**
 * Package definition for application data models and enums.
 */
package com.oem.evplanner.model

/**
 * Enum representing EV charging connector standards with their display names.
 */
enum class ConnectorStandard(val displayName: String) {
    /** Combined Charging System 2 (DC Fast Charging). */
    CCS2("CCS2"),
    /** CHAdeMO DC fast charging standard. */
    CHADEMO("CHAdeMO"),
    /** Type 2 AC charging standard. */
    TYPE_2("Type 2 AC")
}

/**
 * Data class representing an electric vehicle charging station with location, power,
 * availability, pricing, and amenities.
 */
data class ChargingStation(
    val id: String,
    val name: String,
    val networkOperator: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val maxPowerKw: Int,
    val availableStalls: Int,
    val totalStalls: Int,
    val connector: ConnectorStandard,
    val pricePerKwhInr: Double,
    val estimatedMinutes: Int,
    val amenities: List<String> = emptyList()
)
