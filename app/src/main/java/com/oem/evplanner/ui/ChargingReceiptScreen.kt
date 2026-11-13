/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import java.util.Locale

/**
 * Screen class displaying the charging session receipt and billing summary using [PaneTemplate].
 */
class ChargingReceiptScreen(
    carContext: CarContext,
    private val stationName: String,
    private val energyDeliveredKwh: Double,
    private val finalSocPercent: Int,
    private val ratePerKwhInr: Double = 19.50
) : Screen(carContext) {

    /**
     * Builds and returns the [PaneTemplate] showing session details, energy delivered, and total cost.
     */
    override fun onGetTemplate(): Template {
        val kwhStr = String.format(Locale.US, "%.1f", energyDeliveredKwh)
        val totalCost = (energyDeliveredKwh * ratePerKwhInr).toInt()

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("Session Complete • $stationName")
                    .addText("ISO 15118 Plug & Charge Auto-Settled")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Energy Delivered: +$kwhStr kWh")
                    .addText("Final Battery Level: $finalSocPercent% SoC")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Total Billed: ₹$totalCost")
                    .addText("Tariff: ₹$ratePerKwhInr/kWh (GST Included)")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Done")
                    .setBackgroundColor(CarColor.PRIMARY)
                    .setOnClickListener { screenManager.popToRoot() }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane)
            .setTitle("Charging Session Receipt")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
