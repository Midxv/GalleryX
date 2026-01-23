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

package com.app.galleryx.settings.ui.hideapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.app.galleryx.R
import com.app.galleryx.databinding.DialogToggleAppVisibilityBinding
import com.app.galleryx.uicomponnets.Dialogs
import com.app.galleryx.uicomponnets.bindings.BindableDialogFragment

/**
 * Dialog to toggle the visibility of the app icon.
 *
 * @since 1.2.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class ToggleAppVisibilityDialog :
    BindableDialogFragment<DialogToggleAppVisibilityBinding>(R.layout.dialog_toggle_app_visibility) {

    val viewModel: ToggleAppVisibilityViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        useViewModel(viewModel)
        setupLayout()
    }

    private fun setupLayout() {
        val layout =
            if (viewModel.isMainComponentDisabled()) {
                R.layout.dialog_fragment_show_app
            } else {
                R.layout.dialog_fragment_hide_app
            }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.dialogHideAppFragmentContainer, Fragment(layout))
            .commit()
    }

    /**
     * Ask for confirmation and run viewModel.toggleMainComponent.
     */
    fun toggleAppVisibility() {
        Dialogs.showConfirmDialog(requireContext(), viewModel.confirmText) { _, _ ->
            viewModel.toggleMainComponent()
            dismiss()
        }
    }

    override fun bind(binding: DialogToggleAppVisibilityBinding) {
        super.bind(binding)
        binding.context = this
        binding.viewModel = viewModel
    }
}