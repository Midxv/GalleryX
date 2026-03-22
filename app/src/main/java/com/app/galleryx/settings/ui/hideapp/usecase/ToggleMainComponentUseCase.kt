/*
 * Copyright 2020–2026 Leon Latsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * limitations under the License.
 */

package com.app.galleryx.settings.ui.hideapp.usecase

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import com.app.galleryx.BuildConfig
import javax.inject.Inject

// FIXED: Updated the package names from dev.leonlatsch.photok to com.app.galleryx
private val MAIN_LAUNCHER_COMPONENT =
    ComponentName(BuildConfig.APPLICATION_ID, "com.app.galleryx.MainLauncher")

private val STEALTH_LAUNCHER_COMPONENT =
    ComponentName(BuildConfig.APPLICATION_ID, "com.app.galleryx.StealthLauncher")

private val DISGUISE_LAUNCHER_COMPONENT =
    ComponentName(BuildConfig.APPLICATION_ID, "com.app.galleryx.DisguiseLauncher")

class ToggleMainComponentUseCase @Inject constructor(
    private val app: Application
) {

    // Retained for Stealth logic (Hide App)
    operator fun invoke() {
        if (isMainComponentDisabled() && !isDisguiseComponentEnabled()) {
            enableComponent(MAIN_LAUNCHER_COMPONENT)
            disableComponent(STEALTH_LAUNCHER_COMPONENT)
            disableComponent(DISGUISE_LAUNCHER_COMPONENT)
        } else {
            disableComponent(MAIN_LAUNCHER_COMPONENT)
            enableComponent(STEALTH_LAUNCHER_COMPONENT)
            disableComponent(DISGUISE_LAUNCHER_COMPONENT)
        }
    }

    // Toggles the Disguise logic specifically
    fun toggleDisguise() {
        if (isDisguiseComponentEnabled()) {
            // Turn off disguise, back to main
            enableComponent(MAIN_LAUNCHER_COMPONENT)
            disableComponent(STEALTH_LAUNCHER_COMPONENT)
            disableComponent(DISGUISE_LAUNCHER_COMPONENT)
        } else {
            // Turn on disguise
            disableComponent(MAIN_LAUNCHER_COMPONENT)
            disableComponent(STEALTH_LAUNCHER_COMPONENT)
            enableComponent(DISGUISE_LAUNCHER_COMPONENT)
        }
    }

    fun isMainComponentDisabled(): Boolean {
        val enabledSetting = app.packageManager.getComponentEnabledSetting(MAIN_LAUNCHER_COMPONENT)
        return enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun isDisguiseComponentEnabled(): Boolean {
        val enabledSetting = app.packageManager.getComponentEnabledSetting(DISGUISE_LAUNCHER_COMPONENT)
        return enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun enableComponent(componentName: ComponentName) {
        app.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disableComponent(componentName: ComponentName) {
        app.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}