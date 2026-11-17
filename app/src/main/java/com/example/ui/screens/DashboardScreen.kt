package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WarPlanEntity
import com.example.ui.components.EsportsGlassPanel
import com.example.ui.components.GlassStatusPill
import com.example.ui.components.MiniEfficiencyRing
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold

@Composable
fun DashboardScreen(
    canDrawOverlays: Boolean,
    hasNotificationPermission: Boolean,
    isOverlayRunning: Boolean,
    hasMediaProjection: Boolean,
    savedPlans: List<WarPlanEntity>,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestMediaProjectionAndStartOverlay: () -> Unit,
    onToggleOverlayOnly: () -> Unit,
    onPickScreenshot: () -> Unit,
    onAnalyzeSampleBase: () -> Unit,
    onOpenPlan: (WarPlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Esports Glass Hero HUD Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(CyberCyan.copy(alpha = 0.6f), WarGold.copy(alpha = 0.45f))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_war_banner),
                        contentDescription = "Banner Komando Clan War",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianNight.copy(alpha = 0.15f),
                                        ObsidianNight.copy(alpha = 0.55f),
                                        ObsidianNight.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )

                    // Top-right floating live HUD badge
                    GlassStatusPill(
                        label = if (isOverlayRunning) "HUD MENGAMBANG AKTIF" else "SIAP TEMPUR",
                        accentColor = if (isOverlayRunning) VictoryEmerald else CyberCyan,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    )

                    // Bottom floating translucent glass caption
                    EsportsGlassPanel(
                        shape = RoundedCornerShape(20.dp),
                        accentColor = if (isOverlayRunning) VictoryEmerald else WarGold,
                        surfaceAlpha = 0.62f,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = "Esports Tactical War HUD",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Deteksi titik lemah otomatis, jalur pasukan step-by-step, dan penanda real-time di atas base lawan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Floating Pill Quick Controls (Overlay + Live Screen Capture)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassCapsuleAction(
                        label = if (isOverlayRunning) stringResource(R.string.action_stop_overlay) else "Overlay HUD",
                        icon = Icons.Default.Layers,
                        accent = if (isOverlayRunning) ThreatCrimson else WarGold,
                        filled = true,
                        onClick = {
                            if (!canDrawOverlays) onRequestOverlayPermission()
                            else onToggleOverlayOnly()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_overlay_button")
                    )

                    GlassCapsuleAction(
                        label = if (hasMediaProjection) "Scan Aktif" else "Tangkap Layar",
                        icon = Icons.Default.CameraAlt,
                        accent = CyberCyan,
                        filled = false,
                        onClick = {
                            if (!canDrawOverlays) onRequestOverlayPermission()
                            else onRequestMediaProjectionAndStartOverlay()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_capture_button")
                    )
                }
            }

            // Translucent Readiness Glass Matrix
            item {
                EsportsGlassPanel(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = CyberCyan,
                    surfaceAlpha = 0.62f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "KESIAPAN SENSOR & HUD",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                            val readyCount = listOf(
                                canDrawOverlays,
                                hasNotificationPermission,
                                hasMediaProjection
                            ).count { it }
                            Text(
                                text = "$readyCount/3 AKTIF",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (readyCount == 3) VictoryEmerald else WarGold
                            )
                        }

                        GlassPermissionPillRow(
                            icon = Icons.Default.Layers,
                            title = "Overlay di Atas Aplikasi",
                            subtitle = if (canDrawOverlays) "Siap mengambang di atas COC" else "Ketuk untuk beri izin overlay",
                            granted = canDrawOverlays,
                            onClick = onRequestOverlayPermission,
                            tag = "perm_overlay_row"
                        )

                        GlassPermissionPillRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifikasi Layanan HUD",
                            subtitle = if (hasNotificationPermission) "Aktif menjaga overlay tetap stabil" else "Ketuk untuk aktifkan notifikasi",
                            granted = hasNotificationPermission,
                            onClick = onRequestNotificationPermission,
                            tag = "perm_notification_row"
                        )

                        GlassPermissionPillRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Tangkap Layar MediaProjection",
                            subtitle = if (hasMediaProjection) "Siap scan base langsung di layar" else "Ketuk untuk izin rekam bingkai layar",
                            granted = hasMediaProjection,
                            onClick = {
                                if (!canDrawOverlays) onRequestOverlayPermission()
                                else onRequestMediaProjectionAndStartOverlay()
                            },
                            tag = "perm_projection_row"
                        )
                    }
                }
            }

            // AI Scan Launchers
            item {
                Text(
                    text = "PINDAI & SIMULASI BASE",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarGold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassActionFeatureCard(
                        icon = Icons.Default.PhotoLibrary,
                        accent = CyberCyan,
                        title = stringResource(R.string.action_pick_base),
                        description = "Pilih screenshot base lawan untuk analisis titik lemah ML dan simulasi rute serangan.",
                        buttonLabel = "Buka Galeri",
                        testTag = "pick_base_button",
                        onClick = onPickScreenshot
                    )

                    GlassActionFeatureCard(
                        icon = Icons.Default.AutoAwesome,
                        accent = WarGold,
                        title = stringResource(R.string.action_test_sample),
                        description = "Jalankan simulasi langsung di atas peta contoh dengan heatmap kelemahan & jalur animasi.",
                        buttonLabel = "Mulai Simulasi",
                        testTag = "test_sample_base_button",
                        onClick = onAnalyzeSampleBase
                    )
                }
            }

            // Recent War Plans Carousel with Mini Rings
            if (savedPlans.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TARGET WAR TERBARU",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = "${savedPlans.size} arsip",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedPlans.take(6), key = { it.id }) { plan ->
                            EsportsGlassPanel(
                                shape = RoundedCornerShape(22.dp),
                                accentColor = WarGold,
                                surfaceAlpha = 0.65f,
                                modifier = Modifier
                                    .width(248.dp)
                                    .clickable { onOpenPlan(plan) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    MiniEfficiencyRing(score = plan.efficiencyScore)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.townHall,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarGold
                                        )
                                        Text(
                                            text = plan.strategyName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = plan.baseTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCapsuleAction(
    label: String,
    icon: ImageVector,
    accent: Color,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (filled) {
        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.8f)))
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF151E32).copy(alpha = 0.78f),
                Color(0xFF0B1120).copy(alpha = 0.86f)
            )
        )
    }
    val contentColor = if (filled && accent != ThreatCrimson) ObsidianNight else if (filled) Color.White else accent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(54.dp)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, accent.copy(alpha = if (filled) 0.9f else 0.55f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassPermissionPillRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val accent = if (granted) VictoryEmerald else WarGold
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
            contentDescription = if (granted) "Aktif" else "Perlu Izin",
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun GlassActionFeatureCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    EsportsGlassPanel(
        shape = RoundedCornerShape(24.dp),
        accentColor = accent,
        surfaceAlpha = 0.66f,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.3f), accent.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
        }
    }
}
