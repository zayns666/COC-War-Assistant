package com.example.ai

import android.graphics.Bitmap
import com.example.model.TacticalAnalysisResult

/** No-key mode: never invent tactical results when no vision model exists. */
class BaseAnalysisRepository {
    suspend fun analyzeBase(bitmap: Bitmap): TacticalAnalysisResult {
        require(!bitmap.isRecycled) { "Gambar tidak tersedia." }
        throw UnsupportedOperationException(
            "Mode tanpa API key aktif. Model AI vision lokal belum terpasang. " +
            "Gunakan penandaan manual pada Peta Taktis sementara. " +
            "Analisis otomatis memerlukan model lokal yang telah diuji."
        )
    }
}
