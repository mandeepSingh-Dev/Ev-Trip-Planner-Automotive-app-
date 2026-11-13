/**
 * Package definition for domain models and business logic.
 */
package com.oem.evplanner.domain

import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.model.TripWaypoint
import com.oem.evplanner.telemetry.EvTelemetryState
import kotlin.math.roundToInt

/**
 * Data class representing a multi-stop long-distance EV trip plan including waypoints,
 * charging stops, and estimated travel/charging durations.
 */
data class LongDistanceTripPlan(
    val destinationName: String,
    val totalDistanceKm: Double,
    val totalDriveMinutes: Int,
    val totalChargeMinutes: Int,
    val finalArrivalSoc: Int,
    val waypoints: List<TripWaypoint>
)

/**
 * Multi-stop trip planner object responsible for calculating optimal charging stops along
 * a long-distance corridor based on live battery telemetry and station availability.
 */
object MultiStopTripPlanner {

    /**
     * Builds a comprehensive multi-stop trip plan to a given destination.
     *
     * @param destinationName Name of the final destination.
     * @param totalDistanceKm Total distance of the trip in kilometers.
     * @param state Current EV telemetry state.
     * @param availableStations List of available charging stations along or near the route.
     * @return Calculated [LongDistanceTripPlan] with optimal charging waypoints.
     */
    fun buildTripPlan(
        destinationName: String,
        totalDistanceKm: Double,
        state: EvTelemetryState,
        availableStations: List<ChargingStation>
    ): LongDistanceTripPlan {
        // Retrieve live energy efficiency (Wh/km) based on telemetry state
        val efficiencyWhPerKm = EvRangeCalculator.getLiveEfficiencyWhPerKm(state)
        val waypoints = mutableListOf<TripWaypoint>()

        var currentWh = state.batteryEnergyWh.toDouble()
        var coveredKm = 0.0
        var totalChargeMins = 0

        // Sort stations along corridor distance
        val corridorStations = availableStations
            .filter { it.distanceKm < totalDistanceKm }
            .sortedBy { it.distanceKm }

        // Iterate through corridor stations to determine necessary charging stops
        for (station in corridorStations) {
            val legKm = (station.distanceKm - coveredKm).coerceAtLeast(0.0)
            val legEnergyWh = legKm * efficiencyWhPerKm
            val remainingKmAfterStation = totalDistanceKm - station.distanceKm
            val energyNeededToFinishWh = remainingKmAfterStation * efficiencyWhPerKm

            // Stop to charge if we cannot finish the trip with at least 15% buffer
            val minFinishBufferWh = state.batteryCapacityWh * 0.15
            if (currentWh - legEnergyWh < energyNeededToFinishWh + minFinishBufferWh) {
                val arrivalWh = (currentWh - legEnergyWh).coerceAtLeast(state.batteryCapacityWh * 0.08)
                val arrivalSoc = ((arrivalWh / state.batteryCapacityWh) * 100.0).roundToInt()

                // Charge up to 80% SoC
                val targetWh = state.batteryCapacityWh * 0.80
                val addedKwh = ((targetWh - arrivalWh).coerceAtLeast(0.0)) / 1000.0
                val chargeMins = ((addedKwh / (station.maxPowerKw * 0.75)) * 60.0).roundToInt().coerceAtLeast(10)

                // Add charging waypoint to the plan
                waypoints.add(
                    TripWaypoint(
                        title = "Stop: ${station.name}",
                        subtitle = "Arrive $arrivalSoc% → Charge ${chargeMins}m to 80% (${station.maxPowerKw}kW)",
                        distanceFromStartKm = station.distanceKm,
                        arrivalSocPercent = arrivalSoc,
                        departureSocPercent = 80,
                        chargeMinutes = chargeMins,
                        station = station
                    )
                )

                currentWh = targetWh
                coveredKm = station.distanceKm
                totalChargeMins += chargeMins
            }
        }

        // Final leg to destination calculation
        val finalLegKm = (totalDistanceKm - coveredKm).coerceAtLeast(0.0)
        val finalArrivalWh = (currentWh - (finalLegKm * efficiencyWhPerKm)).coerceAtLeast(0.0)
        val finalSoc = ((finalArrivalWh / state.batteryCapacityWh) * 100.0).roundToInt().coerceIn(0, 100)

        // Add final destination waypoint
        waypoints.add(
            TripWaypoint(
                title = "Destination: $destinationName",
                subtitle = "Total ${totalDistanceKm.toInt()} km • Arrive with ~$finalSoc% SoC",
                distanceFromStartKm = totalDistanceKm,
                arrivalSocPercent = finalSoc,
                departureSocPercent = finalSoc
            )
        )

        val driveMins = ((totalDistanceKm / 65.0) * 60.0).roundToInt()
        return LongDistanceTripPlan(
            destinationName = destinationName,
            totalDistanceKm = totalDistanceKm,
            totalDriveMinutes = driveMins,
            totalChargeMinutes = totalChargeMins,
            finalArrivalSoc = finalSoc,
            waypoints = waypoints
        )
    }
}
