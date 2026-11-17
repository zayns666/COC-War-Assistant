package com.example.state

import android.graphics.Bitmap
import com.example.model.AnalysisUiState
import com.example.model.ManualMarker
import com.example.model.MarkerType
import com.example.model.NormalizedPoint
import com.example.model.TacticalAnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object TacticalStateHolder {

    private val _activeBitmap = MutableStateFlow<Bitmap?>(null)
    val activeBitmap: StateFlow<Bitmap?> = _activeBitmap.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _activeStepIndex = MutableStateFlow(0)
    val activeStepIndex: StateFlow<Int> = _activeStepIndex.asStateFlow()

    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _manualMarkers = MutableStateFlow<List<ManualMarker>>(emptyList())
    val manualMarkers: StateFlow<List<ManualMarker>> = _manualMarkers.asStateFlow()

    private val _selectedMarkerTool = MutableStateFlow(MarkerType.TROOP_DROP)
    val selectedMarkerTool: StateFlow<MarkerType> = _selectedMarkerTool.asStateFlow()

    private val _showAiPaths = MutableStateFlow(true)
    val showAiPaths: StateFlow<Boolean> = _showAiPaths.asStateFlow()

    private val _showWeaknessHeatmap = MutableStateFlow(true)
    val showWeaknessHeatmap: StateFlow<Boolean> = _showWeaknessHeatmap.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isOverlayDrawingMode = MutableStateFlow(false)
    val isOverlayDrawingMode: StateFlow<Boolean> = _isOverlayDrawingMode.asStateFlow()

    private val _hasMediaProjection = MutableStateFlow(false)
    val hasMediaProjection: StateFlow<Boolean> = _hasMediaProjection.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun setActiveBitmap(bitmap: Bitmap?) {
        _activeBitmap.value = bitmap
    }

    fun setAnalysisState(state: AnalysisUiState) {
        _analysisState.value = state
        if (state is AnalysisUiState.Success) {
            _activeStepIndex.value = 0
            _isSimulating.value = true
        }
    }

    fun setActiveStepIndex(index: Int) {
        val currentResult = (_analysisState.value as? AnalysisUiState.Success)?.result
        val maxIdx = (currentResult?.steps?.size ?: 1) - 1
        _activeStepIndex.value = index.coerceIn(0, maxIdx.coerceAtLeast(0))
    }

    fun nextStep() {
        val currentResult = (_analysisState.value as? AnalysisUiState.Success)?.result ?: return
        if (currentResult.steps.isEmpty()) return
        _activeStepIndex.value = (_activeStepIndex.value + 1) % currentResult.steps.size
    }

    fun prevStep() {
        val currentResult = (_analysisState.value as? AnalysisUiState.Success)?.result ?: return
        if (currentResult.steps.isEmpty()) return
        val size = currentResult.steps.size
        _activeStepIndex.value = (_activeStepIndex.value - 1 + size) % size
    }

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
    }

    fun setSelectedMarkerTool(tool: MarkerType) {
        _selectedMarkerTool.value = tool
    }

    fun addTapMarker(xNorm: Float, yNorm: Float) {
        val tool = _selectedMarkerTool.value
        val marker = ManualMarker(
            id = UUID.randomUUID().toString(),
            type = tool,
            xNorm = xNorm.coerceIn(0.02f, 0.98f),
            yNorm = yNorm.coerceIn(0.02f, 0.98f),
            points = if (tool == MarkerType.PATH_ARROW) {
                listOf(
                    NormalizedPoint(xNorm.coerceIn(0.02f, 0.98f), yNorm.coerceIn(0.02f, 0.98f)),
                    NormalizedPoint((xNorm + 0.08f).coerceIn(0.02f, 0.98f), (yNorm - 0.08f).coerceIn(0.02f, 0.98f))
                )
            } else emptyList(),
            label = tool.labelId
        )
        _manualMarkers.value = _manualMarkers.value + marker
    }

    fun addPathMarker(points: List<NormalizedPoint>) {
        if (points.size < 2) return
        val first = points.first()
        val marker = ManualMarker(
            id = UUID.randomUUID().toString(),
            type = MarkerType.PATH_ARROW,
            xNorm = first.x,
            yNorm = first.y,
            points = points,
            label = "Jalur"
        )
        _manualMarkers.value = _manualMarkers.value + marker
    }

    fun setManualMarkers(markers: List<ManualMarker>) {
        _manualMarkers.value = markers
    }

    fun undoMarker() {
        val list = _manualMarkers.value
        if (list.isNotEmpty()) {
            _manualMarkers.value = list.dropLast(1)
        }
    }

    fun clearMarkers() {
        _manualMarkers.value = emptyList()
    }

    fun toggleShowAiPaths() {
        _showAiPaths.value = !_showAiPaths.value
    }

    fun toggleShowWeaknessHeatmap() {
        _showWeaknessHeatmap.value = !_showWeaknessHeatmap.value
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
        if (!running) {
            _isOverlayDrawingMode.value = false
        }
    }

    fun setOverlayDrawingMode(enabled: Boolean) {
        _isOverlayDrawingMode.value = enabled
    }

    fun toggleOverlayDrawingMode() {
        _isOverlayDrawingMode.value = !_isOverlayDrawingMode.value
    }

    fun setHasMediaProjection(hasProjection: Boolean) {
        _hasMediaProjection.value = hasProjection
    }

    fun setStatusBanner(message: String?) {
        _statusBanner.value = message
    }

    fun loadSavedPlan(bitmap: Bitmap?, result: TacticalAnalysisResult, markers: List<ManualMarker>) {
        _activeBitmap.value = bitmap
        _analysisState.value = AnalysisUiState.Success(result)
        _manualMarkers.value = markers
        _activeStepIndex.value = 0
        _isSimulating.value = true
    }
}
