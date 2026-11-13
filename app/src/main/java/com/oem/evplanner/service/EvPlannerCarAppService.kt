/**
 * Package definition for Android Auto / Automotive app services and sessions.
 */
package com.oem.evplanner.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.oem.evplanner.data.ChargingStationRepository
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.permissions.EvPermissionHelper
import com.oem.evplanner.telemetry.EvTelemetryManager
import com.oem.evplanner.ui.ChargerListScreen
import com.oem.evplanner.ui.ChargerRoutePreviewScreen
import com.oem.evplanner.ui.ChargingSessionScreen
import com.oem.evplanner.ui.PermissionRequiredScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Main CarAppService implementation for the EV Trip Planner automotive application.
 */
class EvPlannerCarAppService : CarAppService() {
    /** Validates and allows connection from all car app hosts. */
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    /** Creates and returns the application session. */
    override fun onCreateSession(): Session = EvPlannerSession()
}

/**
 * Session class managing car app lifecycle, telemetry managers, repositories, and screen navigation stack.
 */
class EvPlannerSession : Session() {

    /** Telemetry manager for monitoring vehicle VHAL properties. */
    lateinit var telemetryManager: EvTelemetryManager
        private set
    /** Repository providing charging station data. */
    lateinit var repository: ChargingStationRepository
        private set
    /** Map surface renderer for drawing range rings and routes. */
    lateinit var mapRenderer: EvMapSurfaceRenderer
        private set

    /**
     * Called when the session is created, initializing telemetry, repository, map renderer,
     * observing plug-in events, and returning the initial screen.
     */
    override fun onCreateScreen(intent: Intent): Screen {
        telemetryManager = EvTelemetryManager(carContext)
        repository = ChargingStationRepository()
        mapRenderer = EvMapSurfaceRenderer(carContext)

        // Add lifecycle observer to release telemetry manager when session is destroyed
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                telemetryManager.release()
            }
        })

        // Observe charge port connection state changes to automatically trigger charging session screen
        observePlugInTransitions()

        // Return permission screen if required permissions are missing, otherwise return charger list screen
        return if (EvPermissionHelper.hasAllRequiredPermissions(carContext)) {
            ChargerListScreen(carContext, telemetryManager, repository, mapRenderer)
        } else {
            PermissionRequiredScreen(carContext, telemetryManager, repository, mapRenderer)
        }
    }

    /**
     * Observes vehicle charge port plug-in transitions and navigates to the charging session screen when plugged in.
     */
    private fun observePlugInTransitions() {
        telemetryManager.telemetryState
            .map { it.isChargePortConnected }
            .distinctUntilChanged()
            .onEach { isPluggedIn ->
                val screenManager = carContext.getCarService(ScreenManager::class.java)
                if (isPluggedIn && screenManager.top !is ChargingSessionScreen) {
                    screenManager.push(ChargingSessionScreen(carContext, telemetryManager))
                }
            }
            .launchIn(lifecycleScope)
    }

    /**
     * Requests required automotive permissions if not already granted.
     */
    private fun requestRequiredPermissionsIfNeeded() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "android.car.permission.CAR_ENERGY",
            "android.car.permission.CAR_ENERGY_PORTS",
            "android.car.permission.CAR_POWERTRAIN"
        )
        val missing = permissions.filter {
            carContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            carContext.requestPermissions(missing) { granted, _ ->
                if (granted.isNotEmpty()) {
                    telemetryManager.release()
                    telemetryManager = EvTelemetryManager(carContext)
                }
            }
        }
    }

    /**
     * Handles new intents delivered to the session (e.g. navigation intents or geo URIs).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == CarContext.ACTION_NAVIGATE || intent.data?.scheme == "geo") {
            val screenManager = carContext.getCarService(ScreenManager::class.java)
            val nearestStation = repository.stations.value.minByOrNull { it.distanceKm }
            if (nearestStation != null) {
                screenManager.popToRoot()
                screenManager.push(
                    ChargerRoutePreviewScreen(
                        carContext,
                        telemetryManager,
                        mapRenderer,
                        nearestStation
                    )
                )
            }
        }
    }
}
