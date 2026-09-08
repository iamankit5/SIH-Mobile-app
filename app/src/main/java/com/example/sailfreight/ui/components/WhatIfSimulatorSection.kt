package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.data.model.VesselEvaluation
import java.util.Locale

@Composable
fun WhatIfSimulatorSection(
    bunkerShockPct: Int,
    freightShockPct: Int,
    testCargoQty: Int,
    baseBunkerPrice: Double,
    origin: String,
    destination: String,
    onBunkerShockChange: (Int) -> Unit,
    onFreightShockChange: (Int) -> Unit,
    onTestCargoQtyChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val simBunker = baseBunkerPrice * (1.0 + (bunkerShockPct / 100.0))
    val simMultiplier = 1.0 + (freightShockPct / 100.0)

    val simEvaluations = ProcurementEngine.evaluateAllVessels(
        cargoQtyMt = testCargoQty,
        origin = origin,
        destination = destination,
        bunkerPrice = simBunker,
        freightMultiplier = simMultiplier
    )
    val simOptimal = simEvaluations.firstOrNull() ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text(
                    text = "🧪 What-If Stress-Test Scenario Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "Evaluate fleet resilience against sudden bunker spikes or charter index shocks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Bunker Shock Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bunker Price Shift",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${if (bunkerShockPct > 0) "+" else ""}$bunkerShockPct% ($${"%.1f".format(Locale.US, simBunker)}/MT)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bunkerShockPct > 0) Color(0xFFEF4444) else if (bunkerShockPct < 0) Color(0xFF10B981) else Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = bunkerShockPct.toFloat(),
                    onValueChange = { onBunkerShockChange(it.toInt()) },
                    valueRange = -30f..50f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Freight Market Shift Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Freight Market Shift",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${if (freightShockPct > 0) "+" else ""}$freightShockPct% (${"%.2f".format(Locale.US, simMultiplier)}x multiplier)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (freightShockPct > 0) Color(0xFFEF4444) else if (freightShockPct < 0) Color(0xFF10B981) else Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = freightShockPct.toFloat(),
                    onValueChange = { onFreightShockChange(it.toInt()) },
                    valueRange = -20f..40f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test Quantity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Test Cargo Quantity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "${"%,d".format(Locale.US, testCargoQty)} MT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = testCargoQty.toFloat(),
                    onValueChange = { onTestCargoQtyChange(it.toInt()) },
                    valueRange = 25000f..150000f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Simulation Outcome Alert
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📊 Simulation Outcome",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Under a ${bunkerShockPct}% bunker price shift and ${freightShockPct}% market rate shift, ${simOptimal.vessel} remains Rank #1 at $${"%.2f".format(Locale.US, simOptimal.costPerMt)}/MT landed cost (AI Score: ${simOptimal.aiScore}/100, Capacity Fit: ${simOptimal.utilizationPct}%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fleet Resilience Under Simulated Shock",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Spacer(modifier = Modifier.height(8.dp))

        simEvaluations.forEachIndexed { idx, v ->
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (idx == 0) Color(0xFF3B82F6) else Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "#${idx + 1} ${v.vessel} (${v.capacity})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Landed: $${"%.2f".format(Locale.US, v.costPerMt)}/MT • Utilization: ${v.utilizationPct}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Surface(
                        color = if (v.aiScore >= 80) Color(0xFF10B981) else if (v.aiScore >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${v.aiScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
