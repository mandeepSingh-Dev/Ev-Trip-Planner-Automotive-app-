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
        navigationManager.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() {
                screenManager.pop()
            }
        })
        navigationManager.navigationStarted()
        mapRenderer.updateMapState(currentState.rangeRemainingKm, station, isEcoRoute)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                navigationManager.navigationEnded()
            }
        })

        telemetryManager.telemetryState
            .onEach { newState ->
                currentState = newState
                invalidate()
            }
            .launchIn(lifecycleScope)
    }

    override fun onGetTemplate(): Template {
        val estimate = EvRangeCalculator.evaluateRouteToStation(currentState, station)

        val maneuver = Maneuver.Builder(Maneuver.TYPE_KEEP_LEFT)
            .setIcon(CarIcon.APP_ICON)
            .build()

        val step = Step.Builder("Continue toward ${station.name}")
            .setManeuver(maneuver)
            .setRoad("NH-48 Express Corridor")
            .build()

        val routingInfo = RoutingInfo.Builder()
            .setCurrentStep(step, Distance.create(2.4, Distance.UNIT_KILOMETERS))
            .build()

        val arrivalTime = DateTimeWithZone.create(
            ZonedDateTime.now().plusMinutes(initialEtaMinutes.toLong())
        )

        val travelEstimate = TravelEstimate.Builder(
            Distance.create(station.distanceKm, Distance.UNIT_KILOMETERS),
            arrivalTime
        )
            .setRemainingTimeSeconds(TimeUnit.MINUTES.toSeconds(initialEtaMinutes.toLong()))
            .setRemainingTimeColor(if (estimate.arrivalSocPercent >= 15) CarColor.GREEN else CarColor.YELLOW)
            .build()

        val endTripAction = Action.Builder()
            .setTitle("End Route")
            .setBackgroundColor(CarColor.RED)
            .setOnClickListener {
                navigationManager.navigationEnded()
                screenManager.pop()
            }
            .build()

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