package com.example.sailfreight.data.network

import com.example.sailfreight.data.model.PortWeatherForecast
import com.example.sailfreight.data.model.WeatherDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WeatherService {

    suspend fun fetchPortWeather(
        portName: String,
        lat: Double,
        lon: Double
    ): PortWeatherForecast = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
                    "&timezone=auto&forecast_days=5"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Mobile Safari/537.36")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val daily = json.getJSONObject("daily")
                val times = daily.getJSONArray("time")
                val maxTemps = daily.getJSONArray("temperature_2m_max")
                val minTemps = daily.getJSONArray("temperature_2m_min")
                val precipitations = daily.getJSONArray("precipitation_sum")

                val days = mutableListOf<WeatherDay>()
                for (i in 0 until times.length()) {
                    days.add(
                        WeatherDay(
                            date = times.getString(i),
                            maxTemp = maxTemps.optDouble(i, 28.0),
                            minTemp = minTemps.optDouble(i, 20.0),
                            precipitation = precipitations.optDouble(i, 0.0)
                        )
                    )
                }
                return@withContext PortWeatherForecast(portName, days, isLive = true)
            }
        } catch (e: Exception) {
            // Fallback gracefully to offline estimate
        }

        return@withContext createFallbackForecast(portName)
    }

    private fun createFallbackForecast(portName: String): PortWeatherForecast {
        val days = mutableListOf<WeatherDay>()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val (baseMax, baseMin, baseRain) = when {
            portName.contains("Australia") -> Triple(24.5, 15.0, 1.2)
            portName.contains("Indonesia") -> Triple(31.0, 24.0, 5.8)
            portName.contains("Mozambique") -> Triple(27.0, 18.0, 0.5)
            portName.contains("USA") -> Triple(29.0, 21.0, 3.4)
            portName.contains("Russia") -> Triple(18.0, 11.0, 2.0)
            else -> Triple(32.0, 26.0, 4.0) // Indian East Coast ports (Paradip, Vizag, etc.)
        }

        for (i in 0 until 5) {
            days.add(
                WeatherDay(
                    date = fmt.format(cal.time),
                    maxTemp = baseMax + (i % 3 - 1) * 1.5,
                    minTemp = baseMin + (i % 2) * 1.0,
                    precipitation = (baseRain + (i * 1.3)) % 8.0
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return PortWeatherForecast(portName, days, isLive = false)
    }
}
