package com.example.sailfreight.data.engine

import com.example.sailfreight.data.model.PortCoordinates
import com.example.sailfreight.data.model.RouteRiskProfile
import com.example.sailfreight.data.model.VesselEvaluation
import com.example.sailfreight.data.model.VesselSpec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object ProcurementEngine {

    val ORIGIN_PORTS = listOf(
        "Australia (Newcastle)",
        "Australia (Hay Point)",
        "Indonesia (Samarinda)",
        "Mozambique (Maputo)",
        "USA (New Orleans)",
        "Russia (Vladivostok)"
    )

    val DESTINATION_PORTS = listOf(
        "Paradip",
        "Haldia",
        "Vizag",
        "Dhamra"
    )

    val CARGO_MATERIALS = listOf(
        "Coking Coal",
        "Thermal Coal",
        "Iron Ore Pellets",
        "Limestone"
    )

    val PORT_COORDINATES: Map<String, PortCoordinates> = mapOf(
        "Australia (Newcastle)" to PortCoordinates("Australia (Newcastle)", -32.9283, 151.7817, weatherRisk = "Low", congestionRisk = "Medium"),
        "Australia (Hay Point)" to PortCoordinates("Australia (Hay Point)", -21.2858, 149.2997, weatherRisk = "Low", congestionRisk = "Low"),
        "Indonesia (Samarinda)" to PortCoordinates("Indonesia (Samarinda)", -0.5021, 117.1537, weatherRisk = "Medium", congestionRisk = "Medium"),
        "Mozambique (Maputo)" to PortCoordinates("Mozambique (Maputo)", -25.9692, 32.5732, weatherRisk = "Low", congestionRisk = "Low"),
        "USA (New Orleans)" to PortCoordinates("USA (New Orleans)", 29.9511, -90.0715, weatherRisk = "Medium", congestionRisk = "High"),
        "Russia (Vladivostok)" to PortCoordinates("Russia (Vladivostok)", 43.1155, 131.8855, weatherRisk = "High", congestionRisk = "Low"),
        "Paradip" to PortCoordinates("Paradip", 20.2644, 86.6083, waitingDays = 2.5, draftLimitM = 14.5),
        "Haldia" to PortCoordinates("Haldia", 22.0257, 88.0583, waitingDays = 3.8, draftLimitM = 8.5),
        "Vizag" to PortCoordinates("Vizag", 17.6868, 83.2185, waitingDays = 2.0, draftLimitM = 14.5),
        "Dhamra" to PortCoordinates("Dhamra", 20.8317, 86.9583, waitingDays = 1.8, draftLimitM = 18.0)
    )

    val ROUTES: Map<String, Map<String, Int>> = mapOf(
        "Australia (Newcastle)" to mapOf("Paradip" to 4500, "Haldia" to 4650, "Vizag" to 4400, "Dhamra" to 4550),
        "Australia (Hay Point)" to mapOf("Paradip" to 4300, "Haldia" to 4450, "Vizag" to 4200, "Dhamra" to 4350),
        "Indonesia (Samarinda)" to mapOf("Paradip" to 2200, "Haldia" to 2350, "Vizag" to 2100, "Dhamra" to 2250),
        "Mozambique (Maputo)" to mapOf("Paradip" to 3900, "Haldia" to 4050, "Vizag" to 3750, "Dhamra" to 3950),
        "USA (New Orleans)" to mapOf("Paradip" to 9800, "Haldia" to 9950, "Vizag" to 9650, "Dhamra" to 9850),
        "Russia (Vladivostok)" to mapOf("Paradip" to 4800, "Haldia" to 4950, "Vizag" to 4650, "Dhamra" to 4850)
    )

    val VESSEL_SPECS: Map<String, VesselSpec> = mapOf(
        "Handysize" to VesselSpec("Handysize", avgCap = 35000, speedKnots = 13, fuelPerDay = 20, dailyHireRate = 11500, draftM = 10.0, availability = "4 vessels", risk = "🟡 Medium"),
        "Supramax" to VesselSpec("Supramax", avgCap = 55000, speedKnots = 14, fuelPerDay = 26, dailyHireRate = 14500, draftM = 11.5, availability = "3 vessels", risk = "🟡 Medium"),
        "Panamax" to VesselSpec("Panamax", avgCap = 75000, speedKnots = 14, fuelPerDay = 32, dailyHireRate = 17000, draftM = 13.5, availability = "8 vessels", risk = "🟢 Low"),
        "Capesize" to VesselSpec("Capesize", avgCap = 150000, speedKnots = 13, fuelPerDay = 46, dailyHireRate = 24000, draftM = 18.0, availability = "1 vessel", risk = "🔴 High")
    )

    fun evaluateAllVessels(
        cargoQtyMt: Int,
        origin: String,
        destination: String,
        bunkerPrice: Double,
        freightMultiplier: Double = 1.0
    ): List<VesselEvaluation> {
        val distance = ROUTES[origin]?.get(destination) ?: 4500
        val destDraft = PORT_COORDINATES[destination]?.draftLimitM ?: 14.5
        val evaluations = mutableListOf<VesselEvaluation>()

        val safeQty = if (cargoQtyMt <= 0) 50000 else cargoQtyMt

        for ((vName, spec) in VESSEL_SPECS) {
            val dailyDist = spec.speedKnots * 24.0
            val voyageDays = distance / dailyDist

            val fuelCost = voyageDays * spec.fuelPerDay * bunkerPrice
            val charterCost = voyageDays * spec.dailyHireRate * freightMultiplier
            val portCharges = 35000.0
            val demurrageRisk = when (spec.risk) {
                "🔴 High" -> 12000.0
                "🟡 Medium" -> 6000.0
                else -> 2500.0
            }
            val canalFees = if (origin.contains("USA")) 8500.0 else 2000.0

            val totalVoyageCost = fuelCost + charterCost + portCharges + demurrageRisk + canalFees
            val costPerMt = totalVoyageCost / safeQty

            val utilization = (safeQty.toDouble() / spec.avgCap) * 100.0
            val utilPenalty = if (utilization <= 120.0) {
                abs(100.0 - utilization) * 0.5
            } else {
                (utilization - 100.0) * 1.8
            }

            // Physical Constraint Check: Draft Limit
            val isDraftCompliant = spec.draftM <= destDraft
            val draftPenalty = if (!isDraftCompliant) 50.0 else 0.0
            val feasibility = if (isDraftCompliant) {
                "🟢 Cleared"
            } else {
                "🔴 Draft Exceeds Limit (${spec.draftM}m > ${destDraft}m)"
            }

            val baseScore = 96.0 - utilPenalty - (costPerMt * 0.05) - draftPenalty
            val aiScore = max(0.0, min(99.0, round(baseScore * 10.0) / 10.0))

            evaluations.add(
                VesselEvaluation(
                    vessel = vName,
                    capacity = "${spec.avgCap / 1000}K MT",
                    voyageDays = round(voyageDays * 10.0) / 10.0,
                    fuelBurnedMt = round(voyageDays * spec.fuelPerDay * 10.0) / 10.0,
                    totalCostUsd = round(totalVoyageCost),
                    costPerMt = round(costPerMt * 100.0) / 100.0,
                    freightPmt = round((charterCost / safeQty) * 100.0) / 100.0,
                    fuelPmt = round((fuelCost / safeQty) * 100.0) / 100.0,
                    portPmt = round(((portCharges + canalFees) / safeQty) * 100.0) / 100.0,
                    demurragePmt = round((demurrageRisk / safeQty) * 100.0) / 100.0,
                    utilizationPct = round(min(100.0, utilization) * 10.0) / 10.0,
                    availability = spec.availability,
                    risk = spec.risk,
                    feasibility = feasibility,
                    isDraftCompliant = isDraftCompliant,
                    aiScore = aiScore
                )
            )
        }

        return evaluations.sortedByDescending { it.aiScore }
    }

    fun getRouteRiskProfile(origin: String, destination: String): RouteRiskProfile {
        val orig = PORT_COORDINATES[origin] ?: PortCoordinates(origin, 0.0, 0.0)
        val dest = PORT_COORDINATES[destination] ?: PortCoordinates(destination, 0.0, 0.0)

        return RouteRiskProfile(
            originWeather = orig.weatherRisk,
            portCongestion = orig.congestionRisk,
            destWaitingTime = "${dest.waitingDays} Days",
            freightVolatility = "Medium",
            overallRouteRisk = "🟡 LOW–MEDIUM"
        )
    }
}
