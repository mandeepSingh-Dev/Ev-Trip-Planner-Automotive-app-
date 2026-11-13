package com.oem.evplanner.domain

import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryState
import kotlin.math.roundToInt

data class RouteEnergyEstimate(
    val isReachable: Boolean,
    val estimatedEnergyUsedKwh: Double,
    val arrivalSocPercent: Int,
    val arrivalRangeKm: Int,
    val recommendedChargeMinutesTo80: Int,
    val estimatedCostInr: Int,
    val safetyBufferWarning: String?
)

object EvRangeCalculator {

    private const val DEFAULT_EFFICIENCY_WH_PER_KM = 165.0
    private const val MIN_SAFE_ARRIVAL_SOC = 10

    fun getLiveEfficiencyWhPerKm(state: EvTelemetryState): Double {
        return if (state.batteryEnergyWh > 1000f && state.rangeRemainingKm > 5) {
            (state.batteryEnergyWh / state.rangeRemainingKm.toDouble()).coerceIn(110.0, 320.0)
        } else {
            DEFAULT_EFFICIENCY_WH_PER_KM
        }
    }

    fun evaluateRouteToStation(
        state: EvTelemetryState,
        station: ChargingStation,
        effectiveDistanceKm: Double = station.distanceKm,
        isPreconditioningActive: Boolean = false
    ): RouteEnergyEstimate {
        val baseEfficiency = getLiveEfficiencyWhPerKm(state)
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