package com.compresso.app.compression

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.compresso.app.data.SettingsRepository
import com.compresso.app.data.StatsRepository
import com.compresso.app.data.TaskQueue
import com.compresso.app.data.model.CompressionStatus
import com.compresso.app.domain.compression.CompressionEngine
import com.compresso.app.domain.compression.EngineOutcome
import com.compresso.app.service.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompressionService : LifecycleService() {

    private lateinit var engine: CompressionEngine
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var statsRepository: StatsRepository

    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        engine = CompressionEngine(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)
        statsRepository = StatsRepository(applicationContext)
        NotificationHelper.ensureChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!isProcessing) {
            startForegroundInternal()
            processQueue()
        }
        return Service.START_NOT_STICKY
    }

    private fun startForegroundInternal() {
        val notification = NotificationHelper.buildProgressNotification(
            applicationContext,
            0,
            TaskQueue.pendingCount(),
            0
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
        }
    }

    private fun processQueue() {
        isProcessing = true
        lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val total = TaskQueue.pendingCount()
            var current = 0

            while (true) {
                val task = TaskQueue.nextPending() ?: break
                current++

                TaskQueue.update(task.id) {
                    it.copy(status = CompressionStatus.ANALYZING, progress = 0.15f)
                }
                updateNotification(current, total, 15)

                TaskQueue.update(task.id) {
                    it.copy(status = CompressionStatus.COMPRESSING, progress = 0.45f)
                }
                updateNotification(current, total, 45)

                val outcome = withContext(Dispatchers.Default) {
                    engine.process(
                        sourceUri = task.sourceUri,
                        displayName = task.displayName,
                        kind = task.kind,
                        mode = settings.mode,
                        preserveMetadata = settings.preserveMetadata,
                        deleteOriginalWhenDone = settings.deleteOriginalsWhenDone
                    )
                }

                TaskQueue.update(task.id) {
                    it.copy(status = CompressionStatus.VERIFYING, progress = 0.9f)
                }
                updateNotification(current, total, 90)

                when (outcome) {
                    is EngineOutcome.Success -> {
                        TaskQueue.update(task.id) {
                            it.copy(status = CompressionStatus.DONE, progress = 1f, result = outcome.result)
                        }
                        statsRepository.record(outcome.result.originalSize, outcome.result.compressedSize)
                    }

                    is EngineOutcome.Skipped -> {
                        TaskQueue.update(task.id) {
                            it.copy(status = CompressionStatus.SKIPPED, progress = 1f, statusDetail = outcome.reason)
                        }
                    }

                    is EngineOutcome.Failed -> {
                        TaskQueue.update(task.id) {
                            it.copy(status = CompressionStatus.FAILED, progress = 1f, errorMessage = outcome.reason)
                        }
                    }
                }
            }

            isProcessing = false
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildDoneNotification(applicationContext))
            stopForeground(Service.STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun updateNotification(current: Int, total: Int, progress: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildProgressNotification(applicationContext, current, total, progress)
        )
    }
}
