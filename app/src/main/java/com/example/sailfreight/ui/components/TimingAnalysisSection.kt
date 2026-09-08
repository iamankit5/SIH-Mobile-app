package com.example.sailfreight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.sailfreight.data.model.TimingOption
import java.util.Locale

@Composable
fun TimingAnalysisSection(
    timingOptions: List<TimingOption>,
    currentSpotPmt: Double,
    forecast7dPmt: Double,
    forecast14dPmt: Double,
    modifier: Modifier = Modifier
) {
    val diff7d = forecast7dPmt - currentSpotPmt
    val diff14d = forecast14dPmt - currentSpotPmt

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "⏱️ Procurement Timing Analysis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Text(
            text = "Strategic charter execution matrix: \"What Happens If I Wait?\"",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Action", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.2f))
                    Text("Expected Landed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.1f))
                    Text("Total Expense", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.1f))
                    Text("Variance", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), modifier = Modifier.weight(1.3f))
                }

                timingOptions.forEach { opt ->
                    TimingRow(opt)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Decision Sequence
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📌 AI Decision Sequence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DecisionBullet(
                            "Today",
                            "Market currently benchmarked at $${"%.2f".format(Locale.US, currentSpotPmt)}/MT spot freight rate."
                        )
                        DecisionBullet(
                            "+7 Days",
                            if (diff7d < 0) "Rates projected to soften. Favorable window if spot cargo allows minor delay."
                            else "Rates projected to inflate (+${"%.2f".format(Locale.US, diff7d)}/MT). Early booking avoids rate spikes."
                        )
                        DecisionBullet(
                            "+14 Days",
                            if (diff14d < 0) "Optimal window for charter tender release to capture lowest landed cost."
                            else "Cost friction increases significantly (+${"%.2f".format(Locale.US, diff14d)}/MT). Recommend chartering now."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingRow(option: TimingOption) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.action,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = option.expectedLanded,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF38BDF8),
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = option.totalExpenseLakhs,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                modifier = Modifier.weight(1.1f)
            )
            Text(
                text = option.varianceText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (option.varianceAmountLakhs == 0.0) Color(0xFF94A3B8)
                else if (option.isSaving) Color(0xFF10B981)
                else Color(0xFFEF4444),
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

@Composable
private fun DecisionBullet(timeframe: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "• $timeframe: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1)
        )
    }
}
