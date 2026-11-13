package com.oem.evplanner.model

enum class ConnectorStandard(val displayName: String) {
    CCS2("CCS2"),
    CHADEMO("CHAdeMO"),
    TYPE_2("Type 2 AC")
}

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