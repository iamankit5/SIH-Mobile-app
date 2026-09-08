package com.example.sailfreight.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sailfreight.data.engine.CsvDataLoader
import com.example.sailfreight.data.engine.ForecastingEngine
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.data.model.BunkerFuelType
import com.example.sailfreight.data.model.ForecastPoint
import com.example.sailfreight.data.model.HistoricalFreightRecord
import com.example.sailfreight.data.model.PortWeatherForecast
import com.example.sailfreight.data.network.LiveMarketData
import com.example.sailfreight.data.network.LiveMarketService
import com.example.sailfreight.data.network.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val iconLabel: String) {
    OPTIMIZER("Optimizer", "🚢"),
    FORECAST("Forecast", "📈"),
    WHAT_IF("Simulate", "🧪"),
    ROUTE("Route", "🗺️"),
    BUNKER("Bunker", "⛽")
}

data class UiState(
    val origin: String = "Australia (Newcastle)",
    val destination: String = "Paradip",
    val cargoType: String = "Coking Coal",
    val cargoQty: Int = 75000,
    val isLivePrice: Boolean = true,
    val customBunker: Double = 650.0,
    val customUsdInr: Double = 94.52,
    val judgeMode: Boolean = true,
    val bunkerShockPct: Int = 0,
    val freightShockPct: Int = 0,
    val testCargoQty: Int = 75000,
    val selectedFuelType: BunkerFuelType = BunkerFuelType.VLSFO,
    val activeTab: AppTab = AppTab.OPTIMIZER,
    val scrubbedPoint: ForecastPoint? = null,
    val originWeather: PortWeatherForecast? = null,
    val destWeather: PortWeatherForecast? = null,
    val isLoadingWeather: Boolean = false,
    val liveMarketData: LiveMarketData? = null,
    val isSyncingMarket: Boolean = false,
    val historicalRecords: List<HistoricalFreightRecord> = emptyList(),
    val forecastSeries: List<ForecastPoint> = emptyList()
) {
    val effectiveUsdInr: Double
        get() = if (isLivePrice) (liveMarketData?.usdInrRate ?: ForecastingEngine.DEFAULT_FALLBACK_USD_INR) else customUsdInr

    val effectiveBunker: Double
        get() = if (isLivePrice) (liveMarketData?.bunkerPrice ?: ForecastingEngine.DEFAULT_FALLBACK_BUNKER) else customBunker
}

class ProcurementViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 1. Load real historical CSV data from assets
            val records = CsvDataLoader.loadHistoricalData(getApplication<Application>().applicationContext)
            val series = ForecastingEngine.generateForecastSeries(records)
            _uiState.value = _uiState.value.copy(
                historicalRecords = records,
                forecastSeries = series
            )

            // 2. Fetch live market currency & commodity prices
            syncLiveMarketData()

            // 3. Load live weather
            loadWeather()
        }
    }

    fun syncLiveMarketData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingMarket = true)
            val fallbackInr = _uiState.value.historicalRecords.lastOrNull()?.usdInr ?: ForecastingEngine.DEFAULT_FALLBACK_USD_INR
            val fallbackBunker = (_uiState.value.historicalRecords.lastOrNull()?.bunkerOil ?: 83.40) * 7.33

            val marketData = LiveMarketService.fetchLiveMarketData(
                fallbackUsdInr = fallbackInr,
                fallbackBunker = fallbackBunker
            )

            _uiState.value = _uiState.value.copy(
                liveMarketData = marketData,
                isSyncingMarket = false
            )
        }
    }

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setJudgeMode(enabled: Boolean) {
        if (enabled) {
            _uiState.value = _uiState.value.copy(
                judgeMode = true,
                origin = "Australia (Newcastle)",
                destination = "Paradip",
                cargoQty = 75000,
                cargoType = "Coking Coal"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                judgeMode = false,
                origin = ProcurementEngine.ORIGIN_PORTS[0],
                destination = "Paradip",
                cargoQty = 55000
            )
        }
        loadWeather()
    }

    fun setOrigin(newOrigin: String) {
        _uiState.value = _uiState.value.copy(origin = newOrigin)
        loadWeather()
    }

    fun setDestination(newDest: String) {
        _uiState.value = _uiState.value.copy(destination = newDest)
        loadWeather()
    }

    fun setCargoType(type: String) {
        _uiState.value = _uiState.value.copy(cargoType = type)
    }

    fun setCargoQty(qty: Int) {
        _uiState.value = _uiState.value.copy(
            cargoQty = qty,
            testCargoQty = qty
        )
    }

    fun setPriceMode(isLive: Boolean) {
        _uiState.value = _uiState.value.copy(isLivePrice = isLive)
    }

    fun setCustomBunker(price: Double) {
        _uiState.value = _uiState.value.copy(customBunker = price)
    }

    fun setCustomUsdInr(rate: Double) {
        _uiState.value = _uiState.value.copy(customUsdInr = rate)
    }

    fun setBunkerShock(shock: Int) {
        _uiState.value = _uiState.value.copy(bunkerShockPct = shock)
    }

    fun setFreightShock(shock: Int) {
        _uiState.value = _uiState.value.copy(freightShockPct = shock)
    }

    fun setTestCargoQty(qty: Int) {
        _uiState.value = _uiState.value.copy(testCargoQty = qty)
    }

    fun setFuelType(type: BunkerFuelType) {
        _uiState.value = _uiState.value.copy(selectedFuelType = type)
    }

    fun setScrubbedPoint(point: ForecastPoint?) {
        _uiState.value = _uiState.value.copy(scrubbedPoint = point)
    }

    fun refreshAll() {
        syncLiveMarketData()
        loadWeather()
    }

    private fun loadWeather() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingWeather = true)
            val orig = _uiState.value.origin
            val dest = _uiState.value.destination

            val origCoord = ProcurementEngine.PORT_COORDINATES[orig]
            val destCoord = ProcurementEngine.PORT_COORDINATES[dest]

            val origW = if (origCoord != null) {
                WeatherService.fetchPortWeather(orig, origCoord.lat, origCoord.lon)
            } else null

            val destW = if (destCoord != null) {
                WeatherService.fetchPortWeather(dest, destCoord.lat, destCoord.lon)
            } else null

            _uiState.value = _uiState.value.copy(
                originWeather = origW,
                destWeather = destW,
                isLoadingWeather = false
            )
        }
    }
}
