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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.galleryx.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.app.galleryx.BuildConfig
import com.app.galleryx.settings.domain.models.StartPage
import com.app.galleryx.settings.domain.models.SystemDesignEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Manages reading and writing with the config file.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
class Config(context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val preferences: SharedPreferences = context.getSharedPreferences(FILE_NAME, MODE)

    val values: Map<String, *>
        get() = preferences.all

    val valuesFlow: Flow<Map<String, *>> = callbackFlow {
        send(preferences.all)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
            coroutineScope.launch { send(sharedPreferences.all) }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    /**
     * Determines if the app has started before.
     */
    var systemFirstStart: Boolean
        get() = getBoolean(SYSTEM_FIRST_START, SYSTEM_FIRST_START_DEFAULT)
        set(value) = putBoolean(SYSTEM_FIRST_START, value)

    /**
     * The version code of the last app version.
     * Updates after showing new features.
     */
    var systemLastFeatureVersionCode: Int
        get() = getInt(SYSTEM_LAST_FEATURE_VERSION_CODE, SYSTEM_LAST_FEATURE_VERSION_CODE_DEFAULT)
        set(value) = putInt(SYSTEM_LAST_FEATURE_VERSION_CODE, value)

    /*
     * Sets the app design to "light", "dark" or "system"
     */
    var systemDesign: SystemDesignEnum
        get() = SystemDesignEnum.fromValue(getString(SYSTEM_DESIGN, SYSTEM_DESIGN_DEFAULT.value))
        set(value) = putString(SYSTEM_DESIGN, value.value)

    /**
     * Determines if the full screen photo view, should hide the system ui at start.
     */
    var galleryAutoFullscreen: Boolean
        get() = getBoolean(GALLERY_AUTO_FULLSCREEN, GALLERY_AUTO_FULLSCREEN_DEFAULT)
        set(value) = putBoolean(GALLERY_AUTO_FULLSCREEN, value)

    /**
     * Determines if album covers should be replaced by a static folder icon for privacy.
     */
    var galleryStaticAlbumCover: Boolean
        get() = getBoolean(GALLERY_STATIC_ALBUM_COVER, GALLERY_STATIC_ALBUM_COVER_DEFAULT)
        set(value) = putBoolean(GALLERY_STATIC_ALBUM_COVER, value)

    /**
     * Determines the start page of the gallery.
     */
    var galleryStartPage: StartPage
        get() = StartPage.fromValue(getString(GALLERY_START_PAGE, GALLERY_START_PAGE_DEFAULT.value))
        set(value) = putString(GALLERY_START_PAGE, value.value)

    /**
     * Determines if the "All Files" bottom navigation menu item is hidden.
     */
    var galleryHideAllFilesMenu: Boolean
        get() = getBoolean(GALLERY_HIDE_ALL_FILES_MENU, GALLERY_HIDE_ALL_FILES_MENU_DEFAULT)
        set(value) = putBoolean(GALLERY_HIDE_ALL_FILES_MENU, value)

    /**
     * Determines if the search icon in the top app bar is hidden.
     */
    var galleryHideSearchIcon: Boolean
        get() = getBoolean(GALLERY_HIDE_SEARCH_ICON, GALLERY_HIDE_SEARCH_ICON_DEFAULT)
        set(value) = putBoolean(GALLERY_HIDE_SEARCH_ICON, value)

    /**
     * Determines if on-device AI semantic search and indexing is enabled.
     */
    var galleryAiSearchEnabled: Boolean
        get() = getBoolean(GALLERY_AI_SEARCH_ENABLED, GALLERY_AI_SEARCH_ENABLED_DEFAULT)
        set(value) = putBoolean(GALLERY_AI_SEARCH_ENABLED, value)

    // --- NEW: Album Hide Feature Variables ---

    /**
     * Determines if hidden albums should be temporarily visible.
     */
    var galleryShowHiddenAlbums: Boolean
        get() = getBoolean(GALLERY_SHOW_HIDDEN_ALBUMS, GALLERY_SHOW_HIDDEN_ALBUMS_DEFAULT)
        set(value) = putBoolean(GALLERY_SHOW_HIDDEN_ALBUMS, value)

    /**
     * Stores the set of UUIDs for albums that have been hidden.
     */
    var galleryHiddenAlbums: Set<String>
        get() = getStringSet(GALLERY_HIDDEN_ALBUMS, GALLERY_HIDDEN_ALBUMS_DEFAULT)
        set(value) = putStringSet(GALLERY_HIDDEN_ALBUMS, value)

    /**
     * Determines if screenshots should be allowed.
     */
    var securityAllowScreenshots: Boolean
        get() = getBoolean(SECURITY_ALLOW_SCREENSHOTS, SECURITY_ALLOW_SCREENSHOTS_DEFAULT)
        set(value) = putBoolean(SECURITY_ALLOW_SCREENSHOTS, value)

    /**
     * Password hash to check when unlocking.
     */
    var securityPassword: String?
        get() = getString(SECURITY_PASSWORD, SECURITY_PASSWORD_DEFAULT)
        set(value) = putString(SECURITY_PASSWORD, value!!)

    /**
     * Timeout to auto lock when in background.
     */
    var securityLockTimeout: Int
        get() = getIntFromString(SECURITY_LOCK_TIMEOUT, SECURITY_LOCK_TIMEOUT_DEFAULT)
        set(value) = putString(SECURITY_LOCK_TIMEOUT, value.toString())

    /**
     * Launch code to launch from phone dialer.
     */
    var securityDialLaunchCode: String?
        get() = getString(SECURITY_DIAL_LAUNCH_CODE, SECURITY_DIAL_LAUNCH_CODE_DEFAULT)
        set(value) = putString(SECURITY_DIAL_LAUNCH_CODE, value!!)

    /**
     * Determines if uninstall protection (Device Admin) is enabled.
     */
    var securityUninstallProtection: Boolean
        get() = getBoolean(SECURITY_UNINSTALL_PROTECTION, SECURITY_UNINSTALL_PROTECTION_DEFAULT)
        set(value) = putBoolean(SECURITY_UNINSTALL_PROTECTION, value)

    /**
     * Determines if the app should be disguised as "System Apps"
     */
    var securityDisguiseApp: Boolean
        get() = getBoolean(SECURITY_DISGUISE_APP, SECURITY_DISGUISE_APP_DEFAULT)
        set(value) = putBoolean(SECURITY_DISGUISE_APP, value)

    /**
     * Determines if files should be deleted after importing them.
     */
    var deleteImportedFiles: Boolean
        get() = getBoolean(ADVANCED_DELETE_IMPORTED_FILES, ADVANCED_DELETE_IMPORTED_FILES_DEFAULT)
        set(value) = putBoolean(ADVANCED_DELETE_IMPORTED_FILES, value)

    /**
     * Determines if files should be deleted after exporting them.
     */
    var deleteExportedFiles: Boolean
        get() = getBoolean(ADVANCED_DELETE_EXPORTED_FILES, ADVANCED_DELETE_EXPORTED_FILES_DEFAULT)
        set(value) = putBoolean(ADVANCED_DELETE_EXPORTED_FILES, value)

    var timestampLastRecoveryStart: Long
        get() = getLong(TIMESTAMP_LAST_RECOVERY_START, TIMESTAMP_LAST_RECOVERY_START_DEFAULT)
        set(value) = putLong(TIMESTAMP_LAST_RECOVERY_START, value)

    var legacyCurrentlyMigrating: Boolean
        get() = getBoolean("legacy^currentlyMigrating", false)
        set(value) = putBoolean("legacy^currentlyMigrating", value)

    var userSalt: String?
        get() = getString("user^salt", null)
        set(value) = putString("user^salt", value)

    var biometricAuthenticationEnabled: Boolean
        get() = getBoolean(SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED, SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED_DEFAULT)
        set(value) = putBoolean(SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED, value)

    // region put/get methods

    fun getString(key: String, default: String?) = preferences.getString(key, default)

    // --- NEW: Helper for StringSet ---
    fun getStringSet(key: String, default: Set<String>): Set<String> {
        return preferences.getStringSet(key, default) ?: default
    }

    fun getInt(key: String, default: Int) = preferences.getInt(key, default)

    fun getIntFromString(key: String, default: Int): Int {
        val stringValue = preferences.getString(key, default.toString())
        return stringValue?.toInt() ?: default
    }

    fun getLong(key: String, default: Long): Long = preferences.getLong(key, default)

    fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)

    fun putString(key: String, value: String?) {
        preferences.edit {
            putString(key, value)
        }
    }

    // --- NEW: Helper for StringSet ---
    fun putStringSet(key: String, value: Set<String>) {
        preferences.edit {
            putStringSet(key, value)
        }
    }

    fun putInt(key: String, value: Int) {
        preferences.edit {
            putInt(key, value)
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    fun putLong(key: String, value: Long) {
        preferences.edit {
            putLong(key, value)
        }
    }

    // endregion

    companion object {
        const val FILE_NAME = "${BuildConfig.APPLICATION_ID}_preferences"

        const val MODE = Context.MODE_PRIVATE

        const val SYSTEM_FIRST_START = "system^firstStart"
        const val SYSTEM_FIRST_START_DEFAULT = true

        const val SYSTEM_LAST_FEATURE_VERSION_CODE = "system^lastFeatureVersionCode"
        const val SYSTEM_LAST_FEATURE_VERSION_CODE_DEFAULT = 0

        const val SYSTEM_DESIGN = "system^design"
        val SYSTEM_DESIGN_DEFAULT = SystemDesignEnum.System

        const val GALLERY_AUTO_FULLSCREEN = "gallery^fullscreen.auto"
        const val GALLERY_AUTO_FULLSCREEN_DEFAULT = true

        const val GALLERY_STATIC_ALBUM_COVER = "gallery^staticAlbumCover"
        const val GALLERY_STATIC_ALBUM_COVER_DEFAULT = false

        const val GALLERY_START_PAGE = "gallery^startPage"
        val GALLERY_START_PAGE_DEFAULT = StartPage.AllFiles

        const val GALLERY_HIDE_ALL_FILES_MENU = "gallery^hideAllFilesMenu"
        const val GALLERY_HIDE_ALL_FILES_MENU_DEFAULT = false

        const val GALLERY_HIDE_SEARCH_ICON = "gallery^hideSearchIcon"
        const val GALLERY_HIDE_SEARCH_ICON_DEFAULT = false

        const val GALLERY_AI_SEARCH_ENABLED = "gallery^aiSearchEnabled"
        const val GALLERY_AI_SEARCH_ENABLED_DEFAULT = true

        // --- NEW: Keys for Album Hide Feature ---
        const val GALLERY_SHOW_HIDDEN_ALBUMS = "gallery^showHiddenAlbums"
        const val GALLERY_SHOW_HIDDEN_ALBUMS_DEFAULT = false

        const val GALLERY_HIDDEN_ALBUMS = "gallery^hiddenAlbums"
        val GALLERY_HIDDEN_ALBUMS_DEFAULT = emptySet<String>()

        const val SECURITY_ALLOW_SCREENSHOTS = "security^allowScreenshots"
        const val SECURITY_ALLOW_SCREENSHOTS_DEFAULT = false

        const val SECURITY_PASSWORD = "security^password"
        const val SECURITY_PASSWORD_DEFAULT = ""

        const val SECURITY_LOCK_TIMEOUT = "security^lockTimeout"
        const val SECURITY_LOCK_TIMEOUT_DEFAULT = 300000

        const val SECURITY_DIAL_LAUNCH_CODE = "security^dialLaunchCode"
        const val SECURITY_DIAL_LAUNCH_CODE_DEFAULT = "1337"

        const val SECURITY_UNINSTALL_PROTECTION = "security^uninstallProtection"
        const val SECURITY_UNINSTALL_PROTECTION_DEFAULT = false

        const val SECURITY_DISGUISE_APP = "security^disguiseApp"
        const val SECURITY_DISGUISE_APP_DEFAULT = false

        const val ADVANCED_DELETE_IMPORTED_FILES = "advanced^deleteImportedFiles"
        const val ADVANCED_DELETE_IMPORTED_FILES_DEFAULT = false

        const val ADVANCED_DELETE_EXPORTED_FILES = "advanced^deleteExportedFiles"
        const val ADVANCED_DELETE_EXPORTED_FILES_DEFAULT = false

        const val TIMESTAMP_LAST_RECOVERY_START = "internal^timestampLastRecoveryStart"
        const val TIMESTAMP_LAST_RECOVERY_START_DEFAULT = 0L

        const val SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED = "security^biometricAuthenticationEnabled"
        const val SECURITY_BIOMETRIC_AUTHENTICATION_ENABLED_DEFAULT = false
    }
}