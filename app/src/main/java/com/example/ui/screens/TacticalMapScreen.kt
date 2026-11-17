package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisUiState
import com.example.model.ManualMarker
import com.example.model.MarkerType
import com.example.model.NormalizedPoint
import com.example.model.WeaknessSeverity
import com.example.ui.components.EfficiencyArcGauge
import com.example.ui.components.EsportsGlassPanel
import com.example.ui.components.GlassStatusPill
import com.example.ui.components.TacticalMapCanvas
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SpellViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.VictoryEmerald
import com.example.ui.theme.WarGold

@Composable
fun TacticalMapScreen(
    bitmap: Bitmap?,
    analysisState: AnalysisUiState,
    activeStepIndex: Int,
    isSimulating: Boolean,
    showAiPaths: Boolean,
    showWeaknessHeatmap: Boolean,
    manualMarkers: List<ManualMarker>,
    selectedMarkerTool: MarkerType,
    onSelectStep: (Int) -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onToggleSimulation: () -> Unit,
    onToggleAiPaths: () -> Unit,
    onToggleHeatmap: () -> Unit,
    onSelectMarkerTool: (MarkerType) -> Unit,
    onTapPlaceMarker: (Float, Float) -> Unit,
    onDrawPathComplete: (List<NormalizedPoint>) -> Unit,
    onUndoMarker: () -> Unit,
    onClearMarkers: () -> Unit,
    onSaveWithMarkers: () -> Unit,
    onPickScreenshot: () -> Unit,
    onAnalyzeSampleBase: () -> Unit,
    onRetryAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var markingMode by remember { mutableStateOf(false) }

    if (bitmap == null && analysisState is AnalysisUiState.Idle) {
        TacticalEmptyGlassState(
            onPickScreenshot = onPickScreenshot,
            onAnalyzeSampleBase = onAnalyzeSampleBase,
            modifier = modifier
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    GlassMapViewportAndControls(
                        bitmap = bitmap,
                        analysisState = analysisState,
                        activeStepIndex = activeStepIndex,
                        isSimulating = isSimulating,
                        showAiPaths = showAiPaths,
                        showWeaknessHeatmap = showWeaknessHeatmap,
                        manualMarkers = manualMarkers,
                        markingMode = markingMode,
                        selectedMarkerTool = selectedMarkerTool,
                        onToggleMarkingMode = { markingMode = !markingMode },
                        onPrevStep = onPrevStep,
                        onNextStep = onNextStep,
                        onToggleSimulation = onToggleSimulation,
                        onToggleAiPaths = onToggleAiPaths,
                        onToggleHeatmap = onToggleHeatmap,
                        onSelectMarkerTool = onSelectMarkerTool,
                        onTapPlaceMarker = onTapPlaceMarker,
                        onDrawPathComplete = onDrawPathComplete,
                        onUndoMarker = onUndoMarker,
                        onClearMarkers = onClearMarkers,
                        onSaveWithMarkers = onSaveWithMarkers,
                        onRetryAnalysis = onRetryAnalysis
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                ) {
                    glassStrategyBreakdownItems(
                        analysisState = analysisState,
                        activeStepIndex = activeStepIndex,
                        onSelectStep = onSelectStep,
                        onRetryAnalysis = onRetryAnalysis
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    GlassMapViewportAndControls(
                        bitmap = bitmap,
                        analysisState = analysisState,
                        activeStepIndex = activeStepIndex,
                        isSimulating = isSimulating,
                        showAiPaths = showAiPaths,
                        showWeaknessHeatmap = showWeaknessHeatmap,
                        manualMarkers = manualMarkers,
                        markingMode = markingMode,
                        selectedMarkerTool = selectedMarkerTool,
                        onToggleMarkingMode = { markingMode = !markingMode },
                        onPrevStep = onPrevStep,
                        onNextStep = onNextStep,
                        onToggleSimulation = onToggleSimulation,
                        onToggleAiPaths = onToggleAiPaths,
                        onToggleHeatmap = onToggleHeatmap,
                        onSelectMarkerTool = onSelectMarkerTool,
                        onTapPlaceMarker = onTapPlaceMarker,
                        onDrawPathComplete = onDrawPathComplete,
                        onUndoMarker = onUndoMarker,
                        onClearMarkers = onClearMarkers,
                        onSaveWithMarkers = onSaveWithMarkers,
                        onRetryAnalysis = onRetryAnalysis
                    )
                }

                glassStrategyBreakdownItems(
                    analysisState = analysisState,
                    activeStepIndex = activeStepIndex,
                    onSelectStep = onSelectStep,
                    onRetryAnalysis = onRetryAnalysis
                )
            }
        }
    }
}

