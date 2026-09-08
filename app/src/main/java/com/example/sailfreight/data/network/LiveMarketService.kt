package com.example.sailfreight.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

data class LiveMarketData(
    val usdInrRate: Double,
    val bunkerPrice: Double,
    val wtiCrude: Double,
    val brentCrude: Double,
    val naturalGas: Double,
    val usdIndex: Double,
    val isLiveSynced: Boolean,
    val lastUpdatedTime: String
)

object LiveMarketService {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    suspend fun fetchLiveMarketData(fallbackUsdInr: Double = 95.47, fallbackBunker: Double = 611.32): LiveMarketData = withContext(Dispatchers.IO) {
        var liveInr = fallbackUsdInr
        var liveWti = 83.40
        var liveBrent = 87.50
        var liveGas = 2.98
        var liveDxy = 99.16
        var syncSuccess = false

        // 1. Fetch live USD/INR exchange rate
        try {
            val inrUrl = URL("https://open.er-api.com/v6/latest/USD")
            val conn = (inrUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val rates = json.optJSONObject("rates")
                if (rates != null && rates.has("INR")) {
                    liveInr = round(rates.getDouble("INR") * 100.0) / 100.0
                    syncSuccess = true
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            // Secondary exchange rate fallback
            try {
                val altUrl = URL("https://api.frankfurter.app/latest?from=USD&to=INR")
                val conn = (altUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val rates = json.optJSONObject("rates")
                    if (rates != null && rates.has("INR")) {
                        liveInr = round(rates.getDouble("INR") * 100.0) / 100.0
                        syncSuccess = true
                    }
                }
                conn.disconnect()
            } catch (ignored: Exception) {}
        }

        // 2. Fetch live Crude Oil & Macro Regressors from Yahoo Finance Chart API
        try {
            val wtiPrice = fetchYahooPrice("CL=F")
            if (wtiPrice != null && wtiPrice > 20.0) {
                liveWti = wtiPrice
                syncSuccess = true
            }

            val brentPrice = fetchYahooPrice("BZ=F")
            if (brentPrice != null && brentPrice > 20.0) {
                liveBrent = brentPrice
            }

            val gasPrice = fetchYahooPrice("NG=F")
            if (gasPrice != null && gasPrice > 0.5) {
                liveGas = gasPrice
            }

            val dxyPrice = fetchYahooPrice("DX-Y.NYB")
            if (dxyPrice != null && dxyPrice > 70.0) {
                liveDxy = dxyPrice
            }
        } catch (ignored: Exception) {}

        // Calculate bunker price: crude price * 7.33 (matching original procurement engine & yfinance logic)
        val computedBunker = round((liveWti * 7.33) * 100.0) / 100.0
        val finalBunker = if (syncSuccess) computedBunker else fallbackBunker

        val timeFormat = SimpleDateFormat("HH:mm:ss 'IST'", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        LiveMarketData(
            usdInrRate = liveInr,
            bunkerPrice = finalBunker,
            wtiCrude = round(liveWti * 100.0) / 100.0,
            brentCrude = round(liveBrent * 100.0) / 100.0,
            naturalGas = round(liveGas * 100.0) / 100.0,
            usdIndex = round(liveDxy * 100.0) / 100.0,
            isLiveSynced = syncSuccess,
            lastUpdatedTime = timestamp
        )
    }

    private fun fetchYahooPrice(symbol: String): Double? {
        return try {
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?interval=1d&range=1d")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(text)
                val chart = json.getJSONObject("chart")
                val results = chart.getJSONArray("result")
                if (results.length() > 0) {
                    val meta = results.getJSONObject(0).getJSONObject("meta")
                    if (meta.has("regularMarketPrice")) {
                        return meta.getDouble("regularMarketPrice")
                    }
                }
            }
            conn.disconnect()
            null
        } catch (e: Exception) {
            null
        }
    }
}
