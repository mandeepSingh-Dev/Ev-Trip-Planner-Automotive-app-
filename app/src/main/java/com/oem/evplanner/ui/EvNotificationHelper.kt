/**
 * Package definition for Android Auto / Automotive UI notification helpers.
 */
package com.oem.evplanner.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.notification.CarNotificationManager
import androidx.car.app.notification.CarPendingIntent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import com.oem.evplanner.R
import com.oem.evplanner.service.EvPlannerCarAppService

/**
 * Helper object responsible for showing high-priority automotive car notifications
 * such as low battery range warnings.
 */
object EvNotificationHelper {

    private const val CHANNEL_ID = "ev_planner_alerts"

    /**
     * Shows a low battery alert notification on the car screen with a quick action to find fast chargers.
     *
     * @param context Context used for building notifications.
     * @param socPercent Current State of Charge percentage.
     * @param rangeKm Remaining driving range in kilometers.
     */
    fun showLowBatteryAlert(context: Context, socPercent: Int, rangeKm: Int) {
        val manager = CarNotificationManager.from(context)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH,
        ).build()
        manager.createNotificationChannel(channel)

        val launchIntent = Intent(context, EvPlannerCarAppService::class.java)
        val carPendingIntent: PendingIntent = CarPendingIntent.getCarApp(
            context, 101, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Low EV Range: $socPercent% SoC ($rangeKm km)")
            .setContentText("Tap to route to the nearest 120kW+ fast charger")
            .extend(
                CarAppExtender.Builder()
                    .setImportance(NotificationManager.IMPORTANCE_HIGH)
                    .addAction(R.mipmap.ic_launcher, "Find Charger", carPendingIntent)
                    .build()
            )
        manager.notify(2001, notification)
    }
}
