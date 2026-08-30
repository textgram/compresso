package com.compresso.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.compresso.app.R
import com.compresso.app.data.model.CompressionMode
import com.compresso.app.data.model.CompressionStatus
import com.compresso.app.ui.components.DetailSheet
import com.compresso.app.ui.components.DropTargetContainer
import com.compresso.app.ui.components.EmptyState
import com.compresso.app.ui.components.FileQueueItem
import com.compresso.app.ui.components.StatsSummaryBar
import com.compresso.app.util.MediaTypeDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedResultTaskId by remember { mutableStateOf<String?>(null) }
    val savedMessage = stringResource(R.string.save_success)
    val saveFailedMessage = stringResource(R.string.save_failed)

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addFiles(uris)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePicker.launch(MediaTypeDetector.pickerMimeTypes()) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_files)) }
            )
        }
    ) { padding ->
        DropTargetContainer(
            onFilesDropped = { uris -> viewModel.addFiles(uris) }
        ) { isDragging ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.tasks.isEmpty()) {
                    EmptyState(isDragging = isDragging)
                } else {
                    QueueContent(
                        state = state,
                        onModeChange = viewModel::setMode,
                        onCompressAll = viewModel::startProcessing,
                        onClearCompleted = viewModel::clearCompleted,
                        onItemClick = { selectedResultTaskId = it },
                        onRetry = viewModel::retry,
                        onRemove = viewModel::remove
                    )

                    AnimatedVisibility(
                        visible = isDragging,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            EmptyState(isDragging = true)
                        }
                    }
                }
            }
        }
    }

    val selectedTask = state.tasks.firstOrNull { it.id == selectedResultTaskId }
    val selectedResult = selectedTask?.result
    if (selectedTask != null && selectedResult != null) {
        DetailSheet(
            result = selectedResult,
            displayName = selectedTask.displayName,
            onDismiss = { selectedResultTaskId = null },
            onShare = { context.startActivity(viewModel.buildShareIntent(selectedResult)) },
            onSave = {
                viewModel.saveToGallery(selectedResult) { success ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (success) savedMessage else saveFailedMessage
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun QueueContent(
    state: HomeUiState,
    onModeChange: (CompressionMode) -> Unit,
    onCompressAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onItemClick: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val hasCompleted = state.tasks.any {
        it.status == CompressionStatus.DONE || it.status == CompressionStatus.SKIPPED
    }
    val hasPending = state.tasks.any { it.status == CompressionStatus.PENDING }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.settings.mode == CompressionMode.STANDARD,
                onClick = { onModeChange(CompressionMode.STANDARD) },
                label = { Text(stringResource(R.string.mode_standard)) }
            )
            FilterChip(
                selected = state.settings.mode == CompressionMode.EXTREME,
                onClick = { onModeChange(CompressionMode.EXTREME) },
                label = { Text(stringResource(R.string.mode_extreme)) }
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StatsSummaryBar(stats = state.stats)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPending) {
                        TextButton(onClick = onCompressAll) {
                            Text(stringResource(R.string.home_start_all))
                        }
                    }
                    if (hasCompleted) {
                        TextButton(onClick = onClearCompleted) {
                            Text(stringResource(R.string.home_clear_completed))
                        }
                    }
                }
            }
            items(items = state.tasks, key = { it.id }) { task ->
                FileQueueItem(
                    task = task,
                    onClick = { onItemClick(task.id) },
                    onRetry = { onRetry(task.id) },
                    onRemove = { onRemove(task.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
