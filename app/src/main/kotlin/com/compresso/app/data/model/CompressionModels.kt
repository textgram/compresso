package com.compresso.app.data.model

import android.net.Uri
import java.util.UUID

enum class MediaKind {
    IMAGE,
    VIDEO,
    UNKNOWN
}

enum class CompressionStatus {
    PENDING,
    ANALYZING,
    COMPRESSING,
    VERIFYING,
    DONE,
    SKIPPED,
    FAILED,
    CANCELLED
}

enum class CompressionMode {
    STANDARD,
    EXTREME
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class CompressionSettings(
    val mode: CompressionMode = CompressionMode.STANDARD,
    val preserveMetadata: Boolean = true,
    val allowFormatConversion: Boolean = true,
    val deleteOriginalsWhenDone: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM
)

data class CompressionResult(
    val outputUri: Uri,
    val outputDisplayName: String,
    val originalSize: Long,
    val compressedSize: Long,
    val codec: String,
    val container: String,
    val method: String,
    val durationMs: Long,
    val verified: Boolean,
    val metadataPreserved: Boolean,
    val resolution: String? = null,
    val colorDepth: String? = null
) {
    val bytesSaved: Long get() = (originalSize - compressedSize).coerceAtLeast(0)
    val ratio: Float get() = if (originalSize <= 0) 0f else bytesSaved.toFloat() / originalSize.toFloat()
}

data class CompressionTask(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val originalSize: Long,
    val status: CompressionStatus = CompressionStatus.PENDING,
    val progress: Float = 0f,
    val statusDetail: String? = null,
    val result: CompressionResult? = null,
    val errorMessage: String? = null,
    val addedAtMillis: Long = System.currentTimeMillis()
)
