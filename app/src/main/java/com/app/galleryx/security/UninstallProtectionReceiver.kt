package com.app.galleryx.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.app.galleryx.settings.data.Config

class UninstallProtectionReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Sync config when enabled
        Config(context).securityUninstallProtection = true
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Sync config when disabled
        Config(context).securityUninstallProtection = false
    }
}