/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.domain.EvRangeCalculator
import com.oem.evplanner.model.ChargingHistoryRecord
import com.oem.evplanner.telemetry.EvTelemetryManager
import java.util.Locale

/**
 * Screen class displaying energy analytics, consumption efficiency diagnostics,
 * and past charging history logs using [TabTemplate].
 */
class EnergyAnalyticsTabScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager
) : Screen(carContext) {

    private var activeTabId = TAB_DIAGNOSTICS

    // Sample charging history records
    private val historyLogs = listOf(
        ChargingHistoryRecord("h1", "Nov 11 • Cyber Hub", "Tata Power 120kW", 38.4, 114, 710),
        ChargingHistoryRecord("h2", "Nov 07 • Aerocity", "Statiq 180kW", 44.2, 162, 928),
        ChargingHistoryRecord("h3", "Nov 02 • Manesar NH48", "Zeon 240kW", 51.0, 198, 1147)
    )

    /**
     * Builds and returns the [TabTemplate] switching between energy diagnostics and charge history.
     */
    override fun onGetTemplate(): Template {
        val diagnosticsTab = Tab.Builder()
            .setTitle("Energy & Efficiency")
            .setContentId(TAB_DIAGNOSTICS)
            .setIcon(CarIcon.APP_ICON)
            .build()

        val historyTab = Tab.Builder()
            .setTitle("Charge History")
            .setContentId(TAB_HISTORY)
            .setIcon(CarIcon.APP_ICON)
            .build()

        val contentTemplate: Template = if (activeTabId == TAB_DIAGNOSTICS) {
            buildDiagnosticsPane()
        } else {
            buildHistoryList()
        }

        return TabTemplate.Builder(
            object : TabTemplate.TabCallback {
                override fun onTabSelected(tabContentId: String) {
                    activeTabId = tabContentId
                    invalidate()
                }
            }
        )
            .setHeaderAction(Action.BACK)
            .addTab(diagnosticsTab)
            .addTab(historyTab)
            .setActiveTabContentId(activeTabId)
            .setTabContents(TabContents.Builder(contentTemplate).build())
            .build()
    }

    /**
     * Builds the pane template for live energy consumption efficiency and battery diagnostics.
     */
    private fun buildDiagnosticsPane(): PaneTemplate {
        val state = telemetryManager.telemetryState.value
        val efficiencyWhKm = EvRangeCalculator.getLiveEfficiencyWhPerKm(state).toInt()
        val storedKwh = String.format(Locale.US, "%.1f", state.batteryEnergyWh / 1000f)
        val totalKwh = String.format(Locale.US, "%.1f", state.batteryCapacityWh / 1000f)

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("Live Consumption Efficiency")
                    .addText("$efficiencyWhKm Wh/km • Estimated Range: ${state.rangeRemainingKm} km")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("HV Battery Pack Status")
                    .addText("${state.socPercent}% SoC ($storedKwh / $totalKwh kWh) • Thermal State: Optimal")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Auxiliary & Climate Load")
                    .addText("Cabin HVAC & Preconditioning Reserve: ~1.4 kW")
                    .build()
            )
            .build()

        return PaneTemplate.Builder(pane).build()
    }

    /**
     * Builds the list template containing past charging session history records.
     */
    private fun buildHistoryList(): ListTemplate {
        val listBuilder = ItemList.Builder()
        historyLogs.forEach { log ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("${log.stationName} (+${log.energyAddedKwh} kWh)")
                    .addText("${log.dateLabel} • Peak ${log.peakPowerKw}kW • ₹${log.totalCostInr}")
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .build()
    }

    companion object {
        private const val TAB_DIAGNOSTICS = "tab_diagnostics"
        private const val TAB_HISTORY = "tab_history"
    }
}
