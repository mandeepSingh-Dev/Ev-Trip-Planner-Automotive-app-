/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.data.ChargingStationRepository
import com.oem.evplanner.domain.MultiStopTripPlanner
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.telemetry.EvTelemetryManager

/**
 * Screen class allowing drivers to search or select long-distance trip destinations
 * along major highway corridors using [SearchTemplate].
 */
class TripSearchScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val repository: ChargingStationRepository,
    private val mapRenderer: EvMapSurfaceRenderer
) : Screen(carContext) {

    private var queryText = ""

    // Preset destination corridors for trip planning
    private val presetCorridors = listOf(
        "Jaipur Pink City (NH-48)" to 275.0,
        "Chandigarh Sector 17 (NH-44)" to 248.0,
        "Agra Taj Expressway" to 232.0,
        "Neemrana Fort Highway Hub" to 122.0
    )

    /**
     * Builds and returns the [SearchTemplate] with search input and filtered destination corridors.
     */
    override fun onGetTemplate(): Template {
        val state = telemetryManager.telemetryState.value
        val stations = repository.stations.value

        val filtered = if (queryText.isBlank()) {
            presetCorridors
        } else {
            presetCorridors.filter { it.first.contains(queryText, ignoreCase = true) } +
                listOf("$queryText (Custom Trip)" to 210.0)
        }

        val listBuilder = ItemList.Builder()
        filtered.forEach { (destName, distKm) ->
            val plan = MultiStopTripPlanner.buildTripPlan(destName, distKm, state, stations)
            val stopCount = (plan.waypoints.size - 1).coerceAtLeast(0)
            val summary = if (stopCount == 0) {
                "${distKm.toInt()} km • Direct trip (Arrive ~${plan.finalArrivalSoc}% SoC)"
            } else {
                "${distKm.toInt()} km • $stopCount charging stop (+${plan.totalChargeMinutes} min)"
            }

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(destName)
                    .addText(summary)
                    .setOnClickListener {
                        screenManager.push(
                            TripItineraryScreen(carContext, telemetryManager, mapRenderer, plan)
                        )
                    }
                    .build()
            )
        }

        return SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    queryText = searchText
                    invalidate()
                }

                override fun onSearchSubmitted(searchText: String) {
                    queryText = searchText
                    invalidate()
                }
            }
        )
            .setHeaderAction(Action.BACK)
            .setSearchHint("TripSearchScreen - Search city or highway destination")
            .setShowKeyboardByDefault(false)
            .setItemList(listBuilder.build())
            .build()
    }
}
