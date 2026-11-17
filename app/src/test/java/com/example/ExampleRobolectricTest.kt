package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MarkerType
import com.example.model.NormalizedPoint
import com.example.model.TacticalJsonCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WarStrat AI", appName)
    }

    @Test
    fun `parse tactical analysis json and roundtrip`() {
        val sampleJson = """
            {
              "baseArchetype": "Anti-3 Diamond",
              "estimatedTownHall": "TH15",
              "efficiencyScore": 91,
              "predictedStars": 3,
              "recommendedStrategyName": "Queen Charge Root Rider",
              "strategySummary": "Masuk dari sisi barat untuk memotong Eagle Artillery.",
              "weaknessSummary": "Air Defense dan Scattershot terlalu berdekatan.",
              "hardestPathWarning": "Hindari jalur utara karena dua Multi-Inferno + Monolith.",
              "siegeMachine": "Log Launcher",
              "armyComposition": [{"name": "Root Rider", "count": 8, "role": "Pembuka Tembok"}],
              "spellComposition": [{"name": "Rage", "count": 3, "role": "Inti Serangan"}],
              "weaknesses": [{
                "id": "w1",
                "label": "Cluster Scattershot",
                "defenseType": "Scattershot",
                "severity": "HIGH",
                "xNorm": 0.62,
                "yNorm": 0.41,
                "radiusNorm": 0.12,
                "explanation": "Rentan terhadap Freeze + Rage."
              }],
              "steps": [{
                "stepNumber": 1,
                "title": "Funneling Barat",
                "timingWindow": "0:00 - 0:30",
                "instruction": "Turunkan Queen dan Healer di sisi kiri.",
                "troopFocus": "Archer Queen + 5 Healer",
                "pathPoints": [{"x": 0.15, "y": 0.55}, {"x": 0.35, "y": 0.48}],
                "dropZones": [{"label": "Queen", "xNorm": 0.15, "yNorm": 0.55}],
                "spellZones": [{"spellName": "Rage", "xNorm": 0.3, "yNorm": 0.5, "radiusNorm": 0.1}]
              }]
            }
        """.trimIndent()

        val parsed = TacticalJsonCodec.parseAnalysisResult(sampleJson)
        assertEquals("TH15", parsed.estimatedTownHall)
        assertEquals(91, parsed.efficiencyScore)
        assertEquals(1, parsed.weaknesses.size)
        assertEquals(1, parsed.steps.size)
        assertTrue(parsed.steps.first().pathPoints.size == 2)

        val reEncoded = TacticalJsonCodec.encodeAnalysisResult(parsed)
        val reparsed = TacticalJsonCodec.parseAnalysisResult(reEncoded)
        assertEquals(parsed.recommendedStrategyName, reparsed.recommendedStrategyName)
    }

    @Test
    fun `roundtrip manual overlay markers`() {
        val markers = listOf(
            com.example.model.ManualMarker(
                id = "m1",
                type = MarkerType.PATH_ARROW,
                xNorm = 0.2f,
                yNorm = 0.3f,
                points = listOf(NormalizedPoint(0.2f, 0.3f), NormalizedPoint(0.7f, 0.6f)),
                label = "Jalur"
            )
        )
        val encoded = TacticalJsonCodec.encodeManualMarkers(markers)
        val decoded = TacticalJsonCodec.parseManualMarkers(encoded)
        assertEquals(1, decoded.size)
        assertEquals(MarkerType.PATH_ARROW, decoded.first().type)
        assertEquals(2, decoded.first().points.size)
    }
}
