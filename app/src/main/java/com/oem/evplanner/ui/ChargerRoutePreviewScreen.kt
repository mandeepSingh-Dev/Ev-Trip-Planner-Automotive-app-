/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.lifecycle.lifecycleScope
import com.oem.evplanner.domain.EvRangeCalculator
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

/**
 * Screen class displaying route options, energy estimates, and preconditioning controls
 * prior to starting navigation to a selected charging station using [RoutePreviewNavigationTemplate].
 */
class ChargerRoutePreviewScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val mapRenderer: EvMapSurfaceRenderer,
    private val station: ChargingStation
) : Screen(carContext) {

    private var currentState = telemetryManager.telemetryState.value
    private var selectedRouteIndex = 0
    private var isPreconditioningOn = true

    init {
        // Update map surface with initial route preview state
        mapRenderer.updateMapState(currentState.rangeRemainingKm, station, ecoRoute = false)

        // Observe telemetry changes to update route calculations in real time
        telemetryManager.telemetryState
            .onEach { newState ->
                currentState = newState
                invalidate()
            }
            .launchIn(lifecycleScope)
    }

    /**
     * Builds and returns the [RoutePreviewNavigationTemplate] with route choices and preconditioning toggle.
     */
    override fun onGetTemplate(): Template {
        // Define route options (Fastest Route vs. Eco Range-Saver)
        val routes = listOf(
            Triple("Fastest Route (${station.distanceKm} km)", station.distanceKm, station.estimatedMinutes),
            Triple("Eco Range-Saver (${(station.distanceKm * 1.06).toInt()} km)", station.distanceKm * 0.88, station.estimatedMinutes + 6)
        )

        val listBuilder = ItemList.Builder()
            .setSelectedIndex(selectedRouteIndex)
            .setOnSelectedListener { index ->
                selectedRouteIndex = index
                mapRenderer.updateMapState(currentState.rangeRemainingKm, station, ecoRoute = (index == 1))
                invalidate()
            }

        // Populate route options into the list builder
        routes.forEach { (title, effectiveEnergyKm, etaMins) ->
            val estimate = EvRangeCalculator.evaluateRouteToStation(
                state = currentState,
                station = station,
                effectiveDistanceKm = effectiveEnergyKm,
                isPreconditioningActive = isPreconditioningOn
            )

            val etaAndSoc = SpannableString("   • Arrive at ${estimate.arrivalSocPercent}% SoC (${estimate.arrivalRangeKm} km left)").apply {
                setSpan(
                    DurationSpan.create(TimeUnit.MINUTES.toSeconds(etaMins.toLong())),
                    0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                )
            }

            val chargeDetail = estimate.safetyBufferWarning
                ?: "Charge ~${estimate.recommendedChargeMinutesTo80} min to 80% (~₹${estimate.estimatedCostInr})"

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .addText(etaAndSoc)
                    .addText(chargeDetail)
                    .build()
            )
        }

        // Action button for toggling battery preconditioning
        val preconditionAction = Action.Builder()
            .setTitle(if (isPreconditioningOn) "Preconditioning: ON" else "Precondition: OFF")
            .setOnClickListener {
                isPreconditioningOn = !isPreconditioningOn
                invalidate()
            }
            .build()

        // Action button to start active navigation
        val navigateAction = Action.Builder()
            .setTitle("Start Navigation")
            .setBackgroundColor(CarColor.PRIMARY)
            .setOnClickListener {
                val chosenEta = routes[selectedRouteIndex].third
                screenManager.push(
                    ActiveNavigationScreen(
                        carContext = carContext,
                        telemetryManager = telemetryManager,
                        mapRenderer = mapRenderer,
                        station = station,
                        initialEtaMinutes = chosenEta,
                        isEcoRoute = (selectedRouteIndex == 1)
                    )
                )
            }
            .build()

        return RoutePreviewNavigationTemplate.Builder()
            .setTitle(station.name)
            .setHeaderAction(Action.BACK)
            .setActionStrip(ActionStrip.Builder().addAction(preconditionAction).build())
            .setItemList(listBuilder.build())
            .setNavigateAction(navigateAction)
            .build()
    }
}
