package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.model.ChargingStation

class ReportIssueGridScreen(
    carContext: CarContext,
    private val station: ChargingStation
) : Screen(carContext) {

    private val issueOptions = listOf(
        "Stall Blocked" to "ICE vehicle or parked car blocking bay",
        "Charger Offline" to "Screen black or network error",
        "Slow Speed" to "Delivering <30 kW on DC fast stall",
        "Working Great" to "Confirm full-speed session"
    )

    override fun onGetTemplate(): Template {
        val gridBuilder = ItemList.Builder()

        issueOptions.forEach { (title, subtitle) ->
            gridBuilder.addItem(
                GridItem.Builder()
                    .setTitle(title)
                    .setText(subtitle)
                    .setImage(CarIcon.APP_ICON)
                    .setOnClickListener {
                        CarToast.makeText(
                            carContext,
                            "Reported '$title' for ${station.name}",
                            CarToast.LENGTH_LONG
                        ).show()
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return GridTemplate.Builder()
            .setTitle("Report Status • ${station.name}")
            .setHeaderAction(Action.BACK)
            .setSingleList(gridBuilder.build())
            .build()
    }
}