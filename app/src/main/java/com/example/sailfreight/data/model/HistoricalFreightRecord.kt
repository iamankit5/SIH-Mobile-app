package com.example.sailfreight.data.model

data class HistoricalFreightRecord(
    val date: String,
    val bunkerOil: Double,
    val dryBulkIndex: Double,
    val usdInr: Double,
    val commodityGold: Double,
    val bdiMa7: Double,
    val bdiMa30: Double,
    val oilDailyReturn: Double
)
