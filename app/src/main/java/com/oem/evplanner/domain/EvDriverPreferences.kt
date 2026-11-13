package com.oem.evplanner.domain

import com.oem.evplanner.model.ConnectorStandard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EvDriverPreferences(
    val minArrivalSocBufferPercent: Int = 10,
    val preferredConnector: ConnectorStandard = ConnectorStandard.CCS2,
    val autoPreconditionOnRouteStart: Boolean = true,
    val plugAndChargeEnabled: Boolean = true
)

object EvPreferencesStore {
    private val _prefs = MutableStateFlow(EvDriverPreferences())
    val prefs: StateFlow<EvDriverPreferences> = _prefs.asStateFlow()

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

    fun togglePlugAndCharge() {
        _prefs.update { it.copy(plugAndChargeEnabled = !it.plugAndChargeEnabled) }
    }
}