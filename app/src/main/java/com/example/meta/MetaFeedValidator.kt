package com.example.meta

import org.json.JSONObject

/** Reject invalid or misleading remote strategy data before caching. */
object MetaFeedValidator {
    fun validate(payload: JSONObject) {
        require(payload.optInt("schema", -1) == 1) { "Unsupported meta schema" }
        val version = payload.optString("version")
        require(version.isNotBlank() && version.length <= 80) { "Invalid meta version" }
        val strategies = payload.optJSONArray("strategies") ?: error("Missing strategies")
        require(strategies.length() <= 200) { "Too many strategies" }
        val ids = mutableSetOf<String>()
        for (i in 0 until strategies.length()) {
            val item = strategies.optJSONObject(i) ?: error("Invalid strategy entry")
            val id = item.optString("id")
            require(id.matches(Regex("[a-zA-Z0-9_-]{1,64}")) && ids.add(id)) { "Invalid or duplicate strategy ID" }
            val min = item.optInt("th_min", -1)
            val max = item.optInt("th_max", -1)
            require(min in 1..30 && max in min..30) { "Invalid Town Hall range" }
            require(item.optString("name").length in 1..120) { "Invalid strategy name" }
            val status = item.optString("status")
            require(status in setOf("example-unverified", "verified", "experimental")) { "Missing strategy verification status" }
            val guidance = item.optJSONArray("guidance") ?: error("Missing guidance")
            require(guidance.length() in 1..30) { "Invalid guidance count" }
            for (j in 0 until guidance.length()) {
                val line = guidance.optString(j)
                require(line.length in 1..400) { "Invalid guidance" }
            }
        }
    }
}
