package com.example.sailfreight.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.R
import com.example.sailfreight.data.engine.ForecastingEngine
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.ui.AppTab
import com.example.sailfreight.ui.ProcurementViewModel
import com.example.sailfreight.ui.components.ActionAlertBanner
import com.example.sailfreight.ui.components.BunkerIntelligenceSection
import com.example.sailfreight.ui.components.DataProvenanceSection
import com.example.sailfreight.ui.components.ExecutiveSummaryCard
import com.example.sailfreight.ui.components.ForecastChart
import com.example.sailfreight.ui.components.RouteAndWeatherSection
import com.example.sailfreight.ui.components.TimingAnalysisSection
import com.example.sailfreight.ui.components.VesselSuitabilitySection
import com.example.sailfreight.ui.components.WhatIfSimulatorSection
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ProcurementViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dynamic market inputs: uses live Forex & NYMEX or user-entered manual values
    val effectiveBunker = uiState.effectiveBunker
    val effectiveUsdInr = uiState.effectiveUsdInr

    val vesselEvaluations = remember(uiState.cargoQty, uiState.origin, uiState.destination, effectiveBunker) {
        ProcurementEngine.evaluateAllVessels(
            cargoQtyMt = uiState.cargoQty,
            origin = uiState.origin,
            destination = uiState.destination,
            bunkerPrice = effectiveBunker
        )
    }

    val optimalVessel = vesselEvaluations.firstOrNull() ?: return
    val alternativeVessel = vesselEvaluations.getOrNull(1) ?: optimalVessel
    val savingsUsd = (alternativeVessel.costPerMt - optimalVessel.costPerMt) * uiState.cargoQty
    val savingsLakhs = (savingsUsd * effectiveUsdInr) / 100000.0

    // Time-series and timing calculations based on active dataset and live USD/INR
    val forecastSeries = if (uiState.forecastSeries.isNotEmpty()) uiState.forecastSeries
    else remember { ForecastingEngine.generateForecastSeries() }

    val currentSpot = ForecastingEngine.DEFAULT_SPOT_PMT
    val forecast7d = remember(forecastSeries) { ForecastingEngine.getForecastAtDays(forecastSeries, 7) }
    val forecast14d = remember(forecastSeries) { ForecastingEngine.getForecastAtDays(forecastSeries, 14) }
    val forecast30d = remember(forecastSeries) { ForecastingEngine.getForecastAtDays(forecastSeries, 29) }

    val pctChange30d = ((forecast30d - currentSpot) / currentSpot) * 100.0
    val avoidedCost30dLakhs = ((forecast30d - currentSpot) * uiState.cargoQty * effectiveUsdInr) / 100000.0

    val timingOptions = remember(uiState.cargoQty, effectiveUsdInr, currentSpot, forecast7d, forecast14d) {
        ForecastingEngine.calculateTimingAnalysis(
            cargoQty = uiState.cargoQty,
            usdInrRate = effectiveUsdInr,
            currentSpotPmt = currentSpot,
            forecast7dPmt = forecast7d,
            forecast14dPmt = forecast14d
        )
    }

    val routeRiskProfile = remember(uiState.origin, uiState.destination) {
        ProcurementEngine.getRouteRiskProfile(uiState.origin, uiState.destination)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SAIL Freight Intelligence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "USD/INR: ₹${"%.2f".format(Locale.US, effectiveUsdInr)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = " • Bunker: ~$${"%.0f".format(Locale.US, effectiveBunker)} (Est. Proxy)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC084FC),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (uiState.liveMarketData?.isLiveSynced == true) Color(0xFF065F46) else Color(0xFF78350F),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (uiState.liveMarketData?.isLiveSynced == true) "LIVE" else "EST.",
                                    color = if (uiState.liveMarketData?.isLiveSynced == true) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Demo Preset switch
                    Surface(
                        color = if (uiState.judgeMode) Color(0xFF2563EB) else Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .testTag("judge_demo_toggle")
                            .clickable { viewModel.setJudgeMode(!uiState.judgeMode) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Demo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Live Sync Button
                    IconButton(
                        onClick = { viewModel.refreshAll() },
                        modifier = Modifier
                            .testTag("refresh_button")
                            .padding(end = 4.dp)
                    ) {
                        if (uiState.isSyncingMarket) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh live market & weather",
                                tint = Color(0xFFCBD5E1)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        bottomBar = {
            // Persistent Bottom Navigation Bar for smooth, instant tab transitions
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF94A3B8),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    val icon = when (tab) {
                        AppTab.OPTIMIZER -> Icons.Default.DirectionsBoat
                        AppTab.FORECAST -> Icons.Default.ShowChart
                        AppTab.WHAT_IF -> Icons.Default.Tune
                        AppTab.ROUTE -> Icons.Default.Map
                        AppTab.BUNKER -> Icons.Default.LocalGasStation
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF1E293B),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                    )
                }
            }
        },
        containerColor = Color(0xFF0B0F19),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // HERO BANNER WITH OVERLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_maritime_hero),
                    contentDescription = "Bulk Carrier Ship Navigating Deep Ocean",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660F172A),
                                    Color(0xF00B0F19)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "EAST COAST BULK CARGO LOGISTICS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vessel Chartering & Risk Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

                // 1. EXECUTIVE SUMMARY CARD
                ExecutiveSummaryCard(
                    optimal = optimalVessel,
                    cargoQty = uiState.cargoQty,
                    cargoType = uiState.cargoType,
                    origin = uiState.origin,
                    destination = uiState.destination,
                    savingsLakhs = savingsLakhs
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. ACTION ALERT BANNER
                ActionAlertBanner(
                    pctChange30d = pctChange30d,
                    currentSpotPmt = currentSpot,
                    forecast30dPmt = forecast30d,
                    avoidedCostLakhs = avoidedCost30dLakhs
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2.1 DATA PROVENANCE & TRANSPARENCY CARD
                DataProvenanceSection(
                    isLiveSynced = uiState.liveMarketData?.isLiveSynced == true,
                    lastSyncTime = uiState.liveMarketData?.lastUpdatedTime
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. SECONDARY HORIZONTAL TAB STRIP (Quick Touch Bar)
                ScrollableTabRow(
                    selectedTabIndex = uiState.activeTab.ordinal,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                            color = Color(0xFF38BDF8),
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AppTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    text = "${tab.iconLabel} ${tab.title}",
                                    fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (uiState.activeTab == tab) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. SMOOTH TAB CONTENT
                Crossfade(
                    targetState = uiState.activeTab,
                    animationSpec = tween(220),
                    label = "tab_content_transition"
                ) { tab ->
                    when (tab) {
                        AppTab.OPTIMIZER -> {
                            VesselSuitabilitySection(
                                vessels = vesselEvaluations,
                                optimal = optimalVessel,
                                cargoQty = uiState.cargoQty,
                                cargoType = uiState.cargoType,
                                origin = uiState.origin,
                                destination = uiState.destination,
                                isLivePrice = uiState.isLivePrice,
                                customBunkerPrice = uiState.customBunker,
                                liveBunkerPrice = effectiveBunker,
                                customUsdInr = uiState.customUsdInr,
                                liveUsdInr = effectiveUsdInr,
                                savingsLakhs = savingsLakhs,
                                onCargoQtyChange = { viewModel.setCargoQty(it) },
                                onCargoTypeChange = { viewModel.setCargoType(it) },
                                onOriginChange = { viewModel.setOrigin(it) },
                                onDestinationChange = { viewModel.setDestination(it) },
                                onPriceModeChange = { viewModel.setPriceMode(it) },
                                onCustomBunkerChange = { viewModel.setCustomBunker(it) },
                                onCustomUsdInrChange = { viewModel.setCustomUsdInr(it) },
                                isSyncingMarket = uiState.isSyncingMarket,
                                onSyncMarketClick = { viewModel.syncLiveMarketData() },
                                lastSyncTime = uiState.liveMarketData?.lastUpdatedTime
                            )
                        }

                        AppTab.FORECAST -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ForecastChart(
                                    points = forecastSeries,
                                    selectedPoint = uiState.scrubbedPoint,
                                    onPointScrubbed = { viewModel.setScrubbedPoint(it) }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                TimingAnalysisSection(
                                    timingOptions = timingOptions,
                                    currentSpotPmt = currentSpot,
                                    forecast7dPmt = forecast7d,
                                    forecast14dPmt = forecast14d
                                )
                            }
                        }

                        AppTab.WHAT_IF -> {
                            WhatIfSimulatorSection(
                                bunkerShockPct = uiState.bunkerShockPct,
                                freightShockPct = uiState.freightShockPct,
                                testCargoQty = uiState.testCargoQty,
                                baseBunkerPrice = effectiveBunker,
                                origin = uiState.origin,
                                destination = uiState.destination,
                                onBunkerShockChange = { viewModel.setBunkerShock(it) },
                                onFreightShockChange = { viewModel.setFreightShock(it) },
                                onTestCargoQtyChange = { viewModel.setTestCargoQty(it) }
                            )
                        }

                        AppTab.ROUTE -> {
                            RouteAndWeatherSection(
                                origin = uiState.origin,
                                destination = uiState.destination,
                                optimal = optimalVessel,
                                riskProfile = routeRiskProfile,
                                originWeather = uiState.originWeather,
                                destWeather = uiState.destWeather,
                                isLoadingWeather = uiState.isLoadingWeather,
                                onOriginChange = { viewModel.setOrigin(it) },
                                onDestinationChange = { viewModel.setDestination(it) }
                            )
                        }

                        AppTab.BUNKER -> {
                            BunkerIntelligenceSection(
                                selectedFuel = uiState.selectedFuelType,
                                onSelectFuel = { viewModel.setFuelType(it) },
                                liveMarketData = uiState.liveMarketData,
                                isSyncing = uiState.isSyncingMarket,
                                onSyncClick = { viewModel.syncLiveMarketData() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
