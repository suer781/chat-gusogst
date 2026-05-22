package com.gusogst.chat.util

import android.content.Context
import org.json.JSONObject

class SnapshotStorage(context: Context) {
    private val prefs = context.getSharedPreferences("persona_snapshots", Context.MODE_PRIVATE)

    data class EditSnapshot(val prompt: String, val temperature: Float, val topP: Float, val maxTokens: Int, val overrideGlobal: Boolean, val autoMode: String, val timestamp: Long)

    fun save(personaId: String, snap: EditSnapshot) {
        val json = JSONObject().apply {
            put("prompt", snap.prompt); put("temperature", snap.temperature.toDouble()); put("topP", snap.topP.toDouble())
            put("maxTokens", snap.maxTokens); put("overrideGlobal", snap.overrideGlobal); put("autoMode", snap.autoMode); put("timestamp", snap.timestamp)
        }
        prefs.edit().putString("snapshot_$personaId", json.toString()).apply()
    }

    fun load(personaId: String): EditSnapshot? {
        val str = prefs.getString("snapshot_$personaId", null) ?: return null
        return try {
            val j = JSONObject(str)
            EditSnapshot(j.getString("prompt"), j.getDouble("temperature").toFloat(), j.getDouble("topP").toFloat(), j.getInt("maxTokens"), j.getBoolean("overrideGlobal"), j.getString("autoMode"), j.getLong("timestamp"))
        } catch (_: Exception) { null }
    }

    fun clear(personaId: String) { prefs.edit().remove("snapshot_$personaId").apply() }
}
