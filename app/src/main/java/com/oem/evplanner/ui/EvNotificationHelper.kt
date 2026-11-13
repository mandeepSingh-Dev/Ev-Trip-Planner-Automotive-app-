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

object EvNotificationHelper {

    private const val CHANNEL_ID = "ev_planner_alerts"

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