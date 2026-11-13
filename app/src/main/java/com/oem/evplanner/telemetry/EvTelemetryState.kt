/**
 * Package definition for vehicle telemetry and VHAL integration classes.
 */
package com.oem.evplanner.telemetry

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

/**
 * Data class representing the live telemetry state of the electric vehicle (EV),
 * retrieved via the Vehicle Hardware Abstraction Layer (VHAL).
 */
data class EvTelemetryState(
    val isConnectedToCarService: Boolean = false,
    val batteryEnergyWh: Float = 31_500f,       // 42% of 75 kWh default for initial display
    val batteryCapacityWh: Float = 75_000f,
    val rangeRemainingMeters: Float = 158_000f, // 158 km default
    val instantaneousChargeRateMilliwatts: Float = 0f,
    val isChargePortConnected: Boolean = false,
    val currentGear: Int = VehicleGear.GEAR_PARK,
    val isDistractionOptimizationRequired: Boolean = false,
    val maxAllowedListItems: Int = 12,
    /** Ambient outside temperature in Celsius for thermal management calculations. */
    val ambientTempCelsius: Float = 24.0f
) {
    /** State of Charge percentage calculated from battery energy and total capacity. */
    val socPercent: Int
        get() = if (batteryCapacityWh > 0f) {
            ((batteryEnergyWh / batteryCapacityWh) * 100f).toInt().coerceIn(0, 100)
        } else 0

    /** Remaining driving range in kilometers. */
    val rangeRemainingKm: Int
        get() = (rangeRemainingMeters / 1000f).toInt()

    /** Whether the vehicle is currently in Park gear. */
    val isParked: Boolean
        get() = currentGear == VehicleGear.GEAR_PARK

    /** Charging power in kilowatts (kW) derived from absolute instantaneous charge rate. */
    val chargingPowerKw: Float
        get() = abs(instantaneousChargeRateMilliwatts) / 1_000_000f

    /** Whether cold weather derating is active (ambient temperature below 10°C). */
    val isColdWeatherDeratingActive: Boolean
        get() = ambientTempCelsius < 10.0f
}

/**
 * Manager class responsible for connecting to the Android Car Service and subscribing
 * to VHAL properties for EV telemetry updates.
 */
class EvTelemetryManager(context: Context) {

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var uxRestrictionsManager: CarUxRestrictionsManager? = null

    /** Mutable backing StateFlow for EV telemetry state. */
    private val _telemetryState = MutableStateFlow(EvTelemetryState())
    /** Read-only StateFlow exposing EV telemetry state to UI and domain components. */
    val telemetryState: StateFlow<EvTelemetryState> = _telemetryState.asStateFlow()

    /** List of VHAL property IDs tracked by the telemetry manager. */
    private val trackedProperties = listOf(
        VehiclePropertyIds.EV_BATTERY_LEVEL,
        VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
        VehiclePropertyIds.CURRENT_GEAR,
        VehiclePropertyIds.RANGE_REMAINING,
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE
    )

    /** Callback listener for VHAL property change events. */
    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            when (value.propertyId) {
                VehiclePropertyIds.EV_BATTERY_LEVEL -> {
                    (value.value as? Float)?.let { wh ->
                        _telemetryState.update { it.copy(batteryEnergyWh = wh) }
                    }
                }
                VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> {
                    (value.value as? Float)?.let { mw ->
                        _telemetryState.update { it.copy(instantaneousChargeRateMilliwatts = mw) }
                    }
                }
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> {
                    (value.value as? Boolean)?.let { connected ->
                        _telemetryState.update { it.copy(isChargePortConnected = connected) }
                    }
                }
                VehiclePropertyIds.CURRENT_GEAR -> {
                    (value.value as? Int)?.let { gear ->
                        _telemetryState.update {
                            it.copy(
                                currentGear = gear,
                                isDistractionOptimizationRequired = gear != VehicleGear.GEAR_PARK
                            )
                        }
                    }
                }
                VehiclePropertyIds.RANGE_REMAINING -> {
                    (value.value as? Float)?.let { meters ->
                        _telemetryState.update { it.copy(rangeRemainingMeters = meters) }
                    }
                }
                VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> {
                    (value.value as? Float)?.let { tempC ->
                        _telemetryState.update { it.copy(ambientTempCelsius = tempC) }
                    }
                }
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.e(TAG, "VHAL Error on propId=$propId, zone=$zone")
        }
    }

    /** Listener for car UX restrictions changes (e.g. driving state optimizations). */
    private val uxListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        applyUxRestrictions(restrictions)
    }

    init {
        // Initialize connection to Android Car Service
        car = Car.createCar(
            context.applicationContext,
            null,
            Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER
        ) { carInstance, ready ->
            if (ready) {
                propertyManager = carInstance.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                uxRestrictionsManager = carInstance.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager

                _telemetryState.update { it.copy(isConnectedToCarService = true) }
                readBatteryCapacity()
                registerPropertyCallbacks()
                registerUxRestrictions()
            }
        }
    }

    /**
     * Reads EV battery total capacity property from VHAL.
     */
    private fun readBatteryCapacity() {
        try {
            val cap = propertyManager?.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0) ?: 0f
            if (cap > 0f) {
                _telemetryState.update { it.copy(batteryCapacityWh = cap) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "INFO_EV_BATTERY_CAPACITY unavailable; keeping 75 kWh fallback", e)
        }
    }

    /**
     * Registers property callbacks for all tracked VHAL properties.
     */
    private fun registerPropertyCallbacks() {
        val manager = propertyManager ?: return
        trackedProperties.forEach { propId ->
            try {
                manager.registerCallback(
                    propertyCallback,
                    propId,
                    CarPropertyManager.SENSOR_RATE_ONCHANGE
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission missing for VHAL property $propId", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Property $propId unsupported on this VHAL", e)
            }
        }
    }

    /**
     * Registers UX restrictions listener.
     */
    private fun registerUxRestrictions() {
        val uxManager = uxRestrictionsManager ?: return
        uxManager.registerListener(uxListener)
        uxManager.currentCarUxRestrictions?.let { applyUxRestrictions(it) }
    }

    /**
     * Applies UX restrictions to the telemetry state.
     */
    private fun applyUxRestrictions(restrictions: CarUxRestrictions) {
        _telemetryState.update {
            it.copy(
                isDistractionOptimizationRequired = restrictions.isRequiresDistractionOptimization,
                maxAllowedListItems = restrictions.maxCumulativeContentItems.coerceIn(1, 12)
            )
        }
    }

    /**
     * Releases VHAL callbacks and disconnects from car service.
     */
    fun release() {
        propertyManager?.unregisterCallback(propertyCallback)
        uxRestrictionsManager?.unregisterListener()
        car?.disconnect()
    }

    /**
     * Refreshes battery capacity and property subscriptions after runtime permissions are granted.
     */
    fun refreshAfterPermissionGrant() {
        propertyManager?.unregisterCallback(propertyCallback)
        readBatteryCapacity()
        registerPropertyCallbacks()
    }

    companion object {
        private const val TAG = "EvTelemetryManager"
    }
}
