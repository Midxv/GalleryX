/*
 * Copyright 2020–2026 GalleryX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.galleryx.settings.ui.compose

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.app.galleryx.BuildConfig
import com.app.galleryx.R
import com.app.galleryx.backup.domain.BackupStrategy
import com.app.galleryx.backup.ui.BackupBottomSheetDialogFragment
import com.app.galleryx.databinding.BindingConverters
import com.app.galleryx.other.extensions.launchAndIgnoreTimer
import com.app.galleryx.other.extensions.show
import com.app.galleryx.other.openUrl
import com.app.galleryx.other.sendEmail
import com.app.galleryx.other.setAppDesign
import com.app.galleryx.settings.data.Config
import com.app.galleryx.settings.domain.Preference
import com.app.galleryx.settings.domain.PreferenceScreenConfig
import com.app.galleryx.settings.domain.PreferenceScreenConfigContent
import com.app.galleryx.settings.domain.PreferenceSection
import com.app.galleryx.settings.domain.models.SettingsEnum
import com.app.galleryx.settings.domain.models.SystemDesignEnum
import com.app.galleryx.settings.ui.SettingsFragment
import com.app.galleryx.settings.ui.changepassword.ChangePasswordDialog
import com.app.galleryx.settings.ui.checkpassword.CheckPasswordDialog
import com.app.galleryx.ui.LocalFragment
import com.app.galleryx.ui.theme.AppTheme
import com.app.galleryx.uicomponnets.Dialogs

val LocalPreferencesValues: ProvidableCompositionLocal<Map<String, *>> = compositionLocalOf { emptyMap<String, String>() }

fun createBackupFilename(): String {
    return "photok_backup_${BindingConverters.millisToFormattedDateConverter(System.currentTimeMillis())}.zip"
}

@Composable
fun SettingsCallbacks(viewModel: SettingsViewModel) {
    val fragment = LocalFragment.current
    val context = LocalContext.current
    val activity = LocalActivity.current

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        fragment ?: return@rememberLauncherForActivityResult
        BackupBottomSheetDialogFragment(uri, BackupStrategy.Name.Default).show(fragment.parentFragmentManager)
    }

    LaunchedEffect(Unit) {
        fragment ?: return@LaunchedEffect

        viewModel.registerPreferenceCallback(Config.SYSTEM_DESIGN) {
            it as SystemDesignEnum
            setAppDesign(it)
            true
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_CHANGE_PASSWORD) {
            ChangePasswordDialog().show(fragment.childFragmentManager)
            false
        }

        viewModel.registerPreferenceCallback(Config.SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED) {
            viewModel.onBiometricUnlockChanged(it, fragment)
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_RESET) {
            CheckPasswordDialog {
                Dialogs.showConfirmDialog(
                    context,
                    context.getString(R.string.settings_advanced_reset_confirmation)
                ) { _, _ ->
                    viewModel.resetComponents()
                }
            }.show(fragment.childFragmentManager)
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_BACKUP) {
            backupLauncher.launchAndIgnoreTimer(
                createBackupFilename(),
                activity = activity,
            )
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_FEEDBACK) {
            val email = context.getString(R.string.settings_other_feedback_mail_emailaddress)
            val subject =
                "${context.getString(R.string.settings_other_feedback_mail_subject)} (App ${BuildConfig.VERSION_NAME} / Android ${Build.VERSION.RELEASE})"
            val text = context.getString(R.string.settings_other_feedback_mail_body)

            context.sendEmail(
                email = email,
                subject = subject,
                text = text,
                chooserTitle = context.getString(R.string.settings_other_feedback_title)
            )
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_DONATE) {
            fragment.openUrl(context.getString(R.string.settings_other_donate_url))
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_SOURCECODE) {
            fragment.openUrl(context.getString(R.string.settings_other_sourcecode_url))
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_CREDITS) {
            fragment.findNavController().navigate(R.id.action_settingsFragment_to_creditsFragment)
            false
        }

        viewModel.registerPreferenceCallback(SettingsFragment.KEY_ACTION_ABOUT) {
            fragment.findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
            false
        }
    }
}

@Composable
fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalPreferencesValues provides uiState.preferencesValues
    ) {
        SettingsContent(
            screenConfig = uiState.screenConfig,
            handleUiEvent = viewModel::handleUiEvent,
        )
    }

    SettingsCallbacks(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    screenConfig: PreferenceScreenConfig,
    handleUiEvent: (SettingsUiEvent) -> Unit,
) {
    val fragment = LocalFragment.current
    val navController = fragment?.findNavController()
    val uriHandler = LocalUriHandler.current

    AppTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (navController != null) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_back),
                                    contentDescription = stringResource(R.string.process_close)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets.statusBars
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { contentPadding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(bottom = 140.dp, top = 8.dp) // Bottom padding for navbar
            ) {
                for (section in screenConfig.sections) {
                    PreferenceSectionView(section = section) {

                        // Filter out hidden preferences
                        val visiblePreferences = section.preferences.filter {
                            it.key != SettingsFragment.KEY_ACTION_HIDE_APP &&
                                    it.key != Config.SECURITY_DIAL_LAUNCH_CODE
                        }

                        visiblePreferences.forEachIndexed { index, preference ->
                            val isLast = index == visiblePreferences.lastIndex

                            when (preference) {
                                is Preference.Simple -> {
                                    PreferenceView(
                                        icon = painterResource(preference.icon),
                                        title = stringResource(preference.title),
                                        summary = stringResource(preference.summary),
                                        showChevron = true, // iOS style navigation arrow
                                        onClick = {
                                            fragment ?: return@PreferenceView
                                            handleUiEvent(SettingsUiEvent.OnPreferenceClick(preference, null))
                                        }
                                    )
                                }
                                is Preference.Switch -> {
                                    PreferenceSwitchView(
                                        preference = preference,
                                        onSwitchChange = { value ->
                                            fragment ?: return@PreferenceSwitchView
                                            handleUiEvent(SettingsUiEvent.OnPreferenceClick(preference, value))
                                        },
                                    )
                                }
                                is Preference.Enum<*> -> {
                                    PreferenceEnumView(
                                        preference = preference,
                                        onItemSelected = { value ->
                                            fragment ?: return@PreferenceEnumView
                                            handleUiEvent(SettingsUiEvent.OnPreferenceClick(preference, value))
                                        },
                                    )
                                }
                            }

                            // iOS Style Indented Divider
                            if (!isLast) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }

                // NEW DEV & COPYRIGHT FOOTER
                SettingsFooter(uriHandler)
            }
        }
    }
}

@Composable
fun SettingsFooter(uriHandler: UriHandler) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Redirection Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { uriHandler.openUri("https://github.com/midxv/galleryx/releases") },
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_code),
                    contentDescription = "Releases",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Releases", fontWeight = FontWeight.SemiBold)
            }

            FilledTonalButton(
                onClick = { uriHandler.openUri("https://github.com/Midxv") },
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = "Author",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Author", fontWeight = FontWeight.SemiBold)
            }
        }

        // Copyright Info
        Text(
            text = "© 2026 Asif Middya • GalleryX",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PreferenceSectionView(
    section: PreferenceSection,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        // Section Header (Small, capitalized, sitting above the card)
        Text(
            text = stringResource(section.title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        // The iOS-style rounded card holding the items
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            content()
        }

        // Section Summary (Sitting below the card)
        if (section.summary != null) {
            Text(
                text = stringResource(section.summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : SettingsEnum> PreferenceEnumView(
    preference: Preference.Enum<T>,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferencesValues = LocalPreferencesValues.current

    var showDialog by remember { mutableStateOf(false) }

    val rawValue = preferencesValues[preference.key] as? String ?: preference.default.value
    val value = preference.possibleValues.find { it.value == rawValue } ?: preference.default

    PreferenceView(
        icon = painterResource(preference.icon),
        title = stringResource(preference.title),
        summary = stringResource(value.label),
        showChevron = true,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(preference.title),
                )
            },
            text = {
                Column {
                    for (v in preference.possibleValues) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showDialog = false
                                    onItemSelected(v)
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = value == v,
                                onClick = {
                                    showDialog = false
                                    onItemSelected(v)
                                },
                            )

                            Text(
                                text = stringResource(v.label),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun PreferenceSwitchView(
    preference: Preference.Switch,
    onSwitchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferencesValues = LocalPreferencesValues.current

    val summary = stringResource(preference.summary)
    val value = preferencesValues[preference.key] as? Boolean ?: preference.default

    PreferenceView(
        icon = painterResource(preference.icon),
        title = stringResource(preference.title),
        summary = summary,
        trailing = {
            Switch(
                checked = value,
                onCheckedChange = {
                    onSwitchChange(it)
                },
            )
        },
        onClick = {
            onSwitchChange(!value)
        },
        modifier = modifier
    )
}

@Composable
fun PreferenceView(
    icon: Painter,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (showChevron && onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    val context = LocalContext.current
    CompositionLocalProvider(LocalConfig provides Config(context)) {
        AppTheme {
            SettingsContent(
                screenConfig = PreferenceScreenConfig(PreferenceScreenConfigContent),
                handleUiEvent = {},
            )
        }
    }
}