package com.compresso.app.data

import com.compresso.app.data.model.CompressionStatus
import com.compresso.app.data.model.CompressionTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object TaskQueue {

    private val lock = ReentrantLock()
    private val _tasks = MutableStateFlow<List<CompressionTask>>(emptyList())
    val tasks: StateFlow<List<CompressionTask>> = _tasks.asStateFlow()

    fun addAll(newTasks: List<CompressionTask>) {
        lock.withLock {
            _tasks.value = _tasks.value + newTasks
        }
    }

    fun remove(id: String) {
        lock.withLock {
            _tasks.value = _tasks.value.filterNot { it.id == id }
        }
    }

    fun clearCompleted() {
        lock.withLock {
            _tasks.value = _tasks.value.filterNot {
                it.status == CompressionStatus.DONE || it.status == CompressionStatus.SKIPPED
            }
        }
    }

    fun update(id: String, transform: (CompressionTask) -> CompressionTask) {
        lock.withLock {
            _tasks.value = _tasks.value.map { if (it.id == id) transform(it) else it }
        }
    }

    fun nextPending(): CompressionTask? {
        lock.withLock {
            return _tasks.value.firstOrNull { it.status == CompressionStatus.PENDING }
        }
    }

    fun pendingCount(): Int {
        lock.withLock {
            return _tasks.value.count { it.status == CompressionStatus.PENDING }
        }
    }

    fun retry(id: String) {
        lock.withLock {
            _tasks.value = _tasks.value.map {
                if (it.id == id) {
                    it.copy(status = CompressionStatus.PENDING, progress = 0f, errorMessage = null, statusDetail = null)
                } else {
                    it
                }
            }
        }
    }

    fun cancelPending() {
        lock.withLock {
            _tasks.value = _tasks.value.map {
                if (it.status == CompressionStatus.PENDING) it.copy(status = CompressionStatus.CANCELLED) else it
            }
        }
    }
}
