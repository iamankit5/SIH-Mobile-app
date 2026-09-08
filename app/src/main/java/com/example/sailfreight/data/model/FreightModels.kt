package com.example.sailfreight.data.model

data class PortCoordinates(
    val name: String,
    val lat: Double,
    val lon: Double,
    val weatherRisk: String = "Low",
    val congestionRisk: String = "Low",
    val waitingDays: Double = 0.0,
    val draftLimitM: Double = 20.0
)

data class VesselSpec(
    val name: String,
    val avgCap: Int,
    val speedKnots: Int,
    val fuelPerDay: Int,
    val dailyHireRate: Int,
    val draftM: Double,
    val availability: String,
    val risk: String
)

data class VesselEvaluation(
    val vessel: String,
    val capacity: String,
    val voyageDays: Double,
    val fuelBurnedMt: Double,
    val totalCostUsd: Double,
    val costPerMt: Double,
    val freightPmt: Double,
    val fuelPmt: Double,
    val portPmt: Double,
    val demurragePmt: Double,
    val utilizationPct: Double,
    val availability: String,
    val risk: String,
    val feasibility: String,
    val isDraftCompliant: Boolean,
    val aiScore: Double
)

data class RouteRiskProfile(
    val originWeather: String,
    val portCongestion: String,
    val destWaitingTime: String,
    val freightVolatility: String,
    val overallRouteRisk: String
)

data class TimingOption(
    val action: String,
    val expectedLanded: String,
    val totalExpenseLakhs: String,
    val varianceText: String,
    val isSaving: Boolean,
    val varianceAmountLakhs: Double
)

data class ForecastPoint(
    val dateStr: String,
    val benchmarkPmt: Double?,
    val forecastPmt: Double?,
    val lowerBoundPmt: Double?,
    val upperBoundPmt: Double?,
    val isHistorical: Boolean
)

data class WeatherDay(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitation: Double
)

data class PortWeatherForecast(
    val portName: String,
    val days: List<WeatherDay>,
    val isLive: Boolean = true
)

enum class BunkerFuelType(val label: String, val typicalPrice: Double) {
    VLSFO("Very Low Sulphur Fuel Oil (VLSFO)", 611.32),
    HSFO("High Sulphur Fuel Oil (HSFO)", 465.50),
    MGO("Marine Gas Oil (MGO)", 785.00)
}

data class BunkerFuelQuote(
    val fuelType: BunkerFuelType,
    val currentPrice: Double,
    val change24hPct: Double,
    val horizon24h: Double,
    val horizon7d: Double,
    val horizon30d: Double,
    val brentCrude: Double,
    val wtiCrude: Double,
    val usdIndex: Double,
    val naturalGas: Double,
    val freightIndex: Double
)


data class MacroRegressors(
    val brentCrude: Double,
    val wtiCrude: Double,
    val usdIndex: Double,
    val naturalGas: Double,
    val freightIndex: Double
)

data class ModelBenchmark(
    val modelName: String,
    val mae: Double,
    val rmse: Double,
    val mape: Double
)