@Composable
private fun TacticalEmptyGlassState(
    onPickScreenshot: () -> Unit,
    onAnalyzeSampleBase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        EsportsGlassPanel(
            shape = RoundedCornerShape(28.dp),
            accentColor = CyberCyan,
            surfaceAlpha = 0.72f,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CyberCyan.copy(alpha = 0.35f), CyberCyan.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Belum Ada Base yang Dipindai",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pilih screenshot base lawan atau jalankan simulasi pada peta contoh untuk melihat heatmap kelemahan dan rute serangan animasi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(WarGold)
                            .clickable(onClick = onAnalyzeSampleBase)
                            .padding(horizontal = 18.dp)
                            .testTag("empty_sample_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = ObsidianNight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Base Contoh", style = MaterialTheme.typography.labelLarge, color = ObsidianNight)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.6f), CircleShape)
                            .clickable(onClick = onPickScreenshot)
                            .padding(horizontal = 18.dp)
                            .testTag("empty_pick_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dari Galeri", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassMapViewportAndControls(
    bitmap: Bitmap?,
    analysisState: AnalysisUiState,
    activeStepIndex: Int,
    isSimulating: Boolean,
    showAiPaths: Boolean,
    showWeaknessHeatmap: Boolean,
    manualMarkers: List<ManualMarker>,
    markingMode: Boolean,
    selectedMarkerTool: MarkerType,
    onToggleMarkingMode: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onToggleSimulation: () -> Unit,
    onToggleAiPaths: () -> Unit,
    onToggleHeatmap: () -> Unit,
    onSelectMarkerTool: (MarkerType) -> Unit,
    onTapPlaceMarker: (Float, Float) -> Unit,
    onDrawPathComplete: (List<NormalizedPoint>) -> Unit,
    onUndoMarker: () -> Unit,
    onClearMarkers: () -> Unit,
    onSaveWithMarkers: () -> Unit,
    onRetryAnalysis: () -> Unit
) {
    val result = (analysisState as? AnalysisUiState.Success)?.result
    val aspect = if (bitmap != null && bitmap.height > 0) {
        (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.6f, 2.2f)
    } else 16f / 9f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Map Canvas with Floating Glass Pill HUD Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(26.dp))
                .background(ObsidianNight)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        if (markingMode) listOf(WarGold, CyberCyan)
                        else listOf(CyberCyan.copy(alpha = 0.55f), Color.White.copy(alpha = 0.16f))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Peta Base Lawan",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            TacticalMapCanvas(
                analysisResult = result,
                activeStepIndex = activeStepIndex,
                isSimulating = isSimulating,
                showAiPaths = showAiPaths,
                showWeaknessHeatmap = showWeaknessHeatmap,
                manualMarkers = manualMarkers,
                interactiveMarkingEnabled = markingMode,
                selectedTool = selectedMarkerTool,
                onTapPlaceMarker = onTapPlaceMarker,
                onDrawPathComplete = onDrawPathComplete,
                modifier = Modifier.fillMaxSize()
            )

            // Top floating HUD pills inside map viewport
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                if (result != null) {
                    GlassStatusPill(
                        label = "${result.estimatedTownHall} • ${result.baseArchetype}",
                        accentColor = WarGold,
                        isPulsingDot = false,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FloatingMapTogglePill(
                        icon = Icons.Default.Timeline,
                        label = "Jalur",
                        active = showAiPaths,
                        accent = CyberCyan,
                        onClick = onToggleAiPaths
                    )
                    FloatingMapTogglePill(
                        icon = Icons.Default.Radar,
                        label = "Titik Lemah",
                        active = showWeaknessHeatmap,
                        accent = ThreatCrimson,
                        onClick = onToggleHeatmap
                    )
                    FloatingMapTogglePill(
                        icon = Icons.Default.Edit,
                        label = "Tandai",
                        active = markingMode,
                        accent = WarGold,
                        onClick = onToggleMarkingMode,
                        modifier = Modifier.testTag("toggle_marking_mode_chip")
                    )
                }
            }

            // Bottom floating translucent marker dock when marking mode is active
            androidx.compose.animation.AnimatedVisibility(
                visible = markingMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                EsportsGlassPanel(
                    shape = CircleShape,
                    accentColor = WarGold,
                    surfaceAlpha = 0.84f
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        MarkerType.entries.forEach { tool ->
                            val selected = selectedMarkerTool == tool
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) WarGold else Color.White.copy(alpha = 0.08f))
                                    .clickable { onSelectMarkerTool(tool) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tool.labelId,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) ObsidianNight else Color.White
                                )
                            }
                        }
                        IconButton(onClick = onUndoMarker, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onClearMarkers, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Hapus", tint = ThreatCrimson, modifier = Modifier.size(16.dp))
                        }
                        if (result != null && bitmap != null) {
                            IconButton(onClick = onSaveWithMarkers, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.BookmarkAdd, "Simpan", tint = VictoryEmerald, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Loading Scanner Overlay
            if (analysisState is AnalysisUiState.Loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianNight.copy(alpha = 0.78f))
                ) {
                    EsportsGlassPanel(
                        shape = RoundedCornerShape(22.dp),
                        accentColor = WarGold,
                        surfaceAlpha = 0.85f
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                        ) {
                            CircularProgressIndicator(color = WarGold, strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Gemini AI Memetakan Pertahanan...",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Menghitung titik lemah & jalur pasukan optimal",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }

        // Floating Glass Playback Stepper Dock
        if (result != null && result.steps.isNotEmpty()) {
            EsportsGlassPanel(
                shape = CircleShape,
                accentColor = CyberCyan,
                surfaceAlpha = 0.74f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevStep, modifier = Modifier.size(42.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Fase Sebelumnya",
                                tint = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FASE 0${activeStepIndex + 1} / 0${result.steps.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                            Text(
                                text = result.steps.getOrNull(activeStepIndex)?.timingWindow ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        IconButton(onClick = onNextStep, modifier = Modifier.size(42.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Fase Berikutnya",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bitmap != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable(onClick = onSaveWithMarkers)
                            ) {
                                Icon(
                                    Icons.Default.BookmarkAdd,
                                    contentDescription = "Simpan Snapshot",
                                    tint = VictoryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(WarGold, Color(0xFFFBBF24)))
                                )
                                .clickable(onClick = onToggleSimulation)
                                .padding(horizontal = 16.dp)
                                .testTag("play_pause_sim_button")
                        ) {
                            Icon(
                                imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ObsidianNight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSimulating) "Jeda" else "Simulasi",
                                style = MaterialTheme.typography.labelLarge,
                                color = ObsidianNight
                            )
                        }
                    }
                }
            }
        } else if (analysisState is AnalysisUiState.Error) {
            EsportsGlassPanel(
                shape = RoundedCornerShape(20.dp),
                accentColor = ThreatCrimson,
                surfaceAlpha = 0.78f,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = ThreatCrimson)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = analysisState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onRetryAnalysis) {
                        Icon(Icons.Default.Refresh, "Coba Lagi", tint = WarGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingMapTogglePill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) accent.copy(alpha = 0.9f) else ObsidianNight.copy(alpha = 0.72f),
        label = "pill_bg"
    )
    val fgColor = if (active && accent != ThreatCrimson) ObsidianNight else if (active) Color.White else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, accent.copy(alpha = if (active) 1f else 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = fgColor, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.glassStrategyBreakdownItems(
    analysisState: AnalysisUiState,
    activeStepIndex: Int,
    onSelectStep: (Int) -> Unit,
    onRetryAnalysis: () -> Unit
) {
    when (analysisState) {
        is AnalysisUiState.Success -> {
            val res = analysisState.result

            // Radial Efficiency Arc + Strategy Hero Glass Card
            item {
                EsportsGlassPanel(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = WarGold,
                    surfaceAlpha = 0.72f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        EfficiencyArcGauge(
                            score = res.efficiencyScore,
                            predictedStars = res.predictedStars,
                            size = 110.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            GlassStatusPill(
                                label = "REKOMENDASI TERBAIK",
                                accentColor = VictoryEmerald,
                                isPulsingDot = true
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = res.recommendedStrategyName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = res.strategySummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // ML Hardest Path vs Optimal Route Card
            item {
                EsportsGlassPanel(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = ThreatCrimson,
                    surfaceAlpha = 0.68f,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = ThreatCrimson, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PREDIKSI JALUR TERSULIT VS OPTIMAL",
                                style = MaterialTheme.typography.labelLarge,
                                color = ThreatCrimson
                            )
                        }
                        Text(
                            text = res.hardestPathWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VictoryEmerald.copy(alpha = 0.12f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Kelemahan Utama: ${res.weaknessSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = VictoryEmerald
                            )
                        }
                    }
                }
            }

            // Army, Spells & Siege Capsule Deck
            item {
                EsportsGlassPanel(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = SpellViolet,
                    surfaceAlpha = 0.64f,
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
                                text = "KOMPOSISI PASUKAN",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            GlassStatusPill(
                                label = "Siege: ${res.siegeMachine}",
                                accentColor = CyberCyan,
                                isPulsingDot = false
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            res.armyComposition.forEach { unit ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(WarGold.copy(alpha = 0.14f))
                                        .border(1.dp, WarGold.copy(alpha = 0.45f), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${unit.count}x ${unit.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarGold
                                    )
                                }
                            }
                            res.spellComposition.forEach { spell ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(SpellViolet.copy(alpha = 0.16f))
                                        .border(1.dp, SpellViolet.copy(alpha = 0.5f), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${spell.count}x ${spell.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFE9D5FF)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weaknesses Pills
            if (res.weaknesses.isNotEmpty()) {
                item {
                    Text(
                        text = "TITIK KELEMAHAN TERDETEKSI (${res.weaknesses.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        res.weaknesses.forEach { w ->
                            val sevColor = when (w.severity) {
                                WeaknessSeverity.HIGH -> ThreatCrimson
                                WeaknessSeverity.MEDIUM -> WarGold
                                WeaknessSeverity.LOW -> VictoryEmerald
                            }
                            EsportsGlassPanel(
                                shape = RoundedCornerShape(18.dp),
                                accentColor = sevColor,
                                surfaceAlpha = 0.6f,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(sevColor)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${w.label} • ${w.defenseType}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = w.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step-by-Step Timeline Glass Cards
            item {
                Text(
                    text = "EKSEKUSI STEP-BY-STEP",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarGold
                )
            }

            itemsIndexed(res.steps) { index, step ->
                val isSelected = index == activeStepIndex
                val accent = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.25f)

                EsportsGlassPanel(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = accent,
                    surfaceAlpha = if (isSelected) 0.82f else 0.56f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectStep(index) }
                        .testTag("step_card_${step.stepNumber}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) CyberCyan else WarGold.copy(alpha = 0.18f)
                                        )
                                ) {
                                    Text(
                                        text = "0${step.stepNumber}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) ObsidianNight else WarGold
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = step.timingWindow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = step.instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                        if (step.troopFocus.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Pasukan: ${step.troopFocus}",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarGold
                            )
                        }
                    }
                }
            }
        }
        is AnalysisUiState.Error -> {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(WarGold)
                        .clickable(onClick = onRetryAnalysis)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = ObsidianNight)
                    Spacer(Modifier.width(8.dp))
                    Text("Ulangi Analisis Base", style = MaterialTheme.typography.labelLarge, color = ObsidianNight)
                }
            }
        }
        else -> Unit
    }
}
