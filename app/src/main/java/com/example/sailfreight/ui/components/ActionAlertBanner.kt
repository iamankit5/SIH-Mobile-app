package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

@Composable
fun ActionAlertBanner(
    pctChange30d: Double,
    currentSpotPmt: Double,
    forecast30dPmt: Double,
    avoidedCostLakhs: Double,
    modifier: Modifier = Modifier
) {
    val isCharterNow = pctChange30d > 1.0

    val backgroundColor = if (isCharterNow) Color(0x33EF4444) else Color(0x3310B981)
    val borderColor = if (isCharterNow) Color(0xFFEF4444) else Color(0xFF10B981)
    val titleColor = if (isCharterNow) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCharterNow) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCharterNow) "PROCUREMENT ACTION: CHARTER NOW" else "PROCUREMENT ACTION: HOLD / WAIT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isCharterNow) {
                Text(
                    text = "Multi-horizon forecast projects a +${"%.1f".format(Locale.US, pctChange30d)}% rise in freight rates over 30 days. Execute tender promptly to lock in baseline rates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFEE2E2),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    AlertDetailRow("Current Landed Baseline", "$${"%.2f".format(Locale.US, currentSpotPmt)}/MT")
                    AlertDetailRow("Expected 30-Day Landed", "$${"%.2f".format(Locale.US, forecast30dPmt)}/MT")
                    AlertDetailRow("Potential Avoided Cost", "₹${"%.1f".format(Locale.US, avoidedCostLakhs)} Lakhs")
                }
            } else {
                Text(
                    text = "Rates expected to drop or soften by ${"%.1f".format(Locale.US, abs(pctChange30d))}%. Postpone tender release to capture favorable spot pricing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD1FAE5),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AlertDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "• $label: ",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFECACA)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
