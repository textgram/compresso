package com.compresso.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.compresso.app.R
import com.compresso.app.data.model.CompressionResult
import com.compresso.app.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    result: CompressionResult,
    displayName: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(text = displayName, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.padding(top = 16.dp))

            DetailRow(stringResource(R.string.detail_original_size), FormatUtils.formatBytes(result.originalSize))
            DetailRow(stringResource(R.string.detail_compressed_size), FormatUtils.formatBytes(result.compressedSize))
            DetailRow(stringResource(R.string.detail_reduction), FormatUtils.formatPercent(result.ratio))

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            DetailRow(stringResource(R.string.detail_codec), result.codec)
            DetailRow(stringResource(R.string.detail_container), result.container)
            DetailRow(stringResource(R.string.detail_method), result.method)
            result.resolution?.let { DetailRow(stringResource(R.string.detail_resolution), it) }
            result.colorDepth?.let { DetailRow(stringResource(R.string.detail_color_depth), it) }
            DetailRow(
                stringResource(R.string.detail_metadata),
                if (result.metadataPreserved) {
                    stringResource(R.string.detail_metadata_preserved)
                } else {
                    stringResource(R.string.detail_metadata_stripped)
                }
            )
            DetailRow(stringResource(R.string.detail_time_taken), FormatUtils.formatDuration(result.durationMs))

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = stringResource(R.string.detail_verification_passed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.padding(top = 20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.labelMedium)
    }
}
