package com.compresso.app.ui.screens.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compresso.app.compression.CompressionService
import com.compresso.app.data.AggregateStats
import com.compresso.app.data.SettingsRepository
import com.compresso.app.data.StatsRepository
import com.compresso.app.data.TaskQueue
import com.compresso.app.data.model.CompressionMode
import com.compresso.app.data.model.CompressionSettings
import com.compresso.app.data.model.CompressionTask
import com.compresso.app.data.model.MediaKind
import com.compresso.app.data.model.ThemePreference
import com.compresso.app.util.FileUtils
import com.compresso.app.util.MediaTypeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val tasks: List<CompressionTask> = emptyList(),
    val settings: CompressionSettings = CompressionSettings(),
    val stats: AggregateStats = AggregateStats()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val statsRepository = StatsRepository(application)

    val uiState = combine(
        TaskQueue.tasks,
        settingsRepository.settingsFlow,
        statsRepository.stats
    ) { tasks, settings, stats ->
        HomeUiState(tasks = tasks, settings = settings, stats = stats)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val resolver = context.contentResolver
            val newTasks = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) {
                    }
                    val info = FileUtils.queryDocumentInfo(resolver, uri)
                    val mime = MediaTypeDetector.resolveMimeType(resolver, uri, null)
                    val kind = MediaTypeDetector.kindOf(mime)
                    if (kind == MediaKind.UNKNOWN) {
                        null
                    } else {
                        CompressionTask(
                            sourceUri = uri,
                            displayName = info.displayName,
                            mimeType = mime,
                            kind = kind,
                            originalSize = info.size
                        )
                    }
                }
            }
            if (newTasks.isNotEmpty()) {
                TaskQueue.addAll(newTasks)
            }
        }
    }

    fun startProcessing() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, Intent(context, CompressionService::class.java))
    }

    fun retry(id: String) {
        TaskQueue.retry(id)
        startProcessing()
    }

    fun remove(id: String) {
        TaskQueue.remove(id)
    }

    fun clearCompleted() {
        TaskQueue.clearCompleted()
    }

    fun setMode(mode: CompressionMode) {
        viewModelScope.launch { settingsRepository.setMode(mode) }
    }

    fun setPreserveMetadata(value: Boolean) {
        viewModelScope.launch { settingsRepository.setPreserveMetadata(value) }
    }

    fun setAllowFormatConversion(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAllowFormatConversion(value) }
    }

    fun setDeleteOriginalsWhenDone(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDeleteOriginalsWhenDone(value) }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun resetStats() {
        statsRepository.reset()
    }

    fun mimeTypeFor(container: String): String {
        return when (container) {
            "PNG" -> "image/png"
            "WebP" -> "image/webp"
            "MP4" -> "video/mp4"
            "WebM" -> "video/webm"
            else -> "application/octet-stream"
        }
    }

    fun saveToGallery(result: com.compresso.app.data.model.CompressionResult, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val context = getApplication<Application>()
                    val resolver = context.contentResolver
                    val isVideo = result.container == "MP4" || result.container == "WebM"
                    val collection = if (isVideo) {
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, result.outputDisplayName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeTypeFor(result.container))
                        put(
                            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                            (if (isVideo) android.os.Environment.DIRECTORY_MOVIES else android.os.Environment.DIRECTORY_PICTURES) + "/Compresso"
                        )
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(collection, values) ?: return@withContext false
                    resolver.openOutputStream(uri)?.use { out ->
                        resolver.openInputStream(result.outputUri)?.use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            onDone(success)
        }
    }

    fun buildShareIntent(result: com.compresso.app.data.model.CompressionResult): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeTypeFor(result.container)
            putExtra(Intent.EXTRA_STREAM, result.outputUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, null)
    }
}
