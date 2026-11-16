/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.domain.EvRangeCalculator
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryManager

/**
 * Screen class displaying detailed information, stall availability, amenities,
 * and reservation options for a selected charging station using [PaneTemplate].
 */
class StationDetailScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val mapRenderer: EvMapSurfaceRenderer,
    private val station: ChargingStation
) : Screen(carContext) {

    private var isStallReserved = false

    /**
     * Builds and returns the [PaneTemplate] showing station specifications, arrival estimates, and action buttons.
     */
    override fun onGetTemplate(): Template {
        val state = telemetryManager.telemetryState.value
        val estimate = EvRangeCalculator.evaluateRouteToStation(state, station)

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("${station.networkOperator} • ${station.maxPowerKw} kW ${station.connector.displayName}")
                    .addText("${station.availableStalls} of ${station.totalStalls} stalls open • ₹${station.pricePerKwhInr}/kWh")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Projected Arrival & Charge")
                    .addText("Arrive at ${estimate.arrivalSocPercent}% SoC • ~${estimate.recommendedChargeMinutesTo80} min to 80% (~₹${estimate.estimatedCostInr})")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Driver Amenities")
                    .addText(station.amenities.joinToString(" • ").ifEmpty { "24x7 Automated Access" })
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Route Here")
                    .setBackgroundColor(CarColor.PRIMARY)
                    .setOnClickListener {
                        screenManager.push(
                            ChargerRoutePreviewScreen(carContext, telemetryManager, mapRenderer, station)
                        )
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (isStallReserved) "Stall #2 Reserved (15m)" else "Reserve Stall")
                    .setOnClickListener {
                        isStallReserved = !isStallReserved
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("StationDetailScreen - ${station.name}")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
