package com.example.sailfreight.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DataProvenanceSection(
    isLiveSynced: Boolean,
    lastSyncTime: String?,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, if (isLiveSynced) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("data_provenance_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (isLiveSynced) Color(0xFF34D399) else Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Data Provenance & Transparency",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Badge
                            Surface(
                                color = if (isLiveSynced) Color(0xFF065F46) else Color(0xFF78350F),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isLiveSynced) "🟢 LIVE SYNC" else "🟡 ESTIMATED BACKUP",
                                    color = if (isLiveSynced) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isLiveSynced) "Real internet feeds active • Tap to view what is real vs. estimated"
                            else "Offline fallback active • Tap to inspect data sources",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle provenance details",
                    tint = Color(0xFF94A3B8)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(bottom = 12.dp))

                    ProvenanceCategory(
                        badgeText = "🟢 REAL LIVE DATA",
                        badgeColor = Color(0xFF065F46),
                        textColor = Color(0xFF34D399),
                        title = "What comes directly from live internet feeds:",
                        points = listOf(
                            "USD/INR Foreign Exchange: Real-time currency quotes from institutional Forex endpoints.",
                            "WTI & Brent Crude Oil: Real-time commodity futures from NYMEX and ICE market feeds.",
                            "Marine Port Weather & Waves: Live telemetry from Open-Meteo maritime satellite sensors."
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProvenanceCategory(
                        badgeText = "🟣 ESTIMATED PROXIES",
                        badgeColor = Color(0xFF581C87),
                        textColor = Color(0xFFC084FC),
                        title = "What is calculated as an engineering estimate:",
                        points = listOf(
                            "Bunker Fuel ($/MT): Calculated via standard maritime proxy: Real NYMEX Crude ($/bbl) × 7.33 + crack spread. The crude oil is real; the bunker price is an empirical proxy.",
                            "Port Distances: Standard maritime nautical route navigation tables (e.g. ~4,500 NM Newcastle to Paradip).",
                            "Daily Charter Hire & Speed: Baltic Exchange indicative benchmark estimates per vessel class.",
                            "AI Freight Rate Forecasts: Machine learning projections (Prophet & Ensemble) with confidence bands."
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProvenanceCategory(
                        badgeText = "📜 HISTORICAL BASELINE",
                        badgeColor = Color(0xFF1E293B),
                        textColor = Color(0xFF94A3B8),
                        title = "What is historical training data:",
                        points = listOf(
                            "500+ Daily Trading Sessions: Historical Baltic Dry Index, Macroeconomic Regressors, and Bunker data used to train the forecasting algorithms.",
                            "Zero Data Leakage Guarantee: All features strictly shifted by >= 1 day so the model never peeks into current or future answers."
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvenanceCategory(
    badgeText: String,
    badgeColor: Color,
    textColor: Color,
    title: String,
    points: List<String>
) {
    Surface(
        color = Color(0xFF1E293B).copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            points.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text("• ", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
