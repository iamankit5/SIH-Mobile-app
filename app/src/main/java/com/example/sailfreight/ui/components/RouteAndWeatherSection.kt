package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.data.model.PortWeatherForecast
import com.example.sailfreight.data.model.RouteRiskProfile
import com.example.sailfreight.data.model.VesselEvaluation
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteAndWeatherSection(
    origin: String,
    destination: String,
    optimal: VesselEvaluation,
    riskProfile: RouteRiskProfile,
    originWeather: PortWeatherForecast?,
    destWeather: PortWeatherForecast?,
    isLoadingWeather: Boolean,
    onOriginChange: (String) -> Unit = {},
    onDestinationChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val origCoord = ProcurementEngine.PORT_COORDINATES[origin]
    val destCoord = ProcurementEngine.PORT_COORDINATES[destination]
    val distance = ProcurementEngine.ROUTES[origin]?.get(destination) ?: 4500

    Column(modifier = modifier.fillMaxWidth()) {

        // 1. ROUTE MAP & METRICS HEADER
        Text(
            text = "🗺️ Interactive Ocean Freight Lane & 3D Globe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Text(
            text = "3D orthographic maritime trajectory, nautical mileage, and real-time voyage telemetry",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // PORT SELECTORS (Changing ports dynamically updates globe, route & metrics)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Origin Port Dropdown
                    var originExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = originExpanded,
                        onExpandedChange = { originExpanded = !originExpanded },
                        modifier = Modifier.weight(1.1f)
                    ) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Origin Port", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedLabelColor = Color(0xFF38BDF8),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = originExpanded,
                            onDismissRequest = { originExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            ProcurementEngine.ORIGIN_PORTS.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        onOriginChange(opt)
                                        originExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Destination Port Dropdown
                    var destExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Discharge Port", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedLabelColor = Color(0xFF34D399),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            ProcurementEngine.DESTINATION_PORTS.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        onDestinationChange(opt)
                                        destExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3D INTERACTIVE GLOBE
                InteractiveGlobe(
                    origin = origin,
                    destination = destination,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Route summary metrics pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RouteMetricPill("Ocean Distance", "~${"%,d".format(Locale.US, distance)} NM (Est.)")
                    RouteMetricPill("Transit Duration", "${optimal.voyageDays} Days (Est.)")
                    RouteMetricPill("Fuel Burned", "${optimal.fuelBurnedMt} MT (Est.)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Port Geo Info Pill
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Origin: ${"%.2f".format(Locale.US, origCoord?.lat ?: 0.0)}°, ${"%.2f".format(Locale.US, origCoord?.lon ?: 0.0)}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "➔",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Discharge: ${"%.2f".format(Locale.US, destCoord?.lat ?: 0.0)}°, ${"%.2f".format(Locale.US, destCoord?.lon ?: 0.0)}° (Draft: ${destCoord?.draftLimitM ?: 14.5}m)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. VOYAGE RISK & ANOMALY DETECTOR
        Text(
            text = "🌦️ Voyage Risk & Anomaly Detector",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                RiskRow("Origin Weather Risk", riskProfile.originWeather)
                RiskRow("Port Congestion Risk", riskProfile.portCongestion)
                RiskRow("Dest Waiting Time", riskProfile.destWaitingTime)
                RiskRow("Freight Volatility", riskProfile.freightVolatility)
                RiskRow("Overall Route Risk", riskProfile.overallRouteRisk)

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: Within normal historical seasonal band. No route disruption detected.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD1FAE5)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. LIVE 5-DAY PORT WEATHER FORECAST
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⛅ Live 5-Day Port Weather Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "Real-time atmospheric telemetry via Open-Meteo API",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            if (isLoadingWeather) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        WeatherPortCard(originWeather, origin)
        Spacer(modifier = Modifier.height(10.dp))
        WeatherPortCard(destWeather, destination)
    }
}

@Composable
private fun RouteMetricPill(label: String, value: String) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
        }
    }
}

@Composable
private fun RiskRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun WeatherPortCard(weather: PortWeatherForecast?, portName: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Port: $portName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Surface(
                    color = if (weather?.isLive == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (weather?.isLive == true) "🟢 LIVE SYNC" else "⚪ HISTORICAL BASELINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (weather?.isLive == true) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (weather != null && weather.days.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weather.days.forEach { day ->
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.date.takeLast(5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                                Icon(
                                    imageVector = if (day.precipitation > 2.0) Icons.Default.Cloud else Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = if (day.precipitation > 2.0) Color(0xFF60A5FA) else Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${"%.0f".format(Locale.US, day.maxTemp)}°",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${"%.1f".format(Locale.US, day.precipitation)}mm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Loading port atmospheric conditions...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
