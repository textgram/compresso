package com.compresso.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AggregateStats(
    val totalOriginalBytes: Long = 0,
    val totalCompressedBytes: Long = 0,
    val filesProcessed: Int = 0,
    val bestRatio: Float = 0f
)

class StatsRepository(context: Context) {

    private val file = File(context.filesDir, "stats.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<AggregateStats> = _stats.asStateFlow()

    private fun load(): AggregateStats {
        return try {
            if (file.exists()) {
                json.decodeFromString(AggregateStats.serializer(), file.readText())
            } else {
                AggregateStats()
            }
        } catch (e: Exception) {
            AggregateStats()
        }
    }

    private fun persist(stats: AggregateStats) {
        try {
            file.writeText(json.encodeToString(stats))
        } catch (e: Exception) {
        }
    }

    fun record(originalBytes: Long, compressedBytes: Long) {
        val current = _stats.value
        val ratio = if (originalBytes > 0) {
            (originalBytes - compressedBytes).toFloat() / originalBytes.toFloat()
        } else {
            0f
        }
        val updated = current.copy(
            totalOriginalBytes = current.totalOriginalBytes + originalBytes,
            totalCompressedBytes = current.totalCompressedBytes + compressedBytes,
            filesProcessed = current.filesProcessed + 1,
            bestRatio = maxOf(current.bestRatio, ratio)
        )
        _stats.value = updated
        persist(updated)
    }

    fun reset() {
        val fresh = AggregateStats()
        _stats.value = fresh
        persist(fresh)
    }
}
