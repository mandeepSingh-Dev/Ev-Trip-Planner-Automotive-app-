/**
 * Package definition for domain models and business logic.
 */
package com.oem.evplanner.domain

import com.oem.evplanner.model.ConnectorStandard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Data class representing driver preferences for EV trip planning and charging.
 */
data class EvDriverPreferences(
    val minArrivalSocBufferPercent: Int = 10,
    val preferredConnector: ConnectorStandard = ConnectorStandard.CCS2,
    val autoPreconditionOnRouteStart: Boolean = true,
    val plugAndChargeEnabled: Boolean = true
)

/**
 * Singleton state store managing driver preferences across the application.
 */
object EvPreferencesStore {
    /** Mutable backing StateFlow for driver preferences. */
    private val _prefs = MutableStateFlow(EvDriverPreferences())
    /** Read-only StateFlow exposing driver preferences to UI and domain logic. */
    val prefs: StateFlow<EvDriverPreferences> = _prefs.asStateFlow()

    /**
     * Cycles the minimum arrival State of Charge buffer percentage between 10%, 15%, and 20%.
     */
    fun cycleMinArrivalBuffer() {
        _prefs.update {
            val next = when (it.minArrivalSocBufferPercent) {
                10 -> 15
                15 -> 20
                else -> 10
            }
            it.copy(minArrivalSocBufferPercent = next)
        }
    }

    /**
     * Toggles the Plug & Charge auto-authentication setting.
     */
    fun togglePlugAndCharge() {
        _prefs.update { it.copy(plugAndChargeEnabled = !it.plugAndChargeEnabled) }
    }
}
