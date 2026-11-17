package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WarPlanEntity
import com.example.ui.components.EsportsGlassPanel
import com.example.ui.components.MiniEfficiencyRing
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    plans: List<WarPlanEntity>,
    onOpenPlan: (WarPlanEntity) -> Unit,
    onDeletePlan: (WarPlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

    if (plans.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            EsportsGlassPanel(
                shape = RoundedCornerShape(28.dp),
                accentColor = WarGold,
                surfaceAlpha = 0.68f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(WarGold.copy(alpha = 0.16f))
                            .border(1.dp, WarGold.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = WarGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Belum Ada Arsip Scan War",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Setiap base yang Anda analisis akan otomatis tersimpan dengan jalur pasukan dan penanda taktisnya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
        return
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            item {
                Text(
                    text = "ARSIP STRATEGI CLAN WAR",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
            }

            items(plans, key = { it.id }) { plan ->
                EsportsGlassPanel(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = CyberCyan,
                    surfaceAlpha = 0.68f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlan(plan) }
                        .testTag("history_plan_${plan.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        MiniEfficiencyRing(score = plan.efficiencyScore)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(WarGold.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = plan.townHall,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarGold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(plan.predictedStars) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = WarGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = plan.strategyName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${plan.baseTitle} • ${dateFormat.format(Date(plan.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { onOpenPlan(plan) }) {
                            Icon(Icons.Default.PlayArrow, "Muat Simulasi", tint = CyberCyan)
                        }
                        IconButton(onClick = { onDeletePlan(plan) }) {
                            Icon(Icons.Default.DeleteOutline, "Hapus Riwayat", tint = ThreatCrimson)
                        }
                    }
                }
            }
        }
    }
}
