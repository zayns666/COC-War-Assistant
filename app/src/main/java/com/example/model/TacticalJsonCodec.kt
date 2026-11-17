package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object TacticalJsonCodec {

    fun parseAnalysisResult(jsonString: String): TacticalAnalysisResult {
        val root = JSONObject(jsonString)

        val armyList = parseArmyUnits(root.optJSONArray("armyComposition"))
        val spellList = parseArmyUnits(root.optJSONArray("spellComposition"))
        val weaknesses = parseWeaknesses(root.optJSONArray("weaknesses"))
        val steps = parseSteps(root.optJSONArray("steps"))

        return TacticalAnalysisResult(
            baseArchetype = root.optString("baseArchetype", "Base Taktis Clan War"),
            estimatedTownHall = root.optString("estimatedTownHall", "TH14+"),
            efficiencyScore = root.optInt("efficiencyScore", 85).coerceIn(1, 100),
            predictedStars = root.optInt("predictedStars", 3).coerceIn(1, 3),
            recommendedStrategyName = root.optString("recommendedStrategyName", "Serangan Hybrid Terarah"),
            strategySummary = root.optString("strategySummary", ""),
            weaknessSummary = root.optString("weaknessSummary", ""),
            hardestPathWarning = root.optString("hardestPathWarning", ""),
            siegeMachine = root.optString("siegeMachine", "Log Launcher"),
            armyComposition = armyList,
            spellComposition = spellList,
            weaknesses = weaknesses,
            steps = steps
        )
    }

    fun encodeAnalysisResult(result: TacticalAnalysisResult): String {
        val root = JSONObject().apply {
            put("baseArchetype", result.baseArchetype)
            put("estimatedTownHall", result.estimatedTownHall)
            put("efficiencyScore", result.efficiencyScore)
            put("predictedStars", result.predictedStars)
            put("recommendedStrategyName", result.recommendedStrategyName)
            put("strategySummary", result.strategySummary)
            put("weaknessSummary", result.weaknessSummary)
            put("hardestPathWarning", result.hardestPathWarning)
            put("siegeMachine", result.siegeMachine)
            put("armyComposition", encodeArmyUnits(result.armyComposition))
            put("spellComposition", encodeArmyUnits(result.spellComposition))
            put("weaknesses", encodeWeaknesses(result.weaknesses))
            put("steps", encodeSteps(result.steps))
        }
        return root.toString()
    }

    fun parseManualMarkers(jsonString: String): List<ManualMarker> {
        if (jsonString.isBlank()) return emptyList()
        val array = JSONArray(jsonString)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val typeName = obj.optString("type", MarkerType.TROOP_DROP.name)
            val type = MarkerType.entries.firstOrNull { it.name == typeName } ?: MarkerType.TROOP_DROP
            val pts = parsePoints(obj.optJSONArray("points"))
            ManualMarker(
                id = obj.optString("id", "m_$i"),
                type = type,
                xNorm = obj.optDouble("xNorm", 0.5).toFloat().coerceIn(0f, 1f),
                yNorm = obj.optDouble("yNorm", 0.5).toFloat().coerceIn(0f, 1f),
                points = pts,
                label = obj.optString("label", type.labelId)
            )
        }
    }

    fun encodeManualMarkers(markers: List<ManualMarker>): String {
        val array = JSONArray()
        markers.forEach { marker ->
            val obj = JSONObject().apply {
                put("id", marker.id)
                put("type", marker.type.name)
                put("xNorm", marker.xNorm.toDouble())
                put("yNorm", marker.yNorm.toDouble())
                put("label", marker.label)
                put("points", JSONArray().apply {
                    marker.points.forEach { pt ->
                        put(JSONObject().apply {
                            put("x", pt.x.toDouble())
                            put("y", pt.y.toDouble())
                        })
                    }
                })
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseArmyUnits(array: JSONArray?): List<ArmyUnit> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            ArmyUnit(
                name = obj.optString("name", "Unit"),
                count = obj.optInt("count", 1),
                role = obj.optString("role", "")
            )
        }
    }

    private fun encodeArmyUnits(units: List<ArmyUnit>): JSONArray {
        val array = JSONArray()
        units.forEach { u ->
            array.put(JSONObject().apply {
                put("name", u.name)
                put("count", u.count)
                put("role", u.role)
            })
        }
        return array
    }

    private fun parseWeaknesses(array: JSONArray?): List<BaseWeakness> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val sevStr = obj.optString("severity", "HIGH").uppercase()
            val severity = when (sevStr) {
                "HIGH" -> WeaknessSeverity.HIGH
                "MEDIUM" -> WeaknessSeverity.MEDIUM
                else -> WeaknessSeverity.LOW
            }
            BaseWeakness(
                id = obj.optString("id", "w_$i"),
                label = obj.optString("label", "Titik Lemah"),
                defenseType = obj.optString("defenseType", "Pertahanan"),
                severity = severity,
                xNorm = obj.optDouble("xNorm", 0.5).toFloat().coerceIn(0.04f, 0.96f),
                yNorm = obj.optDouble("yNorm", 0.5).toFloat().coerceIn(0.04f, 0.96f),
                radiusNorm = obj.optDouble("radiusNorm", 0.11).toFloat().coerceIn(0.05f, 0.25f),
                explanation = obj.optString("explanation", "")
            )
        }
    }

    private fun encodeWeaknesses(weaknesses: List<BaseWeakness>): JSONArray {
        val array = JSONArray()
        weaknesses.forEach { w ->
            array.put(JSONObject().apply {
                put("id", w.id)
                put("label", w.label)
                put("defenseType", w.defenseType)
                put("severity", w.severity.name)
                put("xNorm", w.xNorm.toDouble())
                put("yNorm", w.yNorm.toDouble())
                put("radiusNorm", w.radiusNorm.toDouble())
                put("explanation", w.explanation)
            })
        }
        return array
    }

    private fun parseSteps(array: JSONArray?): List<AttackStep> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapIndexedNotNull { idx, i ->
            val obj = array.optJSONObject(i) ?: return@mapIndexedNotNull null
            AttackStep(
                stepNumber = obj.optInt("stepNumber", idx + 1),
                title = obj.optString("title", "Fase ${idx + 1}"),
                timingWindow = obj.optString("timingWindow", "0:00 - 0:45"),
                instruction = obj.optString("instruction", ""),
                troopFocus = obj.optString("troopFocus", ""),
                pathPoints = parsePoints(obj.optJSONArray("pathPoints")),
                dropZones = parseDropZones(obj.optJSONArray("dropZones")),
                spellZones = parseSpellZones(obj.optJSONArray("spellZones"))
            )
        }
    }

    private fun encodeSteps(steps: List<AttackStep>): JSONArray {
        val array = JSONArray()
        steps.forEach { s ->
            array.put(JSONObject().apply {
                put("stepNumber", s.stepNumber)
                put("title", s.title)
                put("timingWindow", s.timingWindow)
                put("instruction", s.instruction)
                put("troopFocus", s.troopFocus)
                put("pathPoints", JSONArray().apply {
                    s.pathPoints.forEach { pt ->
                        put(JSONObject().apply {
                            put("x", pt.x.toDouble())
                            put("y", pt.y.toDouble())
                        })
                    }
                })
                put("dropZones", JSONArray().apply {
                    s.dropZones.forEach { d ->
                        put(JSONObject().apply {
                            put("label", d.label)
                            put("xNorm", d.xNorm.toDouble())
                            put("yNorm", d.yNorm.toDouble())
                        })
                    }
                })
                put("spellZones", JSONArray().apply {
                    s.spellZones.forEach { sp ->
                        put(JSONObject().apply {
                            put("spellName", sp.spellName)
                            put("xNorm", sp.xNorm.toDouble())
                            put("yNorm", sp.yNorm.toDouble())
                            put("radiusNorm", sp.radiusNorm.toDouble())
                        })
                    }
                })
            })
        }
        return array
    }

    private fun parsePoints(array: JSONArray?): List<NormalizedPoint> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            NormalizedPoint(
                x = obj.optDouble("x", 0.5).toFloat().coerceIn(0.02f, 0.98f),
                y = obj.optDouble("y", 0.5).toFloat().coerceIn(0.02f, 0.98f)
            )
        }
    }

    private fun parseDropZones(array: JSONArray?): List<DropMarker> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            DropMarker(
                label = obj.optString("label", "Drop"),
                xNorm = obj.optDouble("xNorm", 0.5).toFloat().coerceIn(0.03f, 0.97f),
                yNorm = obj.optDouble("yNorm", 0.5).toFloat().coerceIn(0.03f, 0.97f)
            )
        }
    }

    private fun parseSpellZones(array: JSONArray?): List<SpellMarker> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            SpellMarker(
                spellName = obj.optString("spellName", "Rage"),
                xNorm = obj.optDouble("xNorm", 0.5).toFloat().coerceIn(0.05f, 0.95f),
                yNorm = obj.optDouble("yNorm", 0.5).toFloat().coerceIn(0.05f, 0.95f),
                radiusNorm = obj.optDouble("radiusNorm", 0.09).toFloat().coerceIn(0.04f, 0.22f)
            )
        }
    }
}
