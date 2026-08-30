package com.compresso.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.compresso.app.R
import com.compresso.app.data.model.CompressionStatus
import com.compresso.app.data.model.CompressionTask
import com.compresso.app.util.FormatUtils

@Composable
fun FileQueueItem(
    task: CompressionTask,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = task.result?.ratio ?: 0f
    val animatedRatio by animateFloatAsState(
        targetValue = if (task.status == CompressionStatus.DONE) ratio.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(600),
        label = "ratio"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = task.status == CompressionStatus.DONE, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(fraction = if (task.status == CompressionStatus.DONE) animatedRatio.coerceIn(0.04f, 1f) else 0.001f)
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusLine(task = task)
            }

            Spacer(modifier = Modifier.width(8.dp))

            TrailingAction(task = task, onRetry = onRetry, onRemove = onRemove)
        }
    }
}

@Composable
private fun StatusLine(task: CompressionTask) {
    when (task.status) {
        CompressionStatus.DONE -> {
            val result = task.result
            if (result != null) {
                Text(
                    text = "${FormatUtils.formatBytes(result.originalSize)} \u2192 ${FormatUtils.formatBytes(result.compressedSize)} (${FormatUtils.formatPercent(result.ratio)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        CompressionStatus.FAILED -> {
            Text(
                text = task.errorMessage ?: stringResource(R.string.error_generic),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        CompressionStatus.SKIPPED -> {
            Text(
                text = task.statusDetail ?: stringResource(R.string.error_no_gain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        else -> {
            Text(
                text = "${statusLabel(task.status)} \u00b7 ${FormatUtils.formatBytes(task.originalSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun statusLabel(status: CompressionStatus): String {
    return when (status) {
        CompressionStatus.PENDING -> stringResource(R.string.status_pending)
        CompressionStatus.ANALYZING -> stringResource(R.string.status_analyzing)
        CompressionStatus.COMPRESSING -> stringResource(R.string.status_compressing)
        CompressionStatus.VERIFYING -> stringResource(R.string.status_verifying)
        CompressionStatus.CANCELLED -> stringResource(R.string.status_cancelled)
        CompressionStatus.DONE -> stringResource(R.string.status_done)
        CompressionStatus.SKIPPED -> stringResource(R.string.status_skipped)
        CompressionStatus.FAILED -> stringResource(R.string.status_failed)
    }
}

@Composable
private fun TrailingAction(task: CompressionTask, onRetry: () -> Unit, onRemove: () -> Unit) {
    AnimatedContent(
        targetState = task.status,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
        label = "trailing"
    ) { status ->
        when (status) {
            CompressionStatus.DONE -> Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.cd_status_icon),
                tint = MaterialTheme.colorScheme.tertiary
            )

            CompressionStatus.FAILED -> Row {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_retry))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove))
                }
            }

            CompressionStatus.PENDING, CompressionStatus.SKIPPED, CompressionStatus.CANCELLED -> {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove))
                }
            }

            else -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
