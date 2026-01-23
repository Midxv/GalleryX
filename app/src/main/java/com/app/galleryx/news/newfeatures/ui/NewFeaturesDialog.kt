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

package com.app.galleryx.news.newfeatures.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.app.galleryx.BuildConfig
import com.app.galleryx.R
import com.app.galleryx.databinding.DialogNewsBinding
import com.app.galleryx.news.newfeatures.ui.model.NewFeatureViewData
import com.app.galleryx.other.extensions.show
import com.app.galleryx.other.openUrl
import com.app.galleryx.settings.data.Config
import com.app.galleryx.uicomponnets.FixLinearLayoutManager
import com.app.galleryx.uicomponnets.bindings.BindableDialogFragment
import javax.inject.Inject

class ShowNewsDialogUseCase @Inject constructor(
    private val config: Config,
) {
    operator fun invoke(fragmentManager: FragmentManager) {
        if (config.systemLastFeatureVersionCode >= FEATURE_VERSION_CODE) return

        NewFeaturesDialog().show(fragmentManager)
        config.systemLastFeatureVersionCode = FEATURE_VERSION_CODE
    }
}

/**
 * Increase for this Dialog to show on the next update.
 * @see com.app.galleryx.gallery.ui.GalleryViewModel.runIfNews
 */
const val FEATURE_VERSION_CODE = 11

/**
 * Dialog for displaying new features.
 *
 * @since 1.3.0
 * @author Leon Latsch
 */
class NewFeaturesDialog : BindableDialogFragment<DialogNewsBinding>(R.layout.dialog_news) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.newsRecycler.layoutManager = FixLinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = NewFeaturesAdapter(getNewFeaturesViewData())

        binding.newsVersion.text = BuildConfig.VERSION_NAME
    }

    /**
     * Open the github release with the current version name.
     */
    fun openChangelog() {
        val url = getString(R.string.news_changelog_url)
        openUrl(url)
    }

    private fun getNewFeaturesViewData(): List<NewFeatureViewData> {
        val titles = resources.getStringArray(R.array.newsTitles)
        val summaries = resources.getStringArray(R.array.newsSummaries)

        return if (titles.size == summaries.size) {
            val viewDataList = mutableListOf<NewFeatureViewData>()
            for (i in 0..titles.lastIndex) {
                viewDataList.add(NewFeatureViewData(titles[i], summaries[i]))
            }
            viewDataList
        } else {
            listOf()
        }
    }

    override fun bind(binding: DialogNewsBinding) {
        super.bind(binding)
        binding.context = this
    }
}
