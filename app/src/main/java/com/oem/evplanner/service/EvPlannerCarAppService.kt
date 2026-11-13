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

class EvPlannerCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = EvPlannerSession()
}

class EvPlannerSession : Session() {

    lateinit var telemetryManager: EvTelemetryManager
        private set
    lateinit var repository: ChargingStationRepository
        private set
    lateinit var mapRenderer: EvMapSurfaceRenderer
        private set
    override fun onCreateScreen(intent: Intent): Screen {
        telemetryManager = EvTelemetryManager(carContext)
        repository = ChargingStationRepository()
        mapRenderer = EvMapSurfaceRenderer(carContext)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                telemetryManager.release()
            }
        })

        observePlugInTransitions()

        return if (EvPermissionHelper.hasAllRequiredPermissions(carContext)) {
            ChargerListScreen(carContext, telemetryManager, repository, mapRenderer)
        } else {
            PermissionRequiredScreen(carContext, telemetryManager, repository, mapRenderer)
        }
    }

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
    // Add inside EvPlannerSession.kt
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