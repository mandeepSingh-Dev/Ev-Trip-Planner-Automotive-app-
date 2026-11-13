package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.oem.evplanner.telemetry.EvTelemetryManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

class ChargingSessionScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager
) : Screen(carContext) {

    private var currentState = telemetryManager.telemetryState.value
    private var chargeTargetPercent = 80

    init {
        telemetryManager.telemetryState
            .onEach { newState ->
                currentState = newState
                invalidate()
            }
            .launchIn(lifecycleScope)
    }

    override fun onGetTemplate(): Template {
        val currentKwh = String.format(Locale.US, "%.1f", currentState.batteryEnergyWh / 1000f)
        val capacityKwh = String.format(Locale.US, "%.1f", currentState.batteryCapacityWh / 1000f)
        val powerKw = String.format(Locale.US, "%.1f", currentState.chargingPowerKw)

        val portStatus = if (currentState.isChargePortConnected) {
            "Connected & Locked ($powerKw kW intake)"
        } else {
            "Disconnected — Ready to Plug In"
        }

        val targetWh = currentState.batteryCapacityWh * (chargeTargetPercent / 100f)
        val remainingWh = (targetWh - currentState.batteryEnergyWh).coerceAtLeast(0f)
        val minsToTarget = if (currentState.chargingPowerKw > 1f) {
            (((remainingWh / 1000f) / currentState.chargingPowerKw) * 60f).toInt()
        } else {
            0
        }

        val targetDetail = when {
            currentState.isChargePortConnected && minsToTarget > 0 ->
                "Estimated ~$minsToTarget min remaining to $chargeTargetPercent%"
            currentState.socPercent >= chargeTargetPercent ->
                "Target SoC ($chargeTargetPercent%) reached"
            else ->
                "Gear Status: ${if (currentState.isParked) "PARK (Charging Ready)" else "SHIFT TO PARK"}"
        }

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("State of Charge: ${currentState.socPercent}% ($currentKwh / $capacityKwh kWh)")
                    .addText("Estimated Range: ${currentState.rangeRemainingKm} km")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Charge Port Status")
                    .addText(portStatus)
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Target Limit: $chargeTargetPercent% SoC")
                    .addText(targetDetail)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (chargeTargetPercent == 80) "Set 100% Trip" else "Set 80% Daily")
                    .setOnClickListener {
                        chargeTargetPercent = if (chargeTargetPercent == 80) 100 else 80
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Find Chargers")
                    .setBackgroundColor(CarColor.PRIMARY)
                    .setOnClickListener { screenManager.popToRoot() }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("EV Battery & Charging Telemetry")
            .setHeaderAction(Action.BACK)
            .build()
    }
}