package com.compresso.app.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            String.format(Locale.US, "%d %s", bytes, units[0])
        } else {
            String.format(Locale.US, "%.2f %s", value, units[digitGroups])
        }
    }

    fun formatPercent(ratio: Float): String {
        val clamped = ratio.coerceIn(-9.99f, 9.99f)
        return String.format(Locale.US, "%.1f%%", clamped * 100f)
    }

    fun formatDuration(millis: Long): String {
        if (millis < 1000) return "${millis} ms"
        val totalSeconds = millis / 1000.0
        if (totalSeconds < 60) return String.format(Locale.US, "%.1f s", totalSeconds)
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        return String.format(Locale.US, "%dm %ds", minutes, seconds)
    }

    fun formatBytesCompact(bytes: Long): String {
        val sign = if (bytes < 0) "-" else ""
        return sign + formatBytes(abs(bytes))
    }
}
