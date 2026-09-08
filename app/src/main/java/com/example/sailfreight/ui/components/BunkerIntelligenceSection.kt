package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.engine.ForecastingEngine
import com.example.sailfreight.data.model.BunkerFuelType
import com.example.sailfreight.data.model.ModelBenchmark
import java.util.Locale

@Composable
fun BunkerIntelligenceSection(
    selectedFuel: BunkerFuelType,
    onSelectFuel: (BunkerFuelType) -> Unit,
    liveMarketData: com.example.sailfreight.data.network.LiveMarketData? = null,
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val quote = ForecastingEngine.getBunkerQuote(selectedFuel, liveMarketData)
    val benchmarks = ForecastingEngine.getModelBenchmarks()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "⛽ Marine Bunker Intelligence & Macro Regressors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (liveMarketData?.isLiveSynced == true) Color(0xFF065F46) else Color(0xFF78350F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (liveMarketData?.isLiveSynced == true) "🟢 LIVE TELEMETRY" else "🟡 ESTIMATED BACKUP",
                                color = if (liveMarketData?.isLiveSynced == true) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (liveMarketData?.isLiveSynced == true) "NYMEX Crude & Forex Synced • ${liveMarketData.lastUpdatedTime}"
                            else "Historical training baseline & macro regressors",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (liveMarketData?.isLiveSynced == true) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
            androidx.compose.material3.IconButton(
                onClick = onSyncClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                    contentDescription = "Refresh live market",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Fuel Type Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BunkerFuelType.entries.forEach { fuel ->
                FilterChip(
                    selected = selectedFuel == fuel,
                    onClick = { onSelectFuel(fuel) },
                    label = { Text(fuel.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Real-Time Quote Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedFuel.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Surface(
                        color = Color(0xFF581C87),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ESTIMATED PROXY",
                            color = Color(0xFFC084FC),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bunker Spot Price (Estimated Proxy)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(
                            text = "~$${"%.2f".format(Locale.US, quote.currentPrice)}/MT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Derived: Real Brent Crude ($${quote.brentCrude}/bbl) × 7.33 + crack spread",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        color = if (quote.change24hPct >= 0) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${if (quote.change24hPct >= 0) "+" else ""}${"%.1f".format(Locale.US, quote.change24hPct)}% (24h)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (quote.change24hPct >= 0) Color(0xFFF87171) else Color(0xFF34D399),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                Text("Multi-Horizon Forecast Projections (ML Estimates)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ForecastTargetBox("24h Est.", "~$${"%.1f".format(Locale.US, quote.horizon24h)}")
                    ForecastTargetBox("7-Day Est.", "~$${"%.1f".format(Locale.US, quote.horizon7d)}")
                    ForecastTargetBox("30-Day Est.", "~$${"%.1f".format(Locale.US, quote.horizon30d)}")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exogenous Macro Regressors
        Text(
            text = "🌐 Exogenous Macro Indicators",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroPill("Brent Crude", "$${quote.brentCrude}", Modifier.weight(1f))
            MacroPill("WTI Crude", "$${quote.wtiCrude}", Modifier.weight(1f))
            MacroPill("USD Index (DXY)", "${quote.usdIndex}", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroPill("Natural Gas", "$${quote.naturalGas}/MMBtu", Modifier.weight(1f))
            MacroPill("Baltic Dry Index", "${quote.freightIndex.toInt()} pts", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Machine Learning Benchmarks
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.QueryStats,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Model Validation & Error Benchmarks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF1F5F9)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                benchmarks.forEachIndexed { idx, bm ->
                    BenchmarkRow(bm, isTop = idx == 2)
                    if (idx < benchmarks.size - 1) {
                        HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastTargetBox(horizon: String, price: String) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(horizon, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text(price, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun BenchmarkRow(benchmark: ModelBenchmark, isTop: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = benchmark.modelName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Medium,
                color = if (isTop) Color(0xFF38BDF8) else Color.White
            )
            if (isTop) {
                Text("⭐ Production Champion Model", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontSize = 9.sp)
            }
        }
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill("MAE", "${benchmark.mae}")
            MetricPill("RMSE", "${benchmark.rmse}")
            MetricPill("MAPE", "${benchmark.mape}%")
        }
    }
}

@Composable
private fun MetricPill(name: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
    }
}
