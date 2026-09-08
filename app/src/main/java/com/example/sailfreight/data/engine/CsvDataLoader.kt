package com.example.sailfreight.data.engine

import android.content.Context
import com.example.sailfreight.data.model.HistoricalFreightRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvDataLoader {

    private var cachedRecords: List<HistoricalFreightRecord>? = null

    suspend fun loadHistoricalData(context: Context): List<HistoricalFreightRecord> = withContext(Dispatchers.IO) {
        cachedRecords?.let { return@withContext it }

        val records = mutableListOf<HistoricalFreightRecord>()
        try {
            val inputStream = context.assets.open("processed_freight_training_data.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine() // skip header
            while (reader.readLine().also { line = it } != null) {
                val tokens = line?.split(",") ?: continue
                if (tokens.size >= 8) {
                    val date = tokens[0].trim()
                    val bunker = tokens[1].toDoubleOrNull() ?: 0.0
                    val dryBulk = tokens[2].toDoubleOrNull() ?: 0.0
                    val usdInr = tokens[3].toDoubleOrNull() ?: 0.0
                    val gold = tokens[4].toDoubleOrNull() ?: 0.0
                    val ma7 = tokens[5].toDoubleOrNull() ?: 0.0
                    val ma30 = tokens[6].toDoubleOrNull() ?: 0.0
                    val returnOil = tokens[7].toDoubleOrNull() ?: 0.0

                    records.add(
                        HistoricalFreightRecord(
                            date = date,
                            bunkerOil = bunker,
                            dryBulkIndex = dryBulk,
                            usdInr = usdInr,
                            commodityGold = gold,
                            bdiMa7 = ma7,
                            bdiMa30 = ma30,
                            oilDailyReturn = returnOil
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedRecords = records
        records
    }

    fun getCachedRecords(): List<HistoricalFreightRecord>? = cachedRecords
}
