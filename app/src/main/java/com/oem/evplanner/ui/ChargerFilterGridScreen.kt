package com.oem.evplanner.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import com.oem.evplanner.model.ChargerFilterMode

class ChargerFilterGridScreen(
    carContext: CarContext,
    private val currentFilter: ChargerFilterMode,
    private val onFilterSelected: (ChargerFilterMode) -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val gridBuilder = ItemList.Builder()

        ChargerFilterMode.entries.forEach { mode ->
            val isSelected = mode == currentFilter
            gridBuilder.addItem(
                GridItem.Builder()
                    .setTitle(mode.label)
                    .setText(if (isSelected) "Active" else "Tap to apply")
                    .setImage(CarIcon.APP_ICON)
                    .setOnClickListener {
                        onFilterSelected(mode)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return GridTemplate.Builder()
            .setTitle("Filter Charging Hubs")
            .setHeaderAction(Action.BACK)
            .setSingleList(gridBuilder.build())
            .build()
    }
}