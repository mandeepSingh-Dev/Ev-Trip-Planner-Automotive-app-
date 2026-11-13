/**
 * Package definition for domain models and business logic.
 */
package com.oem.evplanner.domain

import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryState
import kotlin.math.roundToInt

/**
 * Data class holding route energy calculation results and estimates for reaching a charging station.
 */
data class RouteEnergyEstimate(
    val isReachable: Boolean,
    val estimatedEnergyUsedKwh: Double,
    val arrivalSocPercent: Int,
    val arrivalRangeKm: Int,
    val recommendedChargeMinutesTo80: Int,
    val estimatedCostInr: Int,
    val safetyBufferWarning: String?
)

/**
 * Calculator object providing range and energy estimation algorithms based on live vehicle telemetry.
 */
object EvRangeCalculator {

    private const val DEFAULT_EFFICIENCY_WH_PER_KM = 165.0
    private const val MIN_SAFE_ARRIVAL_SOC = 10

    /**
     * Calculates live energy efficiency in Watt-hours per kilometer (Wh/km) based on telemetry state.
     *
     * @param state Current [EvTelemetryState].
     * @return Calculated efficiency in Wh/km.
     */
    fun getLiveEfficiencyWhPerKm(state: EvTelemetryState): Double {
        val base = if (state.batteryEnergyWh > 1000f && state.rangeRemainingKm > 5) {
            (state.batteryEnergyWh / state.rangeRemainingKm.toDouble()).coerceIn(110.0, 320.0)
        } else {
            DEFAULT_EFFICIENCY_WH_PER_KM
        }
        // Apply 18% cold-weather battery & cabin heating penalty below 10°C
        return if (state.isColdWeatherDeratingActive) base * 1.18 else base
    }

    /**
     * Evaluates route energy requirements and arrival state of charge for traveling to a charging station.
     *
     * @param state Current [EvTelemetryState].
     * @param station Target [ChargingStation].
     * @param effectiveDistanceKm Distance to the station in kilometers.
     * @param isPreconditioningActive Whether battery preconditioning is enabled.
     * @return Calculated [RouteEnergyEstimate].
     */
    fun evaluateRouteToStation(
        state: EvTelemetryState,
        station: ChargingStation,
        effectiveDistanceKm: Double = station.distanceKm,
        isPreconditioningActive: Boolean = false
    ): RouteEnergyEstimate {
        val baseEfficiency = getLiveEfficiencyWhPerKm(state)
        // Add preconditioning energy overhead if active
        val preconditioningOverheadWh = if (isPreconditioningActive) 1500.0 else 0.0
        val tripEnergyWh = (effectiveDistanceKm * baseEfficiency) + preconditioningOverheadWh

        val remainingWhAtArrival = (state.batteryEnergyWh - tripEnergyWh).coerceAtLeast(0.0)
        val arrivalSoc = ((remainingWhAtArrival / state.batteryCapacityWh) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        val arrivalRangeKm = (remainingWhAtArrival / baseEfficiency).roundToInt()
        val isReachable = remainingWhAtArrival > 0.0 && arrivalSoc >= 3

        val targetWh = state.batteryCapacityWh * 0.80
        val energyNeededKwh = ((targetWh - remainingWhAtArrival).coerceAtLeast(0.0)) / 1000.0
        val effectiveChargerKw = (station.maxPowerKw * 0.75).coerceAtLeast(20.0)
        val chargeMinutes = ((energyNeededKwh / effectiveChargerKw) * 60.0).roundToInt()
        val estimatedCostInr = (energyNeededKwh * station.pricePerKwhInr).roundToInt()

        // Determine safety buffer warning message based on arrival SoC
        val warning = when {
            !isReachable -> "Out of range — closer charger required"
            arrivalSoc < MIN_SAFE_ARRIVAL_SOC -> "Low arrival buffer (<10% SoC) — select Eco route"
            else -> null
        }

        return RouteEnergyEstimate(
            isReachable = isReachable,
            estimatedEnergyUsedKwh = tripEnergyWh / 1000.0,
            arrivalSocPercent = arrivalSoc,
            arrivalRangeKm = arrivalRangeKm,
            recommendedChargeMinutesTo80 = chargeMinutes,
            estimatedCostInr = estimatedCostInr,
            safetyBufferWarning = warning
        )
    }
}
