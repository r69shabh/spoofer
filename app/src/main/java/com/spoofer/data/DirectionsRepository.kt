package com.spoofer.data

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.spoofer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionsRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val baseUrl = "https://router.project-osrm.org"

    private val cache = object : LinkedHashMap<String, RouteInfo>(20, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RouteInfo>): Boolean {
            return size > 20
        }
    }

    private var lastCallTime = 0L

    suspend fun getRoute(origin: LatLng, destination: LatLng): RouteInfo {
        val cacheKey = "${origin.latitude},${origin.longitude}-${destination.latitude},${destination.longitude}"
        cache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/route/v1/driving/" +
                "${origin.longitude},${origin.latitude};" +
                "${destination.longitude},${destination.latitude}" +
                "?overview=full&geometries=polyline&alternatives=false"

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response from OSRM")

            if (!response.isSuccessful) {
                throw IOException("OSRM error ${response.code}")
            }

            val json = Gson().fromJson(body, JsonObject::class.java)
            if (json.get("code")?.asString != "Ok") {
                throw IOException("OSRM error: ${json.get("code")?.asString}")
            }

            val route = json.getAsJsonArray("routes")[0].asJsonObject
            val geometry = route.get("geometry").asString
            val duration = route.get("duration").asDouble.toInt()
            val distance = route.get("distance").asDouble.toInt()

            val polyline = decodePolyline(geometry)

            val routeInfo = RouteInfo(
                polyline = polyline,
                durationSeconds = duration,
                distanceMeters = distance,
            )

            synchronized(cache) { cache[cacheKey] = routeInfo }
            routeInfo
        }
    }

    suspend fun getETA(origin: LatLng, destination: LatLng): Int {
        val now = System.currentTimeMillis()
        val elapsed = now - lastCallTime
        if (elapsed < 1000L) {
            delay(1000L - elapsed)
        }
        lastCallTime = System.currentTimeMillis()
        return getRoute(origin, destination).durationSeconds
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1F) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
            lng += dlng

            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }
}

data class RouteInfo(
    val polyline: List<LatLng>,
    val durationSeconds: Int,
    val distanceMeters: Int,
)
