package com.oem.evplanner.ui

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.oem.evplanner.data.ChargingStationRepository
import com.oem.evplanner.domain.EvRangeCalculator
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.model.ChargerFilterMode
import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.telemetry.EvTelemetryManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChargerListScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val repository: ChargingStationRepository,
    private val mapRenderer: EvMapSurfaceRenderer
) : Screen(carContext) {

    private var currentState = telemetryManager.telemetryState.value
    private var stations = repository.stations.value
    private var activeFilter = ChargerFilterMode.ALL
    private var hasSentLowBatteryAlert = false

    init {
        telemetryManager.telemetryState
            .onEach { newState ->
                currentState = newState
                mapRenderer.updateMapState(rangeKm = newState.rangeRemainingKm, station = null)

                if (newState.socPercent in 1..15 && !newState.isChargePortConnected && !hasSentLowBatteryAlert) {
                    hasSentLowBatteryAlert = true
                    EvNotificationHelper.showLowBatteryAlert(
                        context = carContext,
                        socPercent = newState.socPercent,
                        rangeKm = newState.rangeRemainingKm
                    )
                } else if (newState.socPercent > 20) {
                    hasSentLowBatteryAlert = false
                }

                invalidate()
            }
            .launchIn(lifecycleScope)

        repository.stations
            .onEach { list ->
                stations = list
                invalidate()
            }
            .launchIn(lifecycleScope)
    }

    override fun onGetTemplate(): Template {
        val filteredStations = applyFilter(stations, activeFilter)
        val itemListBuilder = ItemList.Builder()
            .setNoItemsMessage("No chargers match '${activeFilter.label}' within ${currentState.rangeRemainingKm} km")

        // Reserve 1 slot for the Energy & Battery Diagnostics entry row when driving restrictions apply
        val maxStationRows = (currentState.maxAllowedListItems - 1).coerceAtLeast(1)

        // Row 0: Quick entry to Energy Analytics & Live Charging Monitor
        val batterySummaryText = if (currentState.isChargePortConnected) {
            "   • Plugged In • ${String.format("%.1f", currentState.chargingPowerKw)} kW active — Tap to monitor"
        } else {
            "   • ${EvRangeCalculator.getLiveEfficiencyWhPerKm(currentState).toInt()} Wh/km avg • Tap for Energy & History"
        }

        val batterySummary = SpannableString(batterySummaryText).apply {
            setSpan(
                DistanceSpan.create(Distance.create(0.0, Distance.UNIT_KILOMETERS)),
                0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        itemListBuilder.addItem(
            Row.Builder()
                .setTitle("Battery & Energy Diagnostics (${currentState.socPercent}% SoC)")
                .addText(batterySummary)
                .setOnClickListener {
                    if (currentState.isChargePortConnected) {
                        screenManager.push(ChargingSessionScreen(carContext, telemetryManager))
                    } else {
                        screenManager.push(EnergyAnalyticsTabScreen(carContext, telemetryManager))
                    }
                }
                .build()
        )

        // Station Rows
        filteredStations
            .take(maxStationRows)
            .forEach { station ->
                val estimate = EvRangeCalculator.evaluateRouteToStation(currentState, station)
                val pinColor = when {
                    !estimate.isReachable -> CarColor.RED
                    station.availableStalls == 0 -> CarColor.YELLOW
                    else -> CarColor.GREEN
                }

                val distanceLine = SpannableString("   • Arrive ${estimate.arrivalSocPercent}% SoC (~₹${estimate.estimatedCostInr})").apply {
                    setSpan(
                        DistanceSpan.create(Distance.create(station.distanceKm, Distance.UNIT_KILOMETERS)),
                        0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }

                val statusLine = if (estimate.isReachable) {
                    "${station.availableStalls}/${station.totalStalls} free • ${station.maxPowerKw}kW ${station.connector.displayName}"
                } else {
                    "Low Range • ${station.distanceKm} km away (${station.maxPowerKw}kW)"
                }

                val place = Place.Builder(CarLocation.create(station.latitude, station.longitude))
                    .setMarker(PlaceMarker.Builder().setColor(pinColor).build())
                    .build()

                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(station.name)
                        .addText(distanceLine)
                        .addText(statusLine)
                        .setMetadata(Metadata.Builder().setPlace(place).build())
                        .setOnClickListener {
                            screenManager.push(
                                StationDetailScreen(carContext, telemetryManager, mapRenderer, station)
                            )
                        }
                        .build()
                )
            }

        // PlaceListMapTemplate ActionStrip supports up to 2 actions cleanly
        val tripPlannerAction = Action.Builder()
            .setTitle("Trip Plan")
            .setOnClickListener {
                screenManager.push(
                    TripSearchScreen(carContext, telemetryManager, repository, mapRenderer)
                )
            }
            .build()

        val filterAction = Action.Builder()
            .setTitle(if (activeFilter == ChargerFilterMode.ALL) "Filter" else activeFilter.label)
            .setOnClickListener {
                screenManager.push(
                    ChargerFilterGridScreen(carContext, activeFilter) { selectedMode ->
                        activeFilter = selectedMode
                        invalidate()
                    }
                )
            }
            .build()

        val headerTitle = "EV Chargers • ${currentState.socPercent}% (${currentState.rangeRemainingKm} km)"

        return PlaceListMapTemplate.Builder()
            .setTitle(headerTitle)
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(tripPlannerAction)
                    .addAction(filterAction)
                    .build()
            )
            .setItemList(itemListBuilder.build())
            .setCurrentLocationEnabled(true)
            .build()
    }

    private fun applyFilter(
        allStations: List<ChargingStation>,
        mode: ChargerFilterMode
    ): List<ChargingStation> {
        return when (mode) {
            ChargerFilterMode.ALL -> allStations
            ChargerFilterMode.IN_RANGE -> allStations.filter {
                EvRangeCalculator.evaluateRouteToStation(currentState, it).isReachable
            }
            ChargerFilterMode.ULTRA_FAST -> allStations.filter { it.maxPowerKw >= 150 }
            ChargerFilterMode.AVAILABLE_NOW -> allStations.filter { it.availableStalls > 0 }
        }
    }
}