package com.compresso.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.compresso.app.R
import com.compresso.app.data.model.ThemePreference
import com.compresso.app.ui.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_compression_mode_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_preserve_metadata)) },
                    supportingContent = { Text(stringResource(R.string.settings_preserve_metadata_desc)) },
                    trailingContent = {
                        Switch(checked = settings.preserveMetadata, onCheckedChange = viewModel::setPreserveMetadata)
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_allow_conversion)) },
                    supportingContent = { Text(stringResource(R.string.settings_allow_conversion_desc)) },
                    trailingContent = {
                        Switch(checked = settings.allowFormatConversion, onCheckedChange = viewModel::setAllowFormatConversion)
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_delete_originals)) },
                    supportingContent = { Text(stringResource(R.string.settings_delete_originals_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.deleteOriginalsWhenDone,
                            onCheckedChange = viewModel::setDeleteOriginalsWhenDone
                        )
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
                )
            }
            items(items = ThemePreference.entries.toList()) { theme ->
                ListItem(
                    headlineContent = { Text(themeLabel(theme)) },
                    leadingContent = {
                        RadioButton(selected = settings.theme == theme, onClick = { viewModel.setTheme(theme) })
                    },
                    modifier = Modifier.clickable { viewModel.setTheme(theme) }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Text(
                    text = stringResource(R.string.settings_about),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                TextButton(
                    onClick = viewModel::resetStats,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(R.string.settings_reset_stats))
                }
            }
        }
    }
}

@Composable
private fun themeLabel(theme: ThemePreference): String {
    return when (theme) {
        ThemePreference.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemePreference.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemePreference.DARK -> stringResource(R.string.settings_theme_dark)
    }
}
