/**
 * Package definition for data repositories and remote/local data sources.
 */
package com.oem.evplanner.data

import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.model.ConnectorStandard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository class managing charging station data sources, including local cached corridor
 * stations and live network synchronization via Open Charge Map API.
 */
class ChargingStationRepository {

    /** Client for fetching live Point of Interest data from Open Charge Map network. */
    private val ocmClient = OpenChargeMapClient(apiKey = "") // Plug in your OCM key when ready

    /**
     * Refreshes charging stations from the remote Open Charge Map network based on geographic coordinates.
     *
     * @param lat Latitude coordinate for search center.
     * @param lon Longitude coordinate for search center.
     */
    suspend fun refreshFromNetwork(lat: Double = 28.5355, lon: Double = 77.2100) {
        val liveStations = ocmClient.fetchNearbyStations(lat, lon)
        if (liveStations.isNotEmpty()) {
            _stations.value = liveStations
        }
    }

    /** Mutable StateFlow holding the current list of charging stations. */
    private val _stations = MutableStateFlow(
        listOf(
            ChargingStation(
                id = "st_cyberhub",
                name = "Tata Power EZ Charge - Cyber Hub",
                networkOperator = "Tata Power",
                address = "DLF Cyber City, Gurugram",
                latitude = 28.4950,
                longitude = 77.0895,
                distanceKm = 14.2,
                maxPowerKw = 120,
                availableStalls = 3,
                totalStalls = 4,
                connector = ConnectorStandard.CCS2,
                pricePerKwhInr = 18.50,
                estimatedMinutes = 24,
                amenities = listOf("Cafe", "Restroom", "Wi-Fi")
            ),
            ChargingStation(
                id = "st_aerocity",
                name = "Statiq Ultra-Fast Hub - Aerocity",
                networkOperator = "Statiq",
                address = "Worldmark 1, New Delhi",
                latitude = 28.5503,
                longitude = 77.1210,
                distanceKm = 22.8,
                maxPowerKw = 180,
                availableStalls = 2,
                totalStalls = 6,
                connector = ConnectorStandard.CCS2,
                pricePerKwhInr = 21.00,
                estimatedMinutes = 35,
                amenities = listOf("Food Court", "Restroom")
            ),
            ChargingStation(
                id = "st_manesar",
                name = "Zeon Highway Supercharger - NH48",
                networkOperator = "Zeon Charging",
                address = "Manesar Toll Plaza, NH48",
                latitude = 28.3587,
                longitude = 76.9366,
                distanceKm = 46.5,
                maxPowerKw = 240,
                availableStalls = 4,
                totalStalls = 4,
                connector = ConnectorStandard.CCS2,
                pricePerKwhInr = 22.50,
                estimatedMinutes = 52,
                amenities = listOf("24x7 Lounge", "Restroom")
            ),
            ChargingStation(
                id = "st_cp",
                name = "Jio-bp Pulse Hub - Connaught Place",
                networkOperator = "Jio-bp",
                address = "Barakhamba Rd, New Delhi",
                latitude = 28.6315,
                longitude = 77.2215,
                distanceKm = 31.0,
                maxPowerKw = 60,
                availableStalls = 1,
                totalStalls = 2,
                connector = ConnectorStandard.CCS2,
                pricePerKwhInr = 17.00,
                estimatedMinutes = 44,
                amenities = listOf("Shopping")
            ),
            ChargingStation(
                id = "st_neemrana",
                name = "ChargeZone Mega Hub - Neemrana",
                networkOperator = "ChargeZone",
                address = "NH48 Highway, Rajasthan",
                latitude = 27.9889,
                longitude = 76.3835,
                distanceKm = 118.0,
                maxPowerKw = 300,
                availableStalls = 5,
                totalStalls = 6,
                connector = ConnectorStandard.CCS2,
                pricePerKwhInr = 23.00,
                estimatedMinutes = 115,
                amenities = listOf("Highway Plaza", "Restroom", "Dining")
            )
        )
    )
    /** Read-only StateFlow of available charging stations for UI observers. */
    val stations: StateFlow<List<ChargingStation>> = _stations.asStateFlow()

    /**
     * Finds and returns a specific charging station by its unique identifier.
     *
     * @param id Unique station identifier string.
     * @return ChargingStation object if found, or null otherwise.
     */
    fun getStationById(id: String): ChargingStation? = _stations.value.find { it.id == id }
}
