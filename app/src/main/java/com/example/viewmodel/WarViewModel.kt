package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.BaseAnalysisRepository
import com.example.data.local.WarPlanDatabase
import com.example.data.local.WarPlanEntity
import com.example.data.local.WarPlanRepository
import com.example.model.AnalysisUiState
import com.example.model.MarkerType
import com.example.model.NormalizedPoint
import com.example.model.TacticalJsonCodec
import com.example.state.TacticalStateHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "WarViewModel"

class WarViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = BaseAnalysisRepository()
    private val planRepository = WarPlanRepository(
        context = application,
        dao = WarPlanDatabase.getInstance(application).warPlanDao()
    )

    val activeBitmap: StateFlow<Bitmap?> = TacticalStateHolder.activeBitmap
    val analysisState: StateFlow<AnalysisUiState> = TacticalStateHolder.analysisState
    val activeStepIndex: StateFlow<Int> = TacticalStateHolder.activeStepIndex
    val isSimulating: StateFlow<Boolean> = TacticalStateHolder.isSimulating
    val manualMarkers = TacticalStateHolder.manualMarkers
    val selectedMarkerTool = TacticalStateHolder.selectedMarkerTool
    val showAiPaths = TacticalStateHolder.showAiPaths
    val showWeaknessHeatmap = TacticalStateHolder.showWeaknessHeatmap
    val isOverlayRunning = TacticalStateHolder.isOverlayRunning
    val isOverlayDrawingMode = TacticalStateHolder.isOverlayDrawingMode
    val hasMediaProjection = TacticalStateHolder.hasMediaProjection
    val statusBanner = TacticalStateHolder.statusBanner

    val savedPlans: StateFlow<List<WarPlanEntity>> = planRepository.allPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun analyzeUri(uri: Uri) {
        viewModelScope.launch {
            TacticalStateHolder.setAnalysisState(AnalysisUiState.Loading)
            try {
                val bitmap = decodeBitmapFromUri(uri)
                    ?: throw IllegalStateException("Gagal membaca gambar base dari galeri.")
                TacticalStateHolder.setActiveBitmap(bitmap)
                runAiAnalysis(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from URI", e)
                TacticalStateHolder.setAnalysisState(
                    AnalysisUiState.Error(e.localizedMessage ?: "Gagal memuat gambar base.")
                )
            }
        }
    }

    fun analyzeSampleBase() {
        viewModelScope.launch {
            TacticalStateHolder.setAnalysisState(AnalysisUiState.Loading)
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeResource(
                        getApplication<Application>().resources,
                        R.drawable.img_sample_base
                    )
                } ?: throw IllegalStateException("Aset peta contoh tidak ditemukan.")
                TacticalStateHolder.setActiveBitmap(bitmap)
                runAiAnalysis(bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Sample base analysis failed", e)
                TacticalStateHolder.setAnalysisState(
                    AnalysisUiState.Error(e.localizedMessage ?: "Analisis AI gagal.")
                )
            }
        }
    }

    fun reanalyzeActiveBase() {
        val bitmap = activeBitmap.value ?: return
        viewModelScope.launch {
            TacticalStateHolder.setAnalysisState(AnalysisUiState.Loading)
            runAiAnalysis(bitmap)
        }
    }

    private suspend fun runAiAnalysis(bitmap: Bitmap) {
        try {
            val result = aiRepository.analyzeBase(bitmap)
            TacticalStateHolder.setAnalysisState(AnalysisUiState.Success(result))
            // Auto-save to local Room War History
            runCatching {
                planRepository.savePlan(bitmap, result, TacticalStateHolder.manualMarkers.value)
            }.onFailure { err ->
                Log.w(TAG, "Failed to cache war plan locally", err)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Base analysis unavailable", e)
            TacticalStateHolder.setAnalysisState(
                AnalysisUiState.Error(
                    e.localizedMessage ?: "Analisis otomatis belum tersedia. Gunakan penanda manual."
                )
            )
        }
    }

    fun saveCurrentSnapshotWithMarkers() {
        val bitmap = activeBitmap.value ?: return
        val result = (analysisState.value as? AnalysisUiState.Success)?.result ?: return
        viewModelScope.launch {
            runCatching {
                planRepository.savePlan(bitmap, result, manualMarkers.value)
            }.onSuccess {
                TacticalStateHolder.setStatusBanner("Rencana serangan & penanda berhasil disimpan.")
            }.onFailure { e ->
                TacticalStateHolder.setStatusBanner("Gagal menyimpan: ${e.localizedMessage}")
            }
        }
    }

    fun loadSavedPlan(plan: WarPlanEntity, onLoaded: () -> Unit) {
        viewModelScope.launch {
            try {
                val bitmap = planRepository.loadBitmap(plan.imagePath)
                val result = TacticalJsonCodec.parseAnalysisResult(plan.analysisJson)
                val markers = TacticalJsonCodec.parseManualMarkers(plan.markersJson)
                TacticalStateHolder.loadSavedPlan(bitmap, result, markers)
                onLoaded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved plan", e)
                TacticalStateHolder.setStatusBanner("Gagal memuat riwayat: ${e.localizedMessage}")
            }
        }
    }

    fun deleteSavedPlan(plan: WarPlanEntity) {
        viewModelScope.launch {
            planRepository.deletePlan(plan)
        }
    }

    fun selectStep(index: Int) = TacticalStateHolder.setActiveStepIndex(index)
    fun nextStep() = TacticalStateHolder.nextStep()
    fun prevStep() = TacticalStateHolder.prevStep()
    fun toggleSimulation() = TacticalStateHolder.toggleSimulation()
    fun setSelectedTool(tool: MarkerType) = TacticalStateHolder.setSelectedMarkerTool(tool)
    fun addTapMarker(x: Float, y: Float) = TacticalStateHolder.addTapMarker(x, y)
    fun addPathMarker(points: List<NormalizedPoint>) = TacticalStateHolder.addPathMarker(points)
    fun undoMarker() = TacticalStateHolder.undoMarker()
    fun clearMarkers() = TacticalStateHolder.clearMarkers()
    fun toggleShowAiPaths() = TacticalStateHolder.toggleShowAiPaths()
    fun toggleShowWeaknessHeatmap() = TacticalStateHolder.toggleShowWeaknessHeatmap()
    fun clearStatusBanner() = TacticalStateHolder.setStatusBanner(null)

    @Suppress("DEPRECATION")
    private suspend fun decodeBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(resolver, uri)
        }
    }
}
