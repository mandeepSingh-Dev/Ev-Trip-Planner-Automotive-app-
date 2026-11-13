/**
 * Package definition for Android Auto / Automotive UI screens.
 */
package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.domain.LongDistanceTripPlan
import com.oem.evplanner.map.EvMapSurfaceRenderer
import com.oem.evplanner.telemetry.EvTelemetryManager

/**
 * Screen class displaying a long-distance multi-stop trip itinerary with waypoints
 * and charging stops using [ListTemplate].
 */
class TripItineraryScreen(
    carContext: CarContext,
    private val telemetryManager: EvTelemetryManager,
    private val mapRenderer: EvMapSurfaceRenderer,
    private val plan: LongDistanceTripPlan
) : Screen(carContext) {

    /**
     * Builds and returns the [ListTemplate] showing trip waypoints and start trip action.
     */
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // Populate waypoints into list builder
        plan.waypoints.forEach { waypoint ->
            val rowBuilder = Row.Builder()
                .setTitle(waypoint.title)
                .addText(waypoint.subtitle)

            waypoint.station?.let { station ->
                rowBuilder.setOnClickListener {
                    screenManager.push(
                        ChargerRoutePreviewScreen(carContext, telemetryManager, mapRenderer, station)
                    )
                }
            }
            listBuilder.addItem(rowBuilder.build())
        }

        // Action button to start the first leg of the trip
        val startFirstLegAction = Action.Builder()
            .setTitle("Start Trip")
            .setBackgroundColor(CarColor.PRIMARY)
            .setOnClickListener {
                val firstStation = plan.waypoints.firstNotNullOfOrNull { it.station }
                if (firstStation != null) {
                    screenManager.push(
                        ChargerRoutePreviewScreen(carContext, telemetryManager, mapRenderer, firstStation)
                    )
                }
            }
            .build()

        return ListTemplate.Builder()
            .setTitle("Trip: ${plan.destinationName}")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .setActionStrip(ActionStrip.Builder().addAction(startFirstLegAction).build())
            .build()
    }
}
