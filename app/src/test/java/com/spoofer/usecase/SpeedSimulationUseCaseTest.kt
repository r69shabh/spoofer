package com.spoofer.usecase

import com.google.android.gms.maps.model.LatLng
import com.spoofer.data.DirectionsRepository
import com.spoofer.data.RouteInfo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeedSimulationUseCaseTest {
    private lateinit var directionsRepo: DirectionsRepository
    private lateinit var useCase: SpeedSimulationUseCase

    @Before
    fun setUp() {
        directionsRepo = mockk()
        useCase = SpeedSimulationUseCase(directionsRepo)
    }

    @Test
    fun `distanceBetween calculates correct haversine distance`() {
        // Paris to London approx 344 km
        val paris = LatLng(48.8566, 2.3522)
        val london = LatLng(51.5074, -0.1278)

        val distance = SpeedSimulationUseCase.distanceBetween(paris, london)

        // Assert distance is roughly 343,000 meters (+/- 2000m)
        assertEquals(343000.0, distance, 2000.0)
    }

    @Test
    fun `tick advances position correctly along polyline`() =
        runTest {
            val start = LatLng(0.0, 0.0)
            val end = LatLng(0.0, 1.0) // 1 degree longitude is ~111km at equator

            coEvery { directionsRepo.getRoute(start, end) } returns
                RouteInfo(
                    distanceMeters = 111320,
                    durationSeconds = 3600,
                    polyline = listOf(start, end),
                )

            useCase.initialize(start, end)

            // Move 55,660 meters (approx halfway)
            val result1 = useCase.tick(55660f)
            assertNotNull(result1)
            assertFalse(result1!!.arrived)
            assertEquals(0.5, result1.position.longitude, 0.05)

            // Move another 55,660 meters (should reach end)
            val result2 = useCase.tick(55660f)
            assertNotNull(result2)
            assertTrue(result2!!.arrived)
            assertEquals(1.0, result2.position.longitude, 0.05)
        }
}
