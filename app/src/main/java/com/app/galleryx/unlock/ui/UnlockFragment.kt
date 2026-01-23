/*
 *   Copyright 2020–2026 Leon Latsch
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.app.galleryx.unlock.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.ApplicationState
import com.app.galleryx.BR
import com.app.galleryx.BuildConfig
import com.app.galleryx.R
import com.app.galleryx.databinding.FragmentUnlockBinding
import com.app.galleryx.other.extensions.finishOnBackWhileStarted
import com.app.galleryx.other.extensions.getBaseApplication
import com.app.galleryx.other.extensions.hide
import com.app.galleryx.other.extensions.launchLifecycleAwareJob
import com.app.galleryx.other.extensions.show
import com.app.galleryx.other.extensions.vanish
import com.app.galleryx.other.systemBarsPadding
import com.app.galleryx.security.biometric.BiometricUnlock
import com.app.galleryx.security.biometric.UserCanceledBiometricsException
import com.app.galleryx.security.migration.LegacyEncryptionMigrator
import com.app.galleryx.settings.data.Config
import com.app.galleryx.settings.domain.models.StartPage
import com.app.galleryx.uicomponnets.Dialogs
import com.app.galleryx.uicomponnets.base.BaseActivity
import com.app.galleryx.uicomponnets.bindings.BindableFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Unlock fragment.
 * Handles state and login.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class UnlockFragment : BindableFragment<FragmentUnlockBinding>(R.layout.fragment_unlock) {

    private val viewModel: UnlockViewModel by viewModels()

    @Inject
    lateinit var legacyEncryptionMigrator: LegacyEncryptionMigrator

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var biometricUnlock: BiometricUnlock

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.systemBarsPadding()
        finishOnBackWhileStarted()

        if (BuildConfig.DEBUG) {
            viewModel.password = "abc123"
        }

        launchLifecycleAwareJob {
            viewModel.unlockState.collectLatest {
                when (it) {
                    UnlockState.CHECKING -> binding.loadingOverlay.show()
                    UnlockState.UNLOCKED -> goToGallery()
                    UnlockState.LOCKED -> {
                        binding.loadingOverlay.hide()
                        binding.unlockWrongPasswordWarningTextView.show()
                    }

                    UnlockState.UNDEFINED -> Unit
                }
            }
        }

        viewModel.addOnPropertyChange<String>(BR.password) {
            if (binding.unlockWrongPasswordWarningTextView.visibility != View.INVISIBLE) {
                binding.unlockWrongPasswordWarningTextView.vanish()
            }
        }

        super.onViewCreated(view, savedInstanceState)

        // Check for migration should not be needed. But double check because in this case we don't have the legacy key
        if (biometricUnlock.isSetupAndValid() && !legacyEncryptionMigrator.migrationNeeded()) {
            binding.unlockUseBiometricUnlockButton.show()
            launchBiometricUnlock()
        } else {
            binding.unlockUseBiometricUnlockButton.hide()
        }
    }

    fun launchBiometricUnlock(delay: Long = 500L) {
        lifecycleScope.launch {
            delay(delay)

            biometricUnlock.unlock(this@UnlockFragment)
                .onSuccess { goToGallery() }
                .onFailure {
                    if (it !is UserCanceledBiometricsException) {
                        Dialogs.showLongToast(
                            context = requireContext(),
                            message = getString(R.string.biometric_unlock_error),
                        )
                    }
                }
        }
    }


    private fun goToGallery() {
        val activity = activity

        (activity as? BaseActivity)?.hideKeyboard()
        binding.loadingOverlay.hide()

        if (activity == null || !viewModel.encryptionManager.isReady) {
            Dialogs.showLongToast(requireContext(), getString(R.string.common_error))
            return
        }

        activity.getBaseApplication().state.update { ApplicationState.UNLOCKED }

        if (config.legacyCurrentlyMigrating || legacyEncryptionMigrator.migrationNeeded()) {
            lifecycleScope.launch {
                findNavController().navigate(R.id.action_unlockFragment_to_encryptionMigrationFragment)
            }
        } else {
            val startPageDest = when (config.galleryStartPage) {
                StartPage.AllFiles -> R.id.action_unlockFragment_to_galleryFragment
                StartPage.Albums -> R.id.action_unlockFragment_to_albumsFragment
            }

            findNavController().navigate(startPageDest)
        }
    }

    override fun bind(binding: FragmentUnlockBinding) {
        super.bind(binding)
        binding.context = this
        binding.viewModel = viewModel
    }
}