/**
 * Package definition for Android Auto / Automotive UI screens and templates.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oem.evplanner.domain.EvRangeCalculator
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Screen class displaying turn-by-turn navigation guidance and travel estimates
 * toward a selected charging station using [NavigationTemplate].
 */
class ActiveNavigationScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val mapRenderer: EvMapSurfaceRenderer,
    private val station: ChargingStation,
    private val initialEtaMinutes: Int,
    private val isEcoRoute: Boolean
) : Screen(carContext) {

    private var currentState = telemetryManager.telemetryState.value
    private val navigationManager = carContext.getCarService(NavigationManager::class.java)

    init {
        // Set navigation manager callback to handle stop navigation events
        navigationManager.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() {
                screenManager.pop()
            }
        })
        // Notify car system that navigation has started
        navigationManager.navigationStarted()
        mapRenderer.updateMapState(currentState.rangeRemainingKm, station, isEcoRoute)

        // Observe lifecycle to end navigation when destroyed
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                navigationManager.navigationEnded()
            }
        })

        // Observe live telemetry state changes to update routing and UI
        telemetryManager.telemetryState
            .onEach { newState ->
                currentState = newState
                invalidate()
            }
            .launchIn(lifecycleScope)
    }

    /**
     * Builds and returns the [NavigationTemplate] with routing instructions and travel estimates.
     */
    override fun onGetTemplate(): Template {
        val estimate = EvRangeCalculator.evaluateRouteToStation(currentState, station)

        // Build turn maneuver indicator
        val maneuver = Maneuver.Builder(Maneuver.TYPE_KEEP_LEFT)
            .setIcon(CarIcon.APP_ICON)
            .build()

        // Build navigation step
        val step = Step.Builder("Continue toward ${station.name}")
            .setManeuver(maneuver)
            .setRoad("NH-48 Express Corridor")
            .build()

        // Build routing info container
        val routingInfo = RoutingInfo.Builder()
            .setCurrentStep(step, Distance.create(2.4, Distance.UNIT_KILOMETERS))
            .build()

        // Calculate arrival time
        val arrivalTime = DateTimeWithZone.create(
            ZonedDateTime.now().plusMinutes(initialEtaMinutes.toLong())
        )

        // Build travel estimate with remaining distance, time, and SoC color coding
        val travelEstimate = TravelEstimate.Builder(
            Distance.create(station.distanceKm, Distance.UNIT_KILOMETERS),
            arrivalTime
        )
            .setRemainingTimeSeconds(TimeUnit.MINUTES.toSeconds(initialEtaMinutes.toLong()))
            .setRemainingTimeColor(if (estimate.arrivalSocPercent >= 15) CarColor.GREEN else CarColor.YELLOW)
            .build()

        // Action to end route navigation
        val endTripAction = Action.Builder()
            .setTitle("End Route")
            .setBackgroundColor(CarColor.RED)
            .setOnClickListener {
                navigationManager.navigationEnded()
                screenManager.pop()
            }
            .build()

        // Action to simulate arrival at the charging station
        val simulateArrivalAction = Action.Builder()
            .setTitle("Plug In")
            .setOnClickListener {
                screenManager.push(ChargingSessionScreen(carContext, telemetryManager))
            }
            .build()

        return NavigationTemplate.Builder()
            .setNavigationInfo(routingInfo)
            .setDestinationTravelEstimate(travelEstimate)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(simulateArrivalAction)
                    .addAction(endTripAction)
                    .build()
            )
            .build()
    }
}
