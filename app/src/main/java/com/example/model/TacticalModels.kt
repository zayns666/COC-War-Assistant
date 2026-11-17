package com.example.model

enum class WeaknessSeverity {
    HIGH,
    MEDIUM,
    LOW
}

enum class MarkerType(val labelId: String) {
    TROOP_DROP("Pasukan"),
    SPELL_ZONE("Spell"),
    PRIORITY_TARGET("Target"),
    PATH_ARROW("Jalur")
}

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class ArmyUnit(
    val name: String,
    val count: Int,
    val role: String
)

data class BaseWeakness(
    val id: String,
    val label: String,
    val defenseType: String,
    val severity: WeaknessSeverity,
    val xNorm: Float,
    val yNorm: Float,
    val radiusNorm: Float,
    val explanation: String
)

data class DropMarker(
    val label: String,
    val xNorm: Float,
    val yNorm: Float
)

data class SpellMarker(
    val spellName: String,
    val xNorm: Float,
    val yNorm: Float,
    val radiusNorm: Float
)

data class AttackStep(
    val stepNumber: Int,
    val title: String,
    val timingWindow: String,
    val instruction: String,
    val troopFocus: String,
    val pathPoints: List<NormalizedPoint>,
    val dropZones: List<DropMarker>,
    val spellZones: List<SpellMarker>
)

data class TacticalAnalysisResult(
    val baseArchetype: String,
    val estimatedTownHall: String,
    val efficiencyScore: Int,
    val predictedStars: Int,
    val recommendedStrategyName: String,
    val strategySummary: String,
    val weaknessSummary: String,
    val hardestPathWarning: String,
    val siegeMachine: String,
    val armyComposition: List<ArmyUnit>,
    val spellComposition: List<ArmyUnit>,
    val weaknesses: List<BaseWeakness>,
    val steps: List<AttackStep>
)

data class ManualMarker(
    val id: String,
    val type: MarkerType,
    val xNorm: Float,
    val yNorm: Float,
    val points: List<NormalizedPoint> = emptyList(),
    val label: String = ""
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val result: TacticalAnalysisResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}
