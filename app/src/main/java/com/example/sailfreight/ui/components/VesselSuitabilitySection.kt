package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.engine.ProcurementEngine
import com.example.sailfreight.data.model.VesselEvaluation
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VesselSuitabilitySection(
    vessels: List<VesselEvaluation>,
    optimal: VesselEvaluation,
    cargoQty: Int,
    cargoType: String,
    origin: String,
    destination: String,
    isLivePrice: Boolean,
    customBunkerPrice: Double,
    liveBunkerPrice: Double,
    customUsdInr: Double,
    liveUsdInr: Double,
    savingsLakhs: Double,
    onCargoQtyChange: (Int) -> Unit,
    onCargoTypeChange: (String) -> Unit,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onPriceModeChange: (Boolean) -> Unit,
    onCustomBunkerChange: (Double) -> Unit,
    onCustomUsdInrChange: (Double) -> Unit,
    isSyncingMarket: Boolean = false,
    onSyncMarketClick: () -> Unit = {},
    lastSyncTime: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // 1. PROCUREMENT CONTROLS CARD
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛠️ Cargo & Ocean Route Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "Configure voyage requirements, port lane, and bunker fuel pricing",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Material Type Chips
                Text(
                    text = "Material Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProcurementEngine.CARGO_MATERIALS.forEach { material ->
                        FilterChip(
                            selected = cargoType == material,
                            onClick = { onCargoTypeChange(material) },
                            label = { Text(material) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cargo Quantity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cargo Quantity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "${"%,d".format(Locale.US, cargoQty)} MT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Slider(
                    value = cargoQty.toFloat(),
                    onValueChange = { onCargoQtyChange(it.toInt()) },
                    valueRange = 25000f..150000f,
                    steps = 24, // 5000 step
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Route Selectors (Origin & Destination)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Origin Dropdown
                    var originExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = originExpanded,
                        onExpandedChange = { originExpanded = !originExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Origin Port", fontSize = 12.sp) },
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

                    // Destination Dropdown
                    var destExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = destination,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Destination Port", fontSize = 12.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
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

                Spacer(modifier = Modifier.height(14.dp))

                // Fuel Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fuel Pricing Source",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Row {
                        FilterChip(
                            selected = isLivePrice,
                            onClick = { onPriceModeChange(true) },
                            label = { Text("🟢 Live Market", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = !isLivePrice,
                            onClick = { onPriceModeChange(false) },
                            label = { Text("⚙️ Manual", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                if (!isLivePrice) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customBunkerPrice.toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { onCustomBunkerChange(it) }
                            },
                            label = { Text("Bunker ($/MT)", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customUsdInr.toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { onCustomUsdInrChange(it) }
                            },
                            label = { Text("USD/INR (₹)", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "⚙️ Manual Mode: Set custom bunker oil and foreign exchange parameters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF065F46),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isSyncingMarket) "🔄 SYNCING..." else "🟢 LIVE TELEMETRY",
                                            color = Color(0xFF34D399),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (lastSyncTime != null) {
                                        Text(
                                            text = " • $lastSyncTime",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "USD/INR: ₹${"%.2f".format(Locale.US, liveUsdInr)} (Live Forex API)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF1F5F9)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Bunker Fuel: ~$${"%.2f".format(Locale.US, liveBunkerPrice)}/MT",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFF581C87),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ESTIMATED PROXY",
                                            color = Color(0xFFC084FC),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Derived: NYMEX WTI Crude × 7.33 conversion (not direct terminal quote)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = onSyncMarketClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync live market",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. HERO OPTIMAL VESSEL SELECTION
        Card(
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBoat,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🏆 RANK #1 OPTIMAL: ${optimal.vessel.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                        }
                        Surface(
                            color = Color(0xFF2563EB),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "AI SCORE: ${optimal.aiScore}/100",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Key Metric Grid
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            MetricText("📦 Capacity Fit", "${optimal.utilizationPct}%")
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricText("💵 Est. Landed Cost", "$${"%.2f".format(Locale.US, optimal.costPerMt)}/MT")
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricText("⚓ Est. Total Cost", "$${"%,.0f".format(Locale.US, optimal.totalCostUsd)}")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            MetricText("⏱️ Est. Transit Time", "${optimal.voyageDays} Days")
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricText("🟢 Availability", "${optimal.availability} (${optimal.risk})")
                            Spacer(modifier = Modifier.height(6.dp))
                            MetricText("💰 Est. Avoided Cost", "₹${"%.1f".format(Locale.US, savingsLakhs)} L")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. ALL VESSEL COMPARISON & LANDED COST BREAKDOWN
        Text(
            text = "🚢 Vessel Class Suitability & Cost Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Text(
            text = "Comparing freight charter, bunker fuel consumption, port draft feasibility, and risk scores",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(10.dp))

        vessels.forEachIndexed { index, vessel ->
            VesselComparisonCard(
                vessel = vessel,
                rank = index + 1,
                isOptimal = index == 0,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun VesselComparisonCard(
    vessel: VesselEvaluation,
    rank: Int,
    isOptimal: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isOptimal) Color(0xFF3B82F6) else Color(0xFF334155)
    val cardBg = if (isOptimal) Color(0xFF1E293B) else Color(0xFF0F172A)

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isOptimal) Color(0xFF2563EB) else Color(0xFF334155),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = vessel.vessel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${vessel.capacity})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = if (vessel.aiScore >= 80) Color(0xFF10B981) else if (vessel.aiScore >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Score: ${vessel.aiScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capacity Utilization Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capacity Fit: ${vessel.utilizationPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = vessel.feasibility,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (vessel.isDraftCompliant) Color(0xFF10B981) else Color(0xFFF87171)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (vessel.utilizationPct / 100.0).toFloat().coerceIn(0f, 1f) },
                color = if (vessel.isDraftCompliant) Color(0xFF38BDF8) else Color(0xFFEF4444),
                trackColor = Color(0xFF334155),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Landed Cost Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CostChip("Charter (Est.)", "$${"%.2f".format(Locale.US, vessel.freightPmt)}")
                CostChip("Fuel (Est.)", "$${"%.2f".format(Locale.US, vessel.fuelPmt)}")
                CostChip("Port/Canal (Est.)", "$${"%.2f".format(Locale.US, vessel.portPmt)}")
                CostChip("Demurrage (Est.)", "$${"%.2f".format(Locale.US, vessel.demurragePmt)}")
                CostChip("Landed Cost (Est.)", "$${"%.2f".format(Locale.US, vessel.costPerMt)}", isTotal = true)
            }
        }
    }
}

@Composable
private fun CostChip(label: String, value: String, isTotal: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isTotal) Color(0xFFFBBF24) else Color(0xFF94A3B8),
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isTotal) Color(0xFFFBBF24) else Color.White,
            fontSize = 11.sp
        )
    }
}
