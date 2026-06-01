package com.gusogst.chat.data.memory

class MemoryManager {

    data class MemoryStats(
        val totalMemories: Int = 0,
        val estimatedSizeMB: Double = 0.0,
        val compressionRatio: Double = 0.85,
        val storagePath: String = "hermes_db",
        val lastCompaction: String = "N/A",
    )

    fun getStats(): MemoryStats = MemoryStats()

    fun exportMemories(): String = "No memories found"

    fun clear() {}
}
