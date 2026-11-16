/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import com.oem.evplanner.data.ChargingStationRepository
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.permissions.EvPermissionHelper
import com.oem.evplanner.telemetry.EvTelemetryManager

/**
 * Screen class prompting the driver to grant required automotive permissions (car energy and location)
 * using [MessageTemplate].
 */
class PermissionRequiredScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val repository: ChargingStationRepository,
    private val mapRenderer: EvMapSurfaceRenderer
) : Screen(carContext) {

    private var deniedPermissions: List<String> = emptyList()

    init {
        // Automatically trigger the system permission dialog on screen open
        triggerPermissionPrompt()
    }

    /**
     * Triggers the permission request prompt via [EvPermissionHelper].
     */
    private fun triggerPermissionPrompt() {
        EvPermissionHelper.requestMissingPermissions(carContext) { allGranted, _, rejected ->
            if (allGranted) {
                // Re-bind VHAL listeners now that CAR_ENERGY is granted
                telemetryManager.refreshAfterPermissionGrant()
                screenManager.pop()
                screenManager.push(
                    ChargerListScreen(carContext, telemetryManager, repository, mapRenderer)
                )
            } else {
                deniedPermissions = rejected
                invalidate()
            }
        }
    }

    /**
     * Builds and returns the [MessageTemplate] explaining permission requirements with a grant action button.
     */
    override fun onGetTemplate(): Template {
        val message = if (deniedPermissions.isEmpty()) {
            "EV Trip Planner requires access to vehicle battery telemetry and location to calculate real-time range and locate nearby fast chargers."
        } else {
            "Vehicle battery or location permission was denied. Grant access to view live State of Charge (SoC) and reachable charging stations."
        }

        val grantAction = Action.Builder()
            .setTitle("Grant Permissions")
            .setBackgroundColor(CarColor.PRIMARY)
            .setOnClickListener {
                triggerPermissionPrompt()
            }
            .build()

        return MessageTemplate.Builder(message)
            .setTitle("PermissionRequiredScreen - Permissions Required")
            .setHeaderAction(Action.APP_ICON)
            .addAction(grantAction)
            .build()
    }
}
