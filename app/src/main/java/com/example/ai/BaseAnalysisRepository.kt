package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.TacticalAnalysisResult
import com.example.model.TacticalJsonCodec
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "BaseAnalysisRepo"

class BaseAnalysisRepository {

    private val pointSchema = Schema.obj(
        properties = mapOf(
            "x" to Schema.double(description = "Normalized horizontal coordinate from 0.0 (left) to 1.0 (right)"),
            "y" to Schema.double(description = "Normalized vertical coordinate from 0.0 (top) to 1.0 (bottom)")
        )
    )

    private val armyUnitSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Troop or spell name"),
            "count" to Schema.integer(description = "Recommended unit count"),
            "role" to Schema.string(description = "Short tactical role in Indonesian")
        )
    )

    private val weaknessSchema = Schema.obj(
        properties = mapOf(
            "id" to Schema.string(description = "Unique identifier"),
            "label" to Schema.string(description = "Short label of weakness or threat in Indonesian"),
            "defenseType" to Schema.string(description = "Key defense or gap e.g. Eagle Artillery, Inferno, Air Defense"),
            "severity" to Schema.enumeration(values = listOf("HIGH", "MEDIUM", "LOW")),
            "xNorm" to Schema.double(description = "Normalized X coordinate (0.05..0.95) on base image"),
            "yNorm" to Schema.double(description = "Normalized Y coordinate (0.05..0.95) on base image"),
            "radiusNorm" to Schema.double(description = "Normalized threat/weakness radius (0.06..0.22)"),
            "explanation" to Schema.string(description = "Tactical explanation in Indonesian")
        )
    )

    private val dropZoneSchema = Schema.obj(
        properties = mapOf(
            "label" to Schema.string(description = "Short troop drop marker label"),
            "xNorm" to Schema.double(description = "Normalized X coordinate (0.05..0.95)"),
            "yNorm" to Schema.double(description = "Normalized Y coordinate (0.05..0.95)")
        )
    )

    private val spellZoneSchema = Schema.obj(
        properties = mapOf(
            "spellName" to Schema.string(description = "Spell name e.g. Rage, Freeze, Jump, Invisibility"),
            "xNorm" to Schema.double(description = "Normalized X coordinate (0.05..0.95)"),
            "yNorm" to Schema.double(description = "Normalized Y coordinate (0.05..0.95)"),
            "radiusNorm" to Schema.double(description = "Normalized spell radius (0.05..0.18)")
        )
    )

    private val stepSchema = Schema.obj(
        properties = mapOf(
            "stepNumber" to Schema.integer(description = "Sequential step number starting at 1"),
            "title" to Schema.string(description = "Short step title in Indonesian"),
            "timingWindow" to Schema.string(description = "Estimated attack timer window e.g. 0:00 - 0:35"),
            "instruction" to Schema.string(description = "Detailed step-by-step tactical execution in Indonesian"),
            "troopFocus" to Schema.string(description = "Primary troops/heroes deployed in this phase"),
            "pathPoints" to Schema.array(
                items = pointSchema,
                description = "Ordered list of 3 to 5 normalized (x,y) coordinates showing troop movement trajectory on the base"
            ),
            "dropZones" to Schema.array(
                items = dropZoneSchema,
                description = "1 to 3 deployment drop points for this phase"
            ),
            "spellZones" to Schema.array(
                items = spellZoneSchema,
                description = "0 to 2 spell placement circles for this phase"
            )
        )
    )

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "baseArchetype" to Schema.string(description = "Base layout archetype in Indonesian"),
            "estimatedTownHall" to Schema.string(description = "Estimated Town Hall tier e.g. TH14 / TH15"),
            "efficiencyScore" to Schema.integer(description = "Predicted attack efficiency score 1..100"),
            "predictedStars" to Schema.integer(description = "Predicted war stars 1..3"),
            "recommendedStrategyName" to Schema.string(description = "Best attack army strategy name"),
            "strategySummary" to Schema.string(description = "Concise overview of why this strategy wins in Indonesian"),
            "weaknessSummary" to Schema.string(description = "Automatic weakness summary in Indonesian"),
            "hardestPathWarning" to Schema.string(description = "Prediction of the hardest high-DPS defensive corridor and how the optimal path overcomes/bypasses it in Indonesian"),
            "siegeMachine" to Schema.string(description = "Recommended Siege Machine"),
            "armyComposition" to Schema.array(items = armyUnitSchema),
            "spellComposition" to Schema.array(items = armyUnitSchema),
            "weaknesses" to Schema.array(
                items = weaknessSchema,
                description = "3 to 5 detected base weaknesses and high-threat defense clusters with accurate image coordinates"
            ),
            "steps" to Schema.array(
                items = stepSchema,
                description = "4 sequential attack phases with coordinates for troop simulation paths, drop zones, and spell circles"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = analysisSchema
                temperature = 0.35f
            }
        )
    }

    suspend fun analyzeBase(bitmap: Bitmap): TacticalAnalysisResult {
        val preparedBitmap = withContext(Dispatchers.Default) {
            scaleBitmapForInference(bitmap, maxDim = 1280)
        }

        val promptText = """
            Kamu adalah analis strategi Clan War Clash of Clans (COC) tingkat esports profesional dan pakar machine learning pathing.
            Analisis gambar peta base lawan ini secara mendetail dan hasilkan rencana serangan terbaik untuk meraih 3 Bintang dengan efisiensi maksimal.

            ATURAN PENTING KOORDINAT VISUAL:
            - Semua koordinat (x, y / xNorm, yNorm) HARUS dalam skala ter-normalisasi 0.0 sampai 1.0 relatif terhadap gambar base yang diberikan (0.0 = kiri/atas, 1.0 = kanan/bawah).
            - Amati posisi bangunan pertahanan utama pada gambar (Town Hall, Eagle Artillery, Inferno Tower, Scattershot, Air Defense, X-Bow, celah tembok, kompartemen luar) agar titik kelemahan (weaknesses), dropZones, spellZones, dan pathPoints menempel akurat di atas peta base!
            - Berikan 3 sampai 5 titik kelemahan / ancaman utama pada `weaknesses` (misalnya celah funneling, pertahanan udara terbuka, cluster splash damage, atau jalur masuk terlemah).
            - Pada `hardestPathWarning`, jelaskan prediksi jalur tersulit (area DPS tertinggi / jebakan / multi-inferno) dan bagaimana rute optimal yang diprediksi ini menghancurkan inti pertahanan dengan efisiensi tinggi.
            - Berikan tepat 4 fase serangan berurutan (`steps`) dari Step 1 (Funneling / Queen Charge / Blimp) hingga Step 4 (Cleanup), masing-masing memiliki 3-5 titik `pathPoints` yang membentuk jalur pergerakan pasukan yang realistis di atas peta base, lengkap dengan `dropZones` dan `spellZones`.
            - Gunakan Bahasa Indonesia taktis yang jelas, ringkas, dan siap eksekusi saat war.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(preparedBitmap)
                        text(promptText)
                    }
                )
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Model AI mengembalikan respons kosong.")
            TacticalJsonCodec.parseAnalysisResult(jsonText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Base analysis failed", e)
            throw e
        }
    }

    private fun scaleBitmapForInference(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
