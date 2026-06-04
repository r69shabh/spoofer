package com.spoofer.usecase

import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.DirectionsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class ETAMatchUseCase @Inject constructor(
    private val directionsRepo: DirectionsRepository,
) {

    suspend fun findMatchedLocation(
        realOrigin: LatLng,
        destination: LatLng,
    ): ETAMatchResult {
        val route = directionsRepo.getRoute(realOrigin, destination)
        val realETA = route.durationSeconds

        if (route.polyline.size < 3) {
            return ETAMatchResult(
                matchedLocation = realOrigin,
                matchedETA = realETA,
                realETA = realETA,
                confidence = 1.0,
                distanceSaved = 0.0,
                matchedPolyline = route.polyline,
            )
        }

        val samplePoints = sampleEvenly(route.polyline, numSamples = 12)

        var bestMatch: LatLng = realOrigin
        var bestDiff = Int.MAX_VALUE

        for (point in samplePoints) {
            val eta = try {
                directionsRepo.getETA(point, destination)
            } catch (_: Exception) {
                continue
            }
            val diff = abs(eta - realETA)
            if (diff < bestDiff) {
                bestDiff = diff
                bestMatch = point
            }
        }

        val matchedETA = try {
            directionsRepo.getETA(bestMatch, destination)
        } catch (_: Exception) {
            realETA
        }

        val confidence = if (realETA > 0) {
            (1.0 - abs(matchedETA - realETA).toDouble() / realETA).coerceIn(0.0, 1.0)
        } else 1.0

        val matchedRoute = try {
            directionsRepo.getRoute(bestMatch, destination)
        } catch (_: Exception) {
            route
        }

        var distanceSaved = 0.0
        var found = false
        for (i in 0 until route.polyline.lastIndex) {
            if (!found) {
                distanceSaved += SpeedSimulationUseCase.distanceBetween(
                    route.polyline[i], route.polyline[i + 1]
                )
                if (abs(route.polyline[i].latitude - bestMatch.latitude) < 0.00001 &&
                    abs(route.polyline[i].longitude - bestMatch.longitude) < 0.00001
                ) {
                    found = true
                }
            }
        }

        return ETAMatchResult(
            matchedLocation = bestMatch,
            matchedETA = matchedETA,
            realETA = realETA,
            confidence = confidence,
            distanceSaved = distanceSaved,
            matchedPolyline = matchedRoute.polyline,
        )
    }

    private fun sampleEvenly(polyline: List<LatLng>, numSamples: Int): List<LatLng> {
        if (polyline.size <= numSamples) return polyline

        var totalLength = 0.0
        val segmentLengths = DoubleArray(polyline.size - 1)
        for (i in 0 until polyline.lastIndex) {
            segmentLengths[i] = SpeedSimulationUseCase.distanceBetween(polyline[i], polyline[i + 1])
            totalLength += segmentLengths[i]
        }

        val step = totalLength / (numSamples + 1)
        val samples = mutableListOf<LatLng>()
        var accumulated = 0.0
        var segIdx = 0

        for (s in 1..numSamples) {
            val target = step * s
            while (segIdx < segmentLengths.size && accumulated + segmentLengths[segIdx] < target) {
                accumulated += segmentLengths[segIdx]
                segIdx++
            }
            if (segIdx >= segmentLengths.size) break

            val remaining = target - accumulated
            val fraction = (remaining / segmentLengths[segIdx]).coerceIn(0.0, 1.0)
            val lat = polyline[segIdx].latitude +
                (polyline[segIdx + 1].latitude - polyline[segIdx].latitude) * fraction
            val lng = polyline[segIdx].longitude +
                (polyline[segIdx + 1].longitude - polyline[segIdx].longitude) * fraction
            samples.add(LatLng(lat, lng))
        }

        return samples
    }
}

data class ETAMatchResult(
    val matchedLocation: LatLng,
    val matchedETA: Int,
    val realETA: Int,
    val confidence: Double,
    val distanceSaved: Double,
    val matchedPolyline: List<LatLng>,
)
