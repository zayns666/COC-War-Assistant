package com.example.meta

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Optional public HTTPS JSON feed. No credential required. Caller must configure a trusted URL. */
class MetaUpdater(private val context: Context) {
    private val prefs = context.getSharedPreferences("zayns_meta", Context.MODE_PRIVATE)

    fun cached(): JSONObject = try {
        JSONObject(prefs.getString("payload", null) ?: context.assets.open("meta/strategies.json").bufferedReader().use { it.readText() })
    } catch (_: Exception) { JSONObject("{\"schema\":1,\"strategies\":[]}") }

    fun lastSyncMillis(): Long = prefs.getLong("last_sync", 0L)

    suspend fun refresh(feedUrl: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            require(feedUrl.startsWith("https://")) { "HTTPS required" }
            val conn = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
            }
            try {
                require(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
                require(conn.contentLengthLong <= 512_000 || conn.contentLengthLong < 0) { "Feed too large" }
                val bytes = conn.inputStream.use { it.readNBytes(512_001) }
                require(bytes.size <= 512_000) { "Feed too large" }
                val payload = JSONObject(String(bytes, Charsets.UTF_8))
                require(payload.optInt("schema") == 1) { "Unsupported schema" }
                require(payload.optJSONArray("strategies") != null) { "Missing strategies" }
                prefs.edit().putString("payload", payload.toString()).putLong("last_sync", System.currentTimeMillis()).apply()
                payload
            } finally { conn.disconnect() }
        }
    }
}
