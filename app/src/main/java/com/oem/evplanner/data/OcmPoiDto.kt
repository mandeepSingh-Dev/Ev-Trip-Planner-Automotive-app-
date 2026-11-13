/**
 * Package definition for data repositories and network models.
 */
package com.oem.evplanner.data

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.oem.evplanner.model.ChargingStation
import com.oem.evplanner.model.ConnectorStandard
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// --- Retrofit DTO Models ---

/**
 * Data Transfer Object representing an Open Charge Map Point of Interest (POI).
 */
data class OcmPoiDto(
    @SerializedName("ID") val id: Int,
    @SerializedName("AddressInfo") val addressInfo: OcmAddressInfoDto,
    @SerializedName("OperatorInfo") val operatorInfo: OcmOperatorInfoDto? = null,
    @SerializedName("Connections") val connections: List<OcmConnectionDto> = emptyList()
)

/**
 * Data Transfer Object representing address information for an OcmPoiDto.
 */
data class OcmAddressInfoDto(
    @SerializedName("Title") val title: String,
    @SerializedName("AddressLine1") val addressLine1: String? = null,
    @SerializedName("Town") val town: String? = null,
    @SerializedName("Latitude") val latitude: Double,
    @SerializedName("Longitude") val longitude: Double,
    @SerializedName("Distance") val distanceKm: Double? = null
)

/**
 * Data Transfer Object representing charging network operator details.
 */
data class OcmOperatorInfoDto(
    @SerializedName("Title") val title: String? = null
)

/**
 * Data Transfer Object representing individual connector connection specifications.
 */
data class OcmConnectionDto(
    @SerializedName("PowerKW") val powerKw: Double? = null,
    @SerializedName("Quantity") val quantity: Int? = null,
    @SerializedName("ConnectionType") val connectionType: OcmConnectionTypeDto? = null
)

/**
 * Data Transfer Object representing the connection standard type.
 */
data class OcmConnectionTypeDto(
    @SerializedName("Title") val title: String? = null
)

// --- Retrofit Interface ---

/**
 * Retrofit API interface for querying Open Charge Map web services.
 */
interface OpenChargeMapApi {
    /**
     * Fetches nearby charging POIs from Open Charge Map REST endpoint.
     */
    @GET("v3/poi/")
    suspend fun getNearbyPoi(
        @Query("output") output: String = "json",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("distance") radiusKm: Int = 50,
        @Query("distanceunit") distanceUnit: String = "km",
        @Query("maxresults") maxResults: Int = 15,
        @Query("compact") compact: Boolean = false,
        @Query("verbose") verbose: Boolean = false
    ): List<OcmPoiDto>
}

// --- Retrofit Client & Domain Mapper ---

/**
 * Client class that configures Retrofit and OkHttpClient for communicating with Open Charge Map API,
 * mapping raw DTOs into domain [ChargingStation] models.
 */
class OpenChargeMapClient(
    private val apiKey: String = "" // Add free OpenChargeMap API key here
) {
    /** Lazy-initialized Retrofit API service instance. */
    private val api: OpenChargeMapApi by lazy {
        // Configure HTTP logging interceptor for debugging network requests
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Configure OkHttpClient with timeouts, API key header injection, and logging
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        // Build Retrofit instance with Gson converter
        Retrofit.Builder()
            .baseUrl("https://api.openchargemap.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenChargeMapApi::class.java)
    }

    /**
     * Fetches nearby charging stations from Open Charge Map API and maps them to domain models.
     *
     * @param latitude Center latitude coordinate.
     * @param longitude Center longitude coordinate.
     * @param radiusKm Search radius in kilometers.
     * @return List of mapped [ChargingStation] domain objects.
     */
    suspend fun fetchNearbyStations(
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 50
    ): List<ChargingStation> {
        // If API key is blank, return empty list to fallback on local repository data
        if (apiKey.isBlank()) return emptyList()

        return try {
            // Execute network call to retrieve raw POI DTOs
            val response = api.getNearbyPoi(
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm
            )

            // Map each DTO to a domain ChargingStation model
            response.map { poi ->
                val bestConn = poi.connections.maxByOrNull { it.powerKw ?: 0.0 }
                val powerKw = (bestConn?.powerKw ?: 60.0).roundToInt()
                val totalStalls = (bestConn?.quantity ?: 2).coerceAtLeast(1)
                val distKm = ((poi.addressInfo.distanceKm ?: 5.0) * 10.0).roundToInt() / 10.0
                // Determine connector standard based on connection type title
                val connector = when {
                    bestConn?.connectionType?.title?.contains("CHAdeMO", true) == true -> ConnectorStandard.CHADEMO
                    bestConn?.connectionType?.title?.contains("Type 2", true) == true -> ConnectorStandard.TYPE_2
                    else -> ConnectorStandard.CCS2
                }

                ChargingStation(
                    id = "ocm_${poi.id}",
                    name = poi.addressInfo.title,
                    networkOperator = poi.operatorInfo?.title ?: "Public EV Network",
                    address = listOfNotNull(poi.addressInfo.addressLine1, poi.addressInfo.town)
                        .joinToString(", ")
                        .ifBlank { "Highway Corridor" },
                    latitude = poi.addressInfo.latitude,
                    longitude = poi.addressInfo.longitude,
                    distanceKm = distKm,
                    maxPowerKw = powerKw,
                    availableStalls = totalStalls,
                    totalStalls = totalStalls,
                    connector = connector,
                    pricePerKwhInr = if (powerKw >= 150) 21.50 else 18.00,
                    estimatedMinutes = ((distKm / 45.0) * 60.0).roundToInt().coerceAtLeast(5),
                    amenities = listOf("24x7 Access", "CCS2 Fast Charge")
                )
            }
        } catch (e: Exception) {
            // Log warning on network failure and fallback gracefully
            Log.w("OpenChargeMapClient", "Retrofit fetch failed, using cached stations", e)
            emptyList()
        }
    }
}
