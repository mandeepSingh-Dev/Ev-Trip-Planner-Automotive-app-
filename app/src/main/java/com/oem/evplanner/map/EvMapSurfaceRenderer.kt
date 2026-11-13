/**
 * Package definition for map rendering and custom surface surface callbacks.
 */
package com.oem.evplanner.map

import android.graphics.*
import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.oem.evplanner.model.ChargingStation

/**
 * Surface renderer class implementing [SurfaceCallback] to draw custom EV range rings,
 * route polylines, and charging station pins directly onto the car screen surface.
 */
class EvMapSurfaceRenderer(carContext: CarContext) : SurfaceCallback {

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var visibleArea: Rect? = null

    private var currentRangeKm: Int = 158
    private var selectedStation: ChargingStation? = null
    private var isEcoRoute: Boolean = false

    // Paint objects for rendering map elements
    private val bgPaint = Paint().apply { color = Color.parseColor("#11161D") }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25303D")
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val rangeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 56, 189, 248)
        style = Paint.Style.FILL
    }
    private val rangeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B82F6")
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    private val vehiclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60A5FA")
        style = Paint.Style.FILL
    }
    private val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22C55E")
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        // Register surface callback with the Car App Manager
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
    }

    /**
     * Updates the map rendering state and redraws the frame.
     *
     * @param rangeKm Current vehicle range in kilometers.
     * @param station Selected charging station, if any.
     * @param ecoRoute Whether eco route rendering mode is active.
     */
    fun updateMapState(rangeKm: Int, station: ChargingStation?, ecoRoute: Boolean = false) {
        currentRangeKm = rangeKm
        selectedStation = station
        isEcoRoute = ecoRoute
        renderFrame()
    }

    /**
     * Called when the car screen surface becomes available for drawing.
     */
    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
        width = surfaceContainer.width
        height = surfaceContainer.height
        renderFrame()
    }

    /**
     * Called when the visible map area dimensions change (e.g. split templates).
     */
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea
        renderFrame()
    }

    /**
     * Called when the surface is destroyed.
     */
    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null
    }

    /**
     * Renders a frame on the car surface canvas including background, roads, range ring, route, and vehicle pin.
     */
    private fun renderFrame() {
        val activeSurface = surface ?: return
        if (!activeSurface.isValid || width == 0 || height == 0) return

        val canvas = activeSurface.lockCanvas(null) ?: return
        try {
            // Draw background fill
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Offset center into the visible unoccluded right half of split templates
            val area = visibleArea ?: Rect(0, 0, width, height)
            val centerX = area.left + (area.width() * 0.55f)
            val centerY = area.top + (area.height() * 0.62f)

            // 1. Subtle road grid
            for (x in area.left..area.right step 120) {
                canvas.drawLine(x.toFloat(), area.top.toFloat(), x.toFloat(), area.bottom.toFloat(), roadPaint)
            }
            for (y in area.top..area.bottom step 120) {
                canvas.drawLine(area.left.toFloat(), y.toFloat(), area.right.toFloat(), y.toFloat(), roadPaint)
            }

            // 2. Range circle halo proportional to VHAL RANGE_REMAINING
            val radiusPx = (currentRangeKm * 1.4f).coerceIn(70f, area.width() * 0.42f)
            canvas.drawCircle(centerX, centerY, radiusPx, rangeFillPaint)
            canvas.drawCircle(centerX, centerY, radiusPx, rangeStrokePaint)

            // 3. Route polyline & destination charger pin if a station is selected
            selectedStation?.let { station ->
                val destX = centerX + (area.width() * 0.22f)
                val destY = area.top + (area.height() * 0.28f)

                routePaint.color = if (isEcoRoute) Color.parseColor("#22C55E") else Color.parseColor("#3B82F6")
                val path = Path().apply {
                    moveTo(centerX, centerY)
                    if (isEcoRoute) {
                        quadTo(centerX + 140f, centerY - 60f, destX, destY)
                    } else {
                        quadTo(centerX - 40f, centerY - 140f, destX, destY)
                    }
                }
                canvas.drawPath(path, routePaint)

                // Destination charger marker circle and power label
                canvas.drawCircle(destX, destY, 22f, pinPaint)
                canvas.drawText("${station.maxPowerKw}kW", destX + 30f, destY + 10f, labelPaint)
            }

            // 4. Vehicle location chevron indicator
            val chevron = Path().apply {
                moveTo(centerX, centerY - 24f)
                lineTo(centerX + 18f, centerY + 18f)
                lineTo(centerX, centerY + 10f)
                lineTo(centerX - 18f, centerY + 18f)
                close()
            }
            canvas.drawPath(chevron, vehiclePaint)
        } finally {
            activeSurface.unlockCanvasAndPost(canvas)
        }
    }
}
