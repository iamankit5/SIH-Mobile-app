package com.example.sailfreight

import com.example.sailfreight.data.engine.ForecastingEngine
import com.example.sailfreight.data.engine.ProcurementEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcurementEngineTest {

    @Test
    fun testVesselEvaluation_NewcastleToParadip_75kMt() {
        val evaluations = ProcurementEngine.evaluateAllVessels(
            cargoQtyMt = 75000,
            origin = "Australia (Newcastle)",
            destination = "Paradip",
            bunkerPrice = 611.32
        )

        assertEquals(4, evaluations.size)

        val optimal = evaluations.first()
        assertEquals("Panamax", optimal.vessel)
        assertTrue("Panamax should be draft compliant in Paradip", optimal.isDraftCompliant)
        assertEquals(100.0, optimal.utilizationPct, 0.1)
        assertTrue(optimal.aiScore > 75.0)
    }

    @Test
    fun testDraftConstraint_HaldiaShallowPort() {
        val evaluations = ProcurementEngine.evaluateAllVessels(
            cargoQtyMt = 75000,
            origin = "Australia (Newcastle)",
            destination = "Haldia", // Haldia draft limit is 8.5m
            bunkerPrice = 611.32
        )

        val capesize = evaluations.first { it.vessel == "Capesize" }
        assertFalse("Capesize draft (18m) exceeds Haldia limit (8.5m)", capesize.isDraftCompliant)
        assertTrue(capesize.feasibility.contains("Draft Exceeds Limit"))

        val panamax = evaluations.first { it.vessel == "Panamax" }
        assertFalse("Panamax draft (13.5m) exceeds Haldia limit (8.5m)", panamax.isDraftCompliant)
    }

    @Test
    fun testForecastingEngine_SeriesGeneration() {
        val series = ForecastingEngine.generateForecastSeries()
        assertTrue(series.size >= 75)

        val futurePoints = series.filter { !it.isHistorical && it.forecastPmt != null }
        assertTrue(futurePoints.isNotEmpty())

        futurePoints.forEach { pt ->
            assertNotNull(pt.lowerBoundPmt)
            assertNotNull(pt.upperBoundPmt)
            assertTrue(pt.lowerBoundPmt!! <= pt.forecastPmt!!)
            assertTrue(pt.forecastPmt!! <= pt.upperBoundPmt!!)
        }
    }

    @Test
    fun testTimingAnalysis_Calculation() {
        val timing = ForecastingEngine.calculateTimingAnalysis(
            cargoQty = 75000,
            usdInrRate = 94.52,
            currentSpotPmt = 3.56,
            forecast7dPmt = 3.75,
            forecast14dPmt = 3.90
        )

        assertEquals(3, timing.size)
        assertEquals("Charter Today", timing[0].action)
        assertEquals("— Baseline —", timing[0].varianceText)
        assertFalse(timing[1].isSaving)
        assertTrue(timing[1].varianceText.contains("Added Cost"))
    }

    @Test
    fun testGlobeRouteCoordinatesAndDistances() {
        for (origin in ProcurementEngine.ORIGIN_PORTS) {
            val origCoord = ProcurementEngine.PORT_COORDINATES[origin]
            assertNotNull("Origin $origin must have geographic coordinates", origCoord)
            assertTrue("Latitude must be valid", origCoord!!.lat in -90.0..90.0)
            assertTrue("Longitude must be valid", origCoord.lon in -180.0..180.0)

            for (dest in ProcurementEngine.DESTINATION_PORTS) {
                val destCoord = ProcurementEngine.PORT_COORDINATES[dest]
                assertNotNull("Destination $dest must have geographic coordinates", destCoord)
                val distance = ProcurementEngine.ROUTES[origin]?.get(dest)
                assertNotNull("Route from $origin to $dest must exist", distance)
                assertTrue("Distance must be greater than 0", distance!! > 0)
            }
        }
    }
}
