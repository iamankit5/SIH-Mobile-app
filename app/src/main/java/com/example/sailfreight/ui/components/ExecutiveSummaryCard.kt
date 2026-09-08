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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sailfreight.data.model.VesselEvaluation
import java.util.Locale

@Composable
fun ExecutiveSummaryCard(
    optimal: VesselEvaluation,
    cargoQty: Int,
    cargoType: String,
    origin: String,
    destination: String,
    savingsLakhs: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, Color(0xFF6366F1)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI EXECUTIVE SUMMARY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }

                    Surface(
                        color = Color(0xFF4F46E5),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "CONFIDENCE: 95% (PROPHET)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val formattedQty = "%,d".format(Locale.US, cargoQty)
                val savingsStr = "₹${"%.1f".format(Locale.US, savingsLakhs)} Lakhs"

                val summaryText = buildAnnotatedString {
                    append("Optimal Strategy: Charter ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))) {
                        append(optimal.vessel)
                        append(" (Rank #1)")
                    }
                    append(" for ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append("$formattedQty MT")
                    }
                    append(" of $cargoType from ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFFF1F5F9))) {
                        append(origin)
                    }
                    append(" to ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFFF1F5F9))) {
                        append(destination)
                    }
                    append(". Provides ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF10B981))) {
                        append("${optimal.utilizationPct}%")
                    }
                    append(" capacity fit at a total landed cost of ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))) {
                        append("$${"%.2f".format(Locale.US, optimal.costPerMt)}/MT")
                    }
                    append(", avoiding an estimated ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF34D399))) {
                        append(savingsStr)
                    }
                    append(" compared to alternative vessel classes.")
                }

                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 21.sp
                )
            }
        }
    }
}
