/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.domain.EvPreferencesStore
import com.oem.evplanner.telemetry.EvTelemetryManager

/**
 * Screen class displaying vehicle settings, minimum arrival SoC buffers, Plug & Charge preferences,
 * and ambient thermal status using [ListTemplate].
 */
class VehicleSettingsScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager
) : Screen(carContext) {

    /**
     * Builds and returns the [ListTemplate] showing driver preferences and vehicle settings options.
     */
    override fun onGetTemplate(): Template {
        val prefs = EvPreferencesStore.prefs.value
        val state = telemetryManager.telemetryState.value

        val listBuilder = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle("Minimum Arrival SoC Buffer: ${prefs.minArrivalSocBufferPercent}%")
                    .addText("Tap to cycle safety reserve (10% / 15% / 20%)")
                    .setOnClickListener {
                        EvPreferencesStore.cycleMinArrivalBuffer()
                        invalidate()
                    }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("ISO 15118 Plug & Charge: ${if (prefs.plugAndChargeEnabled) "Enabled" else "Manual RFID"}")
                    .addText("Auto-authenticate vehicle certificate at supported DC hubs")
                    .setOnClickListener {
                        EvPreferencesStore.togglePlugAndCharge()
                        invalidate()
                    }
                    .build()
            )
            .addItem(
                Row.Builder()
                    .setTitle("Ambient Thermal Model: ${state.ambientTempCelsius.toInt()}°C")
                    .addText(
                        if (state.isColdWeatherDeratingActive)
                            "Cold weather active (+18% energy buffer applied)"
                        else
                            "Standard thermal efficiency curve active"
                    )
                    .build()
            )

        return ListTemplate.Builder()
            .setTitle("VehicleSettingsScreen - EV Trip & Vehicle Settings")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }
}
