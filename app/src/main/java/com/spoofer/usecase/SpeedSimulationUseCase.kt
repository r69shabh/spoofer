package com.spoofer.usecase

import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.DirectionsRepository
import com.spoofer.data.RouteInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SpeedSimulationUseCase @Inject constructor(
    private val directionsRepo: DirectionsRepository,
) {
    private var polylinePoints: List<LatLng> = emptyList()
    private var currentSegmentIndex = 0
    private var progressAlongSegment = 0.0
    var totalDistanceTraveled = 0.0
        private set

    val totalDistance: Double get() = if (polylinePoints.size < 2) 0.0 else {
        var d = 0.0
        for (i in 0 until polylinePoints.lastIndex) {
            d += distanceBetween(polylinePoints[i], polylinePoints[i + 1])
        }
        d
    }

    val remainingDistance: Double get() = totalDistance - totalDistanceTraveled

    suspend fun initialize(origin: LatLng, destination: LatLng): RouteInfo {
        val route = directionsRepo.getRoute(origin, destination)
        polylinePoints = route.polyline
        currentSegmentIndex = 0
        progressAlongSegment = 0.0
        totalDistanceTraveled = 0.0
        return route
    }

    fun tick(speedMps: Float): MovementResult? {
        if (currentSegmentIndex >= polylinePoints.lastIndex) return null

        var remainingMeters = speedMps.toDouble()

        while (remainingMeters > 0 && currentSegmentIndex < polylinePoints.lastIndex) {
            val segStart = polylinePoints[currentSegmentIndex]
            val segEnd = polylinePoints[currentSegmentIndex + 1]
            val segLength = distanceBetween(segStart, segEnd)
            val remainingInSeg = segLength - progressAlongSegment

            if (remainingMeters < remainingInSeg) {
                progressAlongSegment += remainingMeters
                totalDistanceTraveled += remainingMeters
                val fraction = progressAlongSegment / segLength
                val position = interpolate(segStart, segEnd, fraction)
                val bearing = calculateBearing(segStart, segEnd)
                return MovementResult(position, bearing, totalDistanceTraveled, arrived = false)
            } else {
                remainingMeters -= remainingInSeg
                totalDistanceTraveled += remainingInSeg
                currentSegmentIndex++
                progressAlongSegment = 0.0
            }
        }

        return MovementResult(polylinePoints.last(), 0f, totalDistanceTraveled, arrived = true)
    }

    companion object {
        private const val EARTH_RADIUS = 6_371_000.0

        fun distanceBetween(a: LatLng, b: LatLng): Double {
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLng = Math.toRadians(b.longitude - a.longitude)
            val sinLat = sin(dLat / 2)
            val sinLng = sin(dLng / 2)
            val aVal = sinLat * sinLat + cos(Math.toRadians(a.latitude)) *
                cos(Math.toRadians(b.latitude)) * sinLng * sinLng
            return EARTH_RADIUS * 2 * atan2(sqrt(aVal), sqrt(1 - aVal))
        }

        fun calculateBearing(from: LatLng, to: LatLng): Float {
            val fromLat = Math.toRadians(from.latitude)
            val toLat = Math.toRadians(to.latitude)
            val dLng = Math.toRadians(to.longitude - from.longitude)
            val x = sin(dLng) * cos(toLat)
            val y = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng)
            return Math.toDegrees(atan2(x, y)).toFloat()
        }

        private fun interpolate(a: LatLng, b: LatLng, fraction: Double): LatLng {
            return LatLng(
                a.latitude + (b.latitude - a.latitude) * fraction,
                a.longitude + (b.longitude - a.longitude) * fraction,
            )
        }
    }
}

data class MovementResult(
    val position: LatLng,
    val bearing: Float,
    val totalDistance: Double,
    val arrived: Boolean,
)
